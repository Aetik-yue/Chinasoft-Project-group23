package com.chinasoft.smokesensor.client;

import com.chinasoft.smokesensor.common.BusinessException;
import com.chinasoft.smokesensor.config.QwenVisionProperties;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 通义千问视觉模型客户端，封装 DashScope Qwen-VL 多模态接口。
 *
 * <p>3D 模拟鹦鹉模式每 ~5s 抓一帧发 Qwen-VL，获取"种类 + 行为"的零样本识别结果。
 * 调用的接口为 DashScope OpenAI 兼容格式：POST /chat/completions。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QwenVisionClient {

    /** VLM 返回的识别结果。 */
    public record VisionResult(String species, String behavior, double confidence) {
    }

    private final QwenVisionProperties props;

    private RestClient restClient;

    @PostConstruct
    void init() {
        if (props.getBaseUrl() != null && !props.getBaseUrl().isBlank()) {
            this.restClient = RestClient.builder()
                    .baseUrl(props.getBaseUrl())
                    .build();
        }
    }

    /** 是否可用（已启用且 base-url / api-key / model 均已配置）。 */
    public boolean isEnabled() {
        return props.isEnabled()
                && restClient != null
                && props.getApiKey() != null && !props.getApiKey().isBlank()
                && props.getModel() != null && !props.getModel().isBlank();
    }

    /**
     * 发送 base64 图像到 Qwen-VL，返回识别的种类、行为与综合置信度。
     *
     * @param base64Image 含或不含 data: 前缀的 base64 JPEG
     * @return VisionResult，失败抛 BusinessException
     */
    @SuppressWarnings("unchecked")
    public VisionResult analyze(String base64Image) {
        if (!isEnabled()) {
            throw new BusinessException(5001,
                    "Qwen-VL 未启用：请在 application.yml 设置 qwen.vision.enabled=true 并配置 api-key",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        // 保底 data:image/jpeg;base64, 前缀
        String imageUrl = base64Image.contains(",")
                ? base64Image
                : "data:image/jpeg;base64," + base64Image;

        Map<String, Object> imageContent = Map.of(
                "type", "image_url",
                "image_url", Map.of("url", imageUrl));
        Map<String, Object> textContent = Map.of(
                "type", "text",
                "text", "请识别图中这只鹦鹉的种类和当前行为。严格返回 JSON，不要其他文字："
                        + "{\"species\":\"种类中文名\",\"behavior\":\"行为中文名（如：进食/飞翔/睡觉/梳理羽毛/攀爬/饮水/站立观察/玩耍/跳跃等）\",\"confidence\":0.0-1.0}");

        Map<String, Object> message = Map.of(
                "role", "user",
                "content", List.of(imageContent, textContent));

        Map<String, Object> body = Map.of(
                "model", props.getModel(),
                "messages", List.of(message));

        try {
            Map<String, Object> response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + props.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            return parseResponse(response);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Qwen-VL 调用失败: {}", e.getMessage());
            throw new BusinessException(5001,
                    "Qwen-VL 识别失败: " + e.getMessage(),
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @SuppressWarnings("unchecked")
    private VisionResult parseResponse(Map<String, Object> response) {
        if (response == null) {
            throw new BusinessException(5001, "Qwen-VL 返回为空",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new BusinessException(5001, "Qwen-VL 返回无 choices",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        if (message == null) {
            throw new BusinessException(5001, "Qwen-VL 返回无 message",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        String content = (String) message.get("content");
        if (content == null || content.isBlank()) {
            throw new BusinessException(5001, "Qwen-VL 返回空内容",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        return extractVisionResult(content.trim());
    }

    /** 从模型返回的文本中提取 JSON 并解析为 VisionResult。 */
    private VisionResult extractVisionResult(String text) {
        // 无条件取首个 { 到末个 }，容忍模型前后加文字或 ```json 围栏
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        String json = (start >= 0 && end > start) ? text.substring(start, end + 1) : text;
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> map = mapper.readValue(json, Map.class);
            String species = (String) map.getOrDefault("species", "未知");
            String behavior = (String) map.getOrDefault("behavior", "未知");
            double confidence = map.get("confidence") instanceof Number n
                    ? n.doubleValue() : 0.5;
            return new VisionResult(species, behavior, confidence);
        } catch (Exception e) {
            log.warn("Qwen-VL 返回 JSON 解析失败: {}", text);
            // 降级：把整个文本当行为描述
            return new VisionResult("鹦鹉", text.length() > 20 ? text.substring(0, 20) : text, 0.6);
        }
    }
}

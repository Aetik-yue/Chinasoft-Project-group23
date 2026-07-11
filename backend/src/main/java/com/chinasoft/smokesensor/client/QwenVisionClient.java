package com.chinasoft.smokesensor.client;

import com.chinasoft.smokesensor.common.BusinessException;
import com.chinasoft.smokesensor.config.QwenVisionProperties;
import com.chinasoft.smokesensor.config.ApiKeyEncryptor;
import com.chinasoft.smokesensor.entity.SystemSetting;
import com.chinasoft.smokesensor.repository.SystemSettingRepository;
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
    private final SystemSettingRepository systemSettingRepository;
    private final ApiKeyEncryptor apiKeyEncryptor;

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
        String apiKey = getApiKey();
        return props.isEnabled()
                && restClient != null
                && apiKey != null && !apiKey.isBlank()
                && props.getModel() != null && !props.getModel().isBlank();
    }

    private String getApiKey() {
        String dbKey = systemSettingRepository.findBySettingKey("qwen_api_key")
                .map(SystemSetting::getSettingValue)
                .orElse(null);
        if (dbKey != null && !dbKey.isBlank()) {
            return apiKeyEncryptor.decrypt(dbKey);
        }
        return props.getApiKey();
    }

    /**
     * 发送 base64 图像到 Qwen-VL，返回识别的种类、行为与综合置信度。
     *
     * @param base64Image 含或不含 data: 前缀的 base64 JPEG
     * @return VisionResult，失败抛 BusinessException
     */
    @SuppressWarnings("unchecked")
    public VisionResult analyze(String base64Image) {
        return analyze(base64Image, null);
    }

    @SuppressWarnings("unchecked")
    public VisionResult analyze(String base64Image, String hint) {
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

        String prompt = "你是一个高精度的宠物行为分析专家。图中是一只处于 3D 虚拟鸟笼中的小太阳鹦鹉（绿颊锥尾鹦鹉）。请结合以下视觉线索，判断它当前正在进行什么行为：\n"
                + "1. 飞翔：鹦鹉处于空中（悬空），双翅张开，不在任何栖木或物体上。\n"
                + "2. 鸣叫：鹦鹉嘴部张开，可能伴随身体微微抖动。\n"
                + "3. 梳理羽毛：鹦鹉的头部扭向身体一侧或后方，嘴部接触翅膀或身体。\n"
                + "4. 睡觉：鹦鹉趴下、头部偏向一侧或缩回，双眼闭合。\n"
                + "5. 进食：鹦鹉靠近或头探入左下方的黄色食盆。\n"
                + "6. 饮水：鹦鹉靠近或头探入右下方的蓝色水盆。\n"
                + "7. 攀爬：鹦鹉抓挂在笼子铁丝网壁上，身体呈倾斜姿态。\n"
                + "8. 玩耍：鹦鹉靠近玩具（如铃铛或秋千等），头上下摆动或翅膀微张。\n"
                + "9. 跳跃：鹦鹉双脚离地在栖木间跳动。\n"
                + "10. 站立观察：鹦鹉正常站立在栖木上，头平视或左右转动，无上述其他特征。\n\n";

        if (hint != null && !hint.isBlank()) {
            prompt += "提示：它当前的动作在 3D 模拟中为 \"" + hint + "\"。请仔细观察图中的动作细节。如果视觉特征与提示的行为基本符合，请优先识别为该提示的行为。\n\n";
        }

        prompt += "严格返回以下格式的 JSON，不要包含任何额外的 Markdown 标记（如 ```json）或任何其他文字解释：\n"
                + "{\"species\":\"绿颊锥尾鹦鹉\",\"behavior\":\"行为中文名\",\"confidence\":0.0-1.0}";

        Map<String, Object> textContent = Map.of(
                "type", "text",
                "text", prompt);

        Map<String, Object> message = Map.of(
                "role", "user",
                "content", List.of(imageContent, textContent));

        Map<String, Object> body = Map.of(
                "model", props.getModel(),
                "messages", List.of(message),
                "enable_thinking", false); // 关闭思考模式，直接出结果，更快更干净

        try {
            Map<String, Object> response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + getApiKey())
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

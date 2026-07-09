package com.chinasoft.smokesensor.client;

import com.chinasoft.smokesensor.config.MaxKBProperties;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * MaxKB 智能问答客户端，封装 MaxKB 对话 API 调用。
 *
 * <p>MaxKB 对话需要两步：
 * <ol>
 *   <li>{@code GET /api/open?application_id={appId}} — 获取会话 ID (chat_id)</li>
 *   <li>{@code POST /api/chat_message/{chat_id}} — 发送消息并获取回答</li>
 * </ol>
 *
 * <p>降级策略：{@code qq.maxkb.enabled=false} 或未配置 api-key 时 {@link #isEnabled} 返回 false，
 * 调用方走规则回退；调用失败只 warn 不抛异常，返回 null 由调用方处理。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaxKBClient {

    private final MaxKBProperties properties;

    /** RestClient 实例，base-url 配置后在 @PostConstruct 中初始化。 */
    private RestClient restClient;

    @PostConstruct
    void init() {
        if (properties.getBaseUrl() != null && !properties.getBaseUrl().isBlank()) {
            this.restClient = RestClient.builder()
                    .baseUrl(properties.getBaseUrl())
                    .build();
        }
    }

    /**
     * 判断 MaxKB 智能问答是否可用（已启用且 base-url / api-key / app-id 均已配置）。
     */
    public boolean isEnabled() {
        return properties.isEnabled()
                && restClient != null
                && properties.getApiKey() != null && !properties.getApiKey().isBlank()
                && properties.getAppId() != null && !properties.getAppId().isBlank();
    }

    /**
     * 向 MaxKB 提问并返回答案文本。
     *
     * <p>内部先调 {@code GET /api/open} 获取 chat_id，再调 {@code POST /api/chat_message/{chat_id}} 发消息。
     *
     * @param userId  用户 QQ 号（仅用于日志，不参与 API 调用）
     * @param question 用户问题
     * @return 答案文本，失败或未启用时返回 null
     */
    public String chat(long userId, String question) {
        if (!isEnabled()) {
            return null;
        }
        try {
            // 第 1 步：获取 chat_id
            String chatId = getChatId();
            if (chatId == null || chatId.isBlank()) {
                log.warn("MaxKB 获取 chat_id 失败: userId={}", userId);
                return null;
            }
            // 第 2 步：发消息
            return sendMessage(chatId, question);
        } catch (Exception e) {
            log.warn("MaxKB 问答失败: userId={}, question={}, reason={}", userId, question, e.getMessage());
            return null;
        }
    }

    /**
     * 调用 {@code GET /api/open?application_id={appId}} 获取会话 ID。
     */
    private String getChatId() {
        Map<String, Object> response = restClient.get()
                .uri("/api/open?application_id={appId}", properties.getAppId())
                .header("Authorization", "Bearer " + properties.getApiKey())
                .retrieve()
                .body(Map.class);
        if (response == null || !Integer.valueOf(200).equals(response.get("code"))) {
            log.warn("MaxKB /api/open 返回异常: {}", response);
            return null;
        }
        Object data = response.get("data");
        return data == null ? null : data.toString();
    }

    /**
     * 调用 {@code POST /api/chat_message/{chat_id}} 发送消息并返回答案。
     */
    private String sendMessage(String chatId, String question) {
        Map<String, Object> body = Map.of(
                "message", question,
                "re_chat", false,
                "stream", false);

        Map<String, Object> response = restClient.post()
                .uri("/api/chat_message/{chatId}", chatId)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);
        return extractAnswer(response);
    }

    /**
     * 从 MaxKB 响应中提取答案文本。
     *
     * <p>响应结构 {@code {"code":200,"data":{"content":"...","answer_list":[...]}}}，优先取 data.content。
     */
    private String extractAnswer(Map<String, Object> response) {
        if (response == null) {
            return null;
        }
        Object code = response.get("code");
        if (!(Integer.valueOf(200).equals(code))) {
            log.warn("MaxKB 返回非 200: code={}, message={}", code, response.get("message"));
            return null;
        }
        Object data = response.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            Object content = dataMap.get("content");
            if (content != null) {
                return content.toString();
            }
        }
        log.warn("MaxKB 响应 data.content 缺失: {}", response);
        return null;
    }
}

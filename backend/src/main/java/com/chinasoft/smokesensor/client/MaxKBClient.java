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
 * MaxKB 智能问答客户端，封装 MaxKB OpenAPI 的会话问答调用。
 *
 * <p>MaxKB 为外部独立部署的 RAG 智能体服务，已导入「智慧宠物烟感安全系统」知识库
 * （含警情应急处理、鹦鹉养护、食物安全等文档）与数据库工具（Text-to-SQL），
 * 用于回答规则意图无法覆盖的自然语言问题（如"鹦鹉能吃辣椒吗""烟雾超标怎么办"）。
 *
 * <p>调用 MaxKB 的 {@code POST /api/application/{app_id}/chat/message} 接口：
 * <pre>
 * Header: Authorization: Bearer {api-key}
 * Body:   {"query": "用户问题", "chat_id": "会话ID", "stream": false}
 * 响应:   {"code":200, "data":{"content":"答案", "chat_id":"..."}}
 * </pre>
 *
 * <p>降级策略：{@code qq.maxkb.enabled=false} 或未配置 base-url 时，{@link #isEnabled} 返回 false，
 * 调用方走规则兜底；调用失败只 warn 不抛异常，返回 null 由调用方处理。
 *
 * <p>会话管理：以 "qq-{userId}" 作为 chat_id，使每个 QQ 用户拥有独立多轮对话上下文。
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
     * 判断 MaxKB 智能问答是否可用（已启用且 base-url 已配置）。
     */
    public boolean isEnabled() {
        return properties.isEnabled() && restClient != null;
    }

    /**
     * 向 MaxKB 提问并返回答案文本。
     *
     * @param chatId   会话 ID（建议用 "qq-{userId}" 保持每用户独立上下文）
     * @param question 用户问题
     * @return 答案文本，失败或未启用时返回 null
     */
    @SuppressWarnings("unchecked")
    public String chat(String chatId, String question) {
        if (!isEnabled()) {
            return null;
        }
        try {
            Map<String, Object> response = restClient.post()
                    .uri("/api/application/{appId}/chat/message", properties.getAppId())
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("query", question, "chat_id", chatId, "stream", false))
                    .retrieve()
                    .body(Map.class);
            return extractContent(response);
        } catch (Exception e) {
            log.warn("MaxKB 问答失败: question={}, reason={}", question, e.getMessage());
            return null;
        }
    }

    /**
     * 从 MaxKB 响应中提取答案文本。
     *
     * <p>MaxKB 响应结构 {@code {"code":200,"data":{"content":"..."}}}，code=200 表示成功。
     * 结构不符时返回 null。
     */
    private String extractContent(Map<String, Object> response) {
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

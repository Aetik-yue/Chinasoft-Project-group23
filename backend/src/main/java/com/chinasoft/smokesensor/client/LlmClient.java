package com.chinasoft.smokesensor.client;

import com.chinasoft.smokesensor.config.LlmProperties;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * DeepSeek（OpenAI 兼容）大模型客户端，封装 chat completions + function calling 调用。
 *
 * <p>调用 {@code POST {base-url}/v1/chat/completions}，请求体含 {@code model / messages / tools / tool_choice}，
 * 响应 {@code choices[0].message}（含 {@code content} 或 {@code tool_calls}）。
 *
 * <p>本类只负责 HTTP 调用与响应解析，工具执行与多轮编排由 {@code AgentToolService} 完成。
 *
 * <p>降级：{@code qq.llm.enabled=false} 或未配置 api-key 时 {@link #isEnabled} 返回 false，
 * 调用方走规则回退。调用失败只 warn 不抛异常，返回 null。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmClient {

    private final LlmProperties properties;

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
     * 是否启用（已 enabled 且 api-key 非空且 restClient 已初始化）。
     */
    public boolean isEnabled() {
        return properties.isEnabled()
                && restClient != null
                && properties.getApiKey() != null
                && !properties.getApiKey().isBlank();
    }

    /**
     * 调用 chat completions，返回 assistant 消息（含 content 或 tool_calls）。
     *
     * @param messages 对话消息列表（含历史 + 上一轮 assistant tool_calls + tool 结果）
     * @param tools    工具 schema 列表（OpenAI function 格式），为空则不传 tools
     * @return assistant 消息 Map，或 null（调用失败 / 无响应）
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> chat(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        if (!isEnabled()) {
            return null;
        }
        try {
            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("model", properties.getModel());
            body.put("messages", messages);
            if (tools != null && !tools.isEmpty()) {
                body.put("tools", tools);
                body.put("tool_choice", "auto");
            }

            Map<String, Object> response = restClient.post()
                    .uri("/v1/chat/completions")
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            return extractMessage(response);
        } catch (Exception e) {
            log.warn("DeepSeek 调用失败: reason={}", e.getMessage());
            return null;
        }
    }

    /**
     * 从响应中提取 choices[0].message。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMessage(Map<String, Object> response) {
        if (response == null) {
            return null;
        }
        Object choices = response.get("choices");
        if (choices instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
            Object message = first.get("message");
            if (message instanceof Map<?, ?> msg) {
                return (Map<String, Object>) msg;
            }
        }
        log.warn("DeepSeek 响应格式异常: {}", response);
        return null;
    }
}

package com.chinasoft.smokesensor.client;

import com.chinasoft.smokesensor.config.OneBotProperties;
import com.chinasoft.smokesensor.service.qq.EchoGuard;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * OneBot v11 HTTP API 客户端，封装向 NapCat 发送消息的调用。
 *
 * <p>当前仅实现私聊消息发送（{@link #sendPrivateMsg}}），后续如需群聊、图片等可按 OneBot v11
 * 规范扩展对应方法（send_group_msg / send_msg 等）。
 *
 * <p>降级策略：{@code qq.onebot.enabled=false} 时所有发送方法静默跳过（仅打 debug 日志），
 * 不影响告警入库、WebSocket 推送等主业务。HTTP 调用失败只 warn 不抛异常，避免阻断调用方。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OneBotClient {

    private final OneBotProperties properties;
    private final EchoGuard echoGuard;

    /** RestClient 实例，构造完成后在 @PostConstruct 中初始化。 */
    private RestClient restClient;

    @PostConstruct
    void init() {
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    /**
     * 判断 QQ 机器人是否启用。
     *
     * @return true 表示已配置并启用，调用方据此决定是否触发推送
     */
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    /**
     * 向指定 QQ 用户发送私聊文本消息。
     *
     * <p>调用 NapCat 的 {@code POST /send_private_msg} 接口，请求体：
     * {@code {"user_id": <userId>, "message": "<message>"}}，Header 携带 Bearer access_token。
     *
     * @param userId  目标 QQ 号
     * @param message 文本消息内容
     */
    public void sendPrivateMsg(long userId, String message) {
        if (!isEnabled()) {
            log.debug("QQ 机器人未启用，跳过发送私聊消息: userId={}, msg={}", userId, message);
            return;
        }
        try {
            restClient.post()
                    .uri("/send_private_msg")
                    .header("Authorization", "Bearer " + properties.getAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("user_id", userId, "message", message))
                    .retrieve()
                    .toBodilessEntity();
            log.info("QQ 私聊消息已发送: userId={}, msg={}", userId, message);
            // 记录发送内容，供 EchoGuard 识别自身消息回显，防止 echo 死循环
            echoGuard.recordSent(userId, message);
        } catch (Exception e) {
            log.warn("QQ 私聊消息发送失败: userId={}, reason={}", userId, e.getMessage());
        }
    }
}

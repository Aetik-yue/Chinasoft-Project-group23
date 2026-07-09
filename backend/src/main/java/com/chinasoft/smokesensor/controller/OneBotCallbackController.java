package com.chinasoft.smokesensor.controller;

import com.chinasoft.smokesensor.client.OneBotClient;
import com.chinasoft.smokesensor.common.ApiResult;
import com.chinasoft.smokesensor.config.OneBotProperties;
import com.chinasoft.smokesensor.service.qq.EchoGuard;
import com.chinasoft.smokesensor.service.qq.OneBotMessageRouter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * QQ 机器人（OneBot v11）回调入口与调试接口。
 *
 * <p>NapCat 配置 HTTP POST 上报地址为 {@code http://<后端地址>:8080/api/qq/callback}，
 * 用户发给机器人的私聊消息会以 OneBot v11 事件 JSON 形式 POST 到这里。
 *
 * <p>回调处理流程：解析事件 -> 过滤非私聊消息 -> 异步分派给 {@link OneBotMessageRouter}
 * 做意图识别与回复 -> 立即返回 {@code {"retcode":0}}。采用异步是为了避免 MaxKB 问答等
 * 耗时操作导致 NapCat 上报超时重试（同一条消息被处理多次）。
 */
@Slf4j
@RestController
@RequestMapping("/api/qq")
@RequiredArgsConstructor
public class OneBotCallbackController {

    private final OneBotClient oneBotClient;
    private final OneBotProperties properties;
    private final OneBotMessageRouter messageRouter;
    private final EchoGuard echoGuard;

    /**
     * OneBot v11 消息上报回调。
     *
     * <p>NapCat 把每条消息事件 POST 到此接口，请求体为 OneBot v11 事件 JSON，
     * 如 {@code {"post_type":"message","message_type":"private","user_id":123,"message":"状态"}}。
     *
     * <p>只处理私聊消息（{@code message_type=private}），群聊消息忽略（符合"私聊为主"决策）。
     * 响应需返回 {@code {"retcode":0}} 告知 NapCat 已接收成功，否则可能触发重试。
     *
     * @param body OneBot v11 事件 JSON
     * @return OneBot 规范响应
     */
    @PostMapping("/callback")
    public Map<String, Object> callback(@RequestBody Map<String, Object> body) {
        log.info("收到 OneBot 上报: {}", body);

        // 仅处理私聊消息事件，其他事件（心跳、群聊等）直接应答
        if (!"message".equals(body.get("post_type"))
                || !"private".equals(body.get("message_type"))) {
            return okResponse();
        }

        Object userIdObj = body.get("user_id");
        Object messageObj = body.get("message");
        if (userIdObj instanceof Number number && messageObj != null) {
            long userId = number.longValue();
            String message = extractText(messageObj);
            // 自身消息回显防护：NapCat 若开启 echo，后端发的回复会被上报回来，导致死循环
            if (echoGuard.isEcho(userId, message)) {
                return okResponse();
            }
            // 异步处理，避免 NapCat 上报超时重试（MaxKB 问答可能耗时数秒）
            CompletableFuture.runAsync(() -> {
                try {
                    String reply = messageRouter.handle(userId, message);
                    if (reply != null) {
                        oneBotClient.sendPrivateMsg(userId, reply);
                    }
                } catch (Exception e) {
                    log.warn("异步处理 QQ 消息失败: userId={}, reason={}", userId, e.getMessage());
                }
            });
        }
        return okResponse();
    }

    /**
     * 调试接口：手动触发一条私聊消息，验证后端 -> NapCat 通路是否畅通。
     *
     * <p>用法：{@code GET /api/qq/test/send?msg=你好}，msg 为空时发送默认测试文本。
     * 消息发送给配置项 {@code qq.onebot.push-target-user} 指定的 QQ 号。
     */
    @GetMapping("/test/send")
    public ApiResult testSend(@RequestParam(defaultValue = "") String msg) {
        if (!properties.isEnabled() || properties.getPushTargetUserId() == null) {
            return ApiResult.error(5001, "QQ 机器人未启用或未配置 push-target-user");
        }
        String message = msg.isBlank()
                ? "测试消息：智慧宠物烟感安全系统 QQ 通道已联通 ✓"
                : msg;
        oneBotClient.sendPrivateMsg(properties.getPushTargetUserId(), message);
        return ApiResult.ok(Map.of(
                "targetUser", properties.getPushTargetUserId(),
                "message", message));
    }

    /**
     * 从 OneBot message 字段提取纯文本。
     *
     * <p>OneBot v11 的 message 字段在 post_format=string 时为纯文本字符串，
     * 在 post_format=array 时为消息段数组（如 {@code [{"type":"text","data":{"text":"状态"}}]}）。
     * 本方法统一提取 text 段内容，非文本消息段忽略。
     */
    private String extractText(Object messageObj) {
        if (messageObj instanceof String text) {
            return text;
        }
        if (messageObj instanceof List<?> segments) {
            StringBuilder sb = new StringBuilder();
            for (Object segment : segments) {
                if (segment instanceof Map<?, ?> segMap && "text".equals(segMap.get("type"))) {
                    Object data = segMap.get("data");
                    if (data instanceof Map<?, ?> dataMap && dataMap.get("text") != null) {
                        sb.append(dataMap.get("text"));
                    }
                }
            }
            return sb.toString();
        }
        return messageObj.toString();
    }

    /** OneBot 规范成功响应，告知 NapCat 已接收，不要重试。 */
    private Map<String, Object> okResponse() {
        return Map.of("status", "ok", "retcode", 0);
    }
}

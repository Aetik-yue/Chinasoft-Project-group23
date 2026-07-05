package com.chinasoft.smokesensor.config;

import com.chinasoft.smokesensor.dto.AlarmWebSocketPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * WebSocket 会话管理器：维护所有活跃的前端 WebSocket 连接。
 *
 * <p>当后端触发告警时，调用 {@link #broadcastAlarm(AlarmWebSocketPayload)}
 * 向所有连接的前端页面推送告警消息。
 *
 * <p>使用 CopyOnWriteArraySet 保证并发安全，适用于连接数较少（<100）的场景。
 */
@Slf4j
@Component
public class AlarmWebSocketSessionManager {

    /** 所有当前连接的 WebSocket 会话集合。 */
    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    /** JSON 序列化工具，用于将告警消息转为 JSON 字符串。 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 添加一个新的 WebSocket 会话到管理器。
     *
     * @param session 前端建立的 WebSocket 会话
     */
    public void addSession(WebSocketSession session) {
        sessions.add(session);
        log.info("WebSocket 前端已连接，当前连接数: {}", sessions.size());
    }

    /**
     * 从管理器中移除一个 WebSocket 会话。
     *
     * @param session 前端断开的 WebSocket 会话
     */
    public void removeSession(WebSocketSession session) {
        sessions.remove(session);
        log.info("WebSocket 前端已断开，当前连接数: {}", sessions.size());
    }

    /**
     * 向所有连接的 WebSocket 前端推送告警消息。
     *
     * <p>遍历所有活跃会话，逐个发送 JSON 格式的告警数据。
     * 发送失败的会话会被自动移除（前端可能已经断开但未关闭连接）。
     *
     * @param payload 告警消息内容
     */
    public void broadcastAlarm(AlarmWebSocketPayload payload) {
        if (sessions.isEmpty()) {
            log.debug("没有 WebSocket 前端连接，跳过告警推送");
            return;
        }
        try {
            String messageJson = objectMapper.writeValueAsString(payload);
            TextMessage textMessage = new TextMessage(messageJson);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        log.warn("向 WebSocket 会话 {} 发送告警消息失败，已移除", session.getId());
                        sessions.remove(session);
                    }
                } else {
                    sessions.remove(session);
                }
            }
        } catch (Exception e) {
            log.error("序列化告警消息失败", e);
        }
    }
}
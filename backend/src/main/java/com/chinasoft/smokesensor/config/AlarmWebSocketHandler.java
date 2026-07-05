package com.chinasoft.smokesensor.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket 事件处理器：处理前端的 WebSocket 连接、断开和心跳。
 *
 * <p>当前端连接 {@code ws://localhost:8080/ws/alarm} 时，Spring WebSocket
 * 将连接交给此类管理。本类只做三件事：
 * <ul>
 *   <li>连接建立时，将会话注册到 AlarmWebSocketSessionManager</li>
 *   <li>连接断开时，将会话从 AlarmWebSocketSessionManager 移除</li>
 *   <li>收到前端心跳 ping 时，回复 pong 保持连接</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlarmWebSocketHandler extends TextWebSocketHandler {

    /** WebSocket 会话管理器，维护所有在线连接。 */
    private final AlarmWebSocketSessionManager sessionManager;

    /**
     * 前端 WebSocket 连接建立时调用。
     *
     * <p>将会话添加到管理器，之后告警触发时可以推送到此会话。
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessionManager.addSession(session);
    }

    /**
     * 前端 WebSocket 连接关闭时调用。
     *
     * <p>将会话从管理器中移除，不再向此会话推送告警消息。
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionManager.removeSession(session);
    }

    /**
     * 收到前端发送的文本消息时调用。
     *
     * <p>当前仅处理心跳 ping：前端每 30 秒发 {"type":"ping"}，回复 {"type":"pong"}。
     * 其他类型的消息不做处理。
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        if ("{\"type\":\"ping\"}".equals(payload.trim())) {
            session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
        }
    }

    /**
     * WebSocket 传输层发生错误时调用。
     *
     * <p>打印错误日志并从管理器中移除异常会话。
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket 传输错误，会话: {}, 错误: {}", session.getId(), exception.getMessage());
        sessionManager.removeSession(session);
    }
}
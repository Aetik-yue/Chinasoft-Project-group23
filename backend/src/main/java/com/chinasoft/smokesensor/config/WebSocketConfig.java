package com.chinasoft.smokesensor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置：注册告警推送端点。
 *
 * <p>前端通过 ws://localhost:8080/ws/alarm 连接后端 WebSocket，
 * 告警触发时后端主动推送告警消息给前端，替代前端轮询告警统计接口。
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    /** 告警 WebSocket 事件处理逻辑。 */
    private final AlarmWebSocketHandler alarmWebSocketHandler;

    /** 鹦鹉实时识别 WebSocket 处理逻辑。 */
    private final ParrotWebSocketHandler parrotWebSocketHandler;

    public WebSocketConfig(AlarmWebSocketHandler alarmWebSocketHandler,
                           ParrotWebSocketHandler parrotWebSocketHandler) {
        this.alarmWebSocketHandler = alarmWebSocketHandler;
        this.parrotWebSocketHandler = parrotWebSocketHandler;
    }

    /**
     * 注册 WebSocket 端点 /ws/alarm 与 /ws/parrot。
     *
     * <p>setAllowedOrigins("*") 允许前端跨域连接（开发阶段使用）。
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(alarmWebSocketHandler, "/ws/alarm")
                .setAllowedOrigins("*");
        registry.addHandler(parrotWebSocketHandler, "/ws/parrot")
                .setAllowedOrigins("*");
    }
}
package com.chinasoft.smokesensor.config;

import com.chinasoft.smokesensor.common.BusinessException;
import com.chinasoft.smokesensor.config.ParrotProperties;
import com.chinasoft.smokesensor.dto.ParrotAbnormalEvent;
import com.chinasoft.smokesensor.dto.ParrotBehaviorResponse;
import com.chinasoft.smokesensor.service.ParrotBehaviorService;
import com.chinasoft.smokesensor.service.parrot.ParrotAbnormalDetector;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 鹦鹉实时识别 WebSocket 处理器：前端每帧发 base64 图 → 后端识别 → 回传框+行为+异常。
 *
 * <p>连接建立时为本会话创建独立的 {@link ParrotAbnormalDetector}（有状态），
 * 每帧识别后把结果喂给检测器，异常时通过响应里的 {@code abnormal} 字段回传。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParrotWebSocketHandler extends TextWebSocketHandler {

    private static final int MAX_MESSAGE_SIZE = 2 * 1024 * 1024;

    private final ParrotBehaviorService parrotBehaviorService;
    private final ParrotProperties parrotProperties;
    /** 注入 Spring 自动配置的 ObjectMapper（已注册 JSR310，可序列化 LocalDateTime）。 */
    private final ObjectMapper objectMapper;

    /** 每个会话一个独立异常检测器。非 final，避免被 @RequiredArgsConstructor 当作注入 bean。 */
    private Map<String, ParrotAbnormalDetector> detectors = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        session.setTextMessageSizeLimit(MAX_MESSAGE_SIZE);
        detectors.put(session.getId(), new ParrotAbnormalDetector(parrotProperties.getAbnormal()));
        log.info("鹦鹉 WebSocket 连接建立: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        if (payload.length() > MAX_MESSAGE_SIZE) {
            log.warn("鹦鹉 WS 消息过大: {} 字符", payload.length());
            return;
        }
        Path tmp = null;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> req = objectMapper.readValue(payload, Map.class);
            String imageData = (String) req.get("image");
            String deviceId = (String) req.getOrDefault("deviceId", "default");
            if (imageData == null || imageData.isBlank()) {
                return;
            }
            String b64 = imageData.contains(",")
                    ? imageData.substring(imageData.indexOf(",") + 1)
                    : imageData;
            byte[] bytes = Base64.getDecoder().decode(b64);
            tmp = Files.createTempFile("parrot-frame-", ".jpg");
            Files.write(tmp, bytes);

            ParrotBehaviorResponse resp = parrotBehaviorService.analyzeRealtime(tmp.toString(), deviceId);

            ParrotAbnormalDetector detector = detectors.get(session.getId());
            if (detector != null) {
                ParrotAbnormalEvent ev = detector.update(
                        Boolean.TRUE.equals(resp.getParrotDetected()),
                        resp.getBoxes(), resp.getBehavior(), System.currentTimeMillis());
                if (ev != null) {
                    resp.setAbnormal(List.of(ev));
                }
            }
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(resp)));
        } catch (BusinessException e) {
            log.warn("鹦鹉实时识别未启用: {}", e.getMessage());
            sendError(session, e.getMessage());
        } catch (Exception e) {
            log.error("鹦鹉 WS 处理失败", e);
            sendError(session, "识别失败: " + e.getMessage());
        } finally {
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (Exception ignored) {
                    // 忽略临时文件删除失败
                }
            }
        }
    }

    private void sendError(WebSocketSession session, String msg) {
        try {
            session.sendMessage(new TextMessage("{\"error\":\"" + msg.replace("\"", "'") + "\"}"));
        } catch (Exception ignored) {
            // 忽略发送失败
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        detectors.remove(session.getId());
        log.info("鹦鹉 WebSocket 连接关闭: {}", session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("鹦鹉 WS 传输错误: {}", exception.getMessage());
    }
}

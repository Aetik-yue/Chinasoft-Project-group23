package com.chinasoft.smokesensor.service.qq;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 自身消息回显（echo）防护。
 *
 * <p>NapCat 若开启「上报自身消息 / echo」，后端发出的每条回复会被 NapCat 上报回来，
 * 后端若再次处理会触发"回复->上报->再回复"死循环，导致 LLM 被高频调用异常扣费。
 *
 * <p>本组件记录后端最近发送的 (userId, content)，收到上报时若在 TTL 内命中相同内容，
 * 判定为自身消息回显，由调用方忽略。
 *
 * <p>使用内存 Map 而非 Redis：echo 防护是毫秒级短期记忆，无需持久化，内存更快且无依赖。
 */
@Slf4j
@Component
public class EchoGuard {

    /** 判定为 echo 的时间窗口：5 秒内相同内容视为自身回显。 */
    private static final Duration TTL = Duration.ofSeconds(5);

    /** key = userId:content，value = 发送时间戳。 */
    private final Map<String, Long> sentMessages = new ConcurrentHashMap<>();

    /**
     * 记录后端向某用户发送的消息，用于后续 echo 识别。
     *
     * @param userId  目标用户 QQ 号
     * @param content 发送的消息内容
     */
    public void recordSent(long userId, String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        sentMessages.put(key(userId, content), System.currentTimeMillis());
    }

    /**
     * 判断收到的消息是否为自身回显（5 秒内后端曾发送过相同内容给该用户）。
     *
     * @param userId  发送方 QQ 号
     * @param content 消息内容
     * @return true 表示这是自身回显，应忽略
     */
    public boolean isEcho(long userId, String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        Long sentAt = sentMessages.get(key(userId, content));
        if (sentAt == null) {
            return false;
        }
        if (System.currentTimeMillis() - sentAt > TTL.toMillis()) {
            // 超过时间窗口，清理并判定为非 echo
            sentMessages.remove(key(userId, content));
            return false;
        }
        log.debug("检测到自身消息回显，忽略: userId={}, content={}", userId, content);
        return true;
    }

    private String key(long userId, String content) {
        return userId + ":" + content;
    }
}

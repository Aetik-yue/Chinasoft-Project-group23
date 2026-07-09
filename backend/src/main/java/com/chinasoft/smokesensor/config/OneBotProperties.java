package com.chinasoft.smokesensor.config;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * QQ 机器人（NapCat / OneBot v11）配置，前缀 qq.onebot。
 *
 * <p>默认 enabled=false，未部署 NapCat 时 QQ 相关功能静默跳过，应用仍可正常启动。
 * 部署好 NapCat 并完成扫码登录后，将 enabled 改为 true 即可启用。
 *
 * <p>OneBot v11 协议通过 HTTP 收发消息：
 * <ul>
 *   <li>收消息：NapCat 把消息 POST 上报到 {@code /api/qq/callback}</li>
 *   <li>发消息：后端调用 NapCat 的 {@code /send_private_msg} 等 HTTP API</li>
 * </ul>
 */
@Data
@Component
@ConfigurationProperties(prefix = "qq.onebot")
public class OneBotProperties {

    /** 是否启用 QQ 机器人（需先部署 NapCat 并配置 token）。 */
    private boolean enabled = false;

    /** NapCat HTTP API 地址，如 http://localhost:3000。 */
    private String baseUrl = "http://localhost:3000";

    /** NapCat 配置的 access_token，用于 HTTP API 鉴权（Bearer）。 */
    private String accessToken;

    /** 允许交互的 QQ 号白名单（只响应这些用户的私聊消息，防止陌生人操控设备）。 */
    private List<Long> allowedUsers;

    /**
     * 主动推送目标 QQ 号（告警 / 日报等主动消息发送给该用户）。
     *
     * <p>用 String 而非 Long，以便环境变量未设置时 {@code ${ONEBOT_PUSH_USER:}} 留空绑定 null，
     * 避免 Long 绑定空字符串失败（@ConfigurationProperties 不支持 SpEL null 技巧）。
     * 实际使用时通过 {@link #getPushTargetUserId()} 解析为 Long。
     */
    private String pushTargetUser;

    /**
     * 解析推送目标 QQ 号为 Long；空或非法格式返回 null。
     *
     * @return 目标 QQ 号，或 null（未配置）
     */
    public Long getPushTargetUserId() {
        if (pushTargetUser == null || pushTargetUser.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(pushTargetUser.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

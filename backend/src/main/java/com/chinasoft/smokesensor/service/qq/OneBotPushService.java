package com.chinasoft.smokesensor.service.qq;

import com.chinasoft.smokesensor.client.OneBotClient;
import com.chinasoft.smokesensor.config.OneBotProperties;
import com.chinasoft.smokesensor.service.alarm.AlarmTriggeredEvent;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * QQ 消息统一推送服务。
 *
 * <p>所有需要主动推送给用户的场景（告警、定时报告、离线提醒）都通过此服务，
 * 内部统一处理 enabled 判断、目标用户解析、消息文本组装，调用方只需传入业务数据。
 *
 * <p>当前实现告警实时推送；每日环境报告 / 宠物成长日报 / 设备离线提醒在 P5 阶段补充。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OneBotPushService {

    private final OneBotClient oneBotClient;
    private final OneBotProperties properties;

    /**
     * 推送告警实时消息给配置的 push-target-user。
     *
     * <p>未配置目标用户时静默跳过（debug 日志），不抛异常。
     *
     * @param event 告警触发事件
     */
    public void pushAlarm(AlarmTriggeredEvent event) {
        Long target = properties.getPushTargetUserId();
        if (target == null) {
            log.debug("未配置 qq.onebot.push-target-user，跳过告警推送: alarmId={}", event.alarmId());
            return;
        }
        oneBotClient.sendPrivateMsg(target, buildAlarmMessage(event));
    }

    /**
     * 推送任意文本消息（供定时报告等场景使用，内容由调用方组装）。
     *
     * @param content 文本内容
     */
    public void pushMessage(String content) {
        Long target = properties.getPushTargetUserId();
        if (target == null) {
            log.debug("未配置 push-target-user，跳过推送: content={}", content);
            return;
        }
        oneBotClient.sendPrivateMsg(target, content);
    }

    /**
     * 推送设备离线告警。
     *
     * @param deviceId    设备编号
     * @param lastDataAt  最后一次数据时间
     */
    public void pushDeviceOffline(String deviceId, LocalDateTime lastDataAt) {
        Long target = properties.getPushTargetUserId();
        if (target == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("⚠️ 设备离线\n");
        sb.append("设备：").append(deviceId).append("\n");
        if (lastDataAt != null) {
            sb.append("最后数据：").append(lastDataAt).append("\n");
        }
        sb.append("请检查设备连接与供电");
        oneBotClient.sendPrivateMsg(target, sb.toString());
    }

    /**
     * 推送设备恢复在线通知。
     */
    public void pushDeviceRecover(String deviceId) {
        Long target = properties.getPushTargetUserId();
        if (target == null) {
            return;
        }
        oneBotClient.sendPrivateMsg(target, "✅ 设备 " + deviceId + " 已恢复在线");
    }

    /**
     * 组装告警文本消息。
     *
     * <p>按风险等级使用不同 emoji 前缀，包含设备、浓度、等级、时间，并提示用户可回复"处理告警"处理。
     */
    private String buildAlarmMessage(AlarmTriggeredEvent event) {
        String levelText = switch (event.riskLevel() == null ? "" : event.riskLevel()) {
            case "high" -> "🔴 高危";
            case "medium" -> "🟠 中危";
            case "low" -> "🟡 低危";
            default -> "⚠️ 告警";
        };
        StringBuilder sb = new StringBuilder();
        sb.append(levelText).append(" 烟雾告警\n");
        sb.append("设备：").append(event.deviceId()).append("\n");
        if (event.smokeValue() != null) {
            sb.append("浓度：").append(event.smokeValue()).append("ppm\n");
        }
        sb.append("等级：").append(event.riskLevel()).append("\n");
        sb.append("时间：").append(event.triggeredAt()).append("\n");
        if (event.simulated()) {
            sb.append("（模拟触发）\n");
        }
        sb.append("回复“处理告警”查看并处理");
        return sb.toString();
    }
}

package com.chinasoft.smokesensor.service.qq;

import com.chinasoft.smokesensor.client.OneBotClient;
import com.chinasoft.smokesensor.config.OneBotProperties;
import com.chinasoft.smokesensor.repository.UserPreferenceRepository;
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

    public static volatile Long lastScreenshotQq = null;
    public static volatile long lastScreenshotTime = 0;

    private final OneBotClient oneBotClient;
    private final OneBotProperties properties;
    private final UserPreferenceRepository userPreferenceRepository;

    /**
     * 推送告警实时消息给配置的 push-target-user。
     *
     * <p>未配置目标用户时静默跳过（debug 日志），不抛异常。
     *
     * @param event 告警触发事件
     */
    public void pushAlarm(AlarmTriggeredEvent event) {
        Long target = null;
        if (event.userId() != null) {
            target = userPreferenceRepository.findByUserIdAndPrefKey(event.userId(), "bound_qq")
                    .map(pref -> {
                        try {
                            return Long.parseLong(pref.getPrefValue().trim());
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .orElse(null);
        }
        if (target == null) {
            target = properties.getPushTargetUserId();
        }
        if (target == null) {
            log.debug("未配置 qq.onebot.push-target-user，跳过告警推送: alarmId={}", event.alarmId());
            return;
        }
        oneBotClient.sendPrivateMsg(target, buildAlarmMessage(event));
    }

    /**
     * 推送健康综合评分过低警报与专业养护就医建议。
     */
    public void pushHealthScoreAlert(Long ownerId, String petName, int score, String latestSymptom) {
        Long target = null;
        if (ownerId != null) {
            target = userPreferenceRepository.findByUserIdAndPrefKey(ownerId, "bound_qq")
                    .map(pref -> {
                        try {
                            return Long.parseLong(pref.getPrefValue().trim());
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .orElse(null);
        }
        if (target == null) {
            target = properties.getPushTargetUserId();
        }
        if (target == null) {
            log.debug("未配置推送目标用户，跳过健康警报推送");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("🩺 警报：宠物健康评分过低！\n");
        sb.append("鹦鹉名字：").append(petName).append("\n");
        sb.append("综合健康评分：🔴 ").append(score).append(" 分 (警惕)\n");
        if (latestSymptom != null && !latestSymptom.isBlank()) {
            sb.append("近期异常症状：").append(latestSymptom).append("\n");
        }
        sb.append("\n💡 执业兽医建议：\n");
        sb.append("该鹦鹉近90天就诊和异常症状记录较频繁，健康状态评级较差。建议您尽快预约并前往本地的异宠/鸟类专科宠物医院（可向我发送“推荐附近宠物医院”进行精准推荐）做一次全面体检；同时注意保暖，避免改变笼舍环境以防产生应激反应。");

        oneBotClient.sendPrivateMsg(target, sb.toString());
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
     * 组装告警文本消息，包含具体的报警情况和解决建议。
     */
    private String buildAlarmMessage(AlarmTriggeredEvent event) {
        String metric = event.metric();
        double val = event.metricValue() != null ? event.metricValue() : (event.smokeValue() != null ? event.smokeValue().doubleValue() : 0.0);
        double boundary = event.thresholdValue() != null ? event.thresholdValue() : 0.0;
        String unit = event.unit() != null ? event.unit() : "ppm";
        String alarmType = event.alarmType() != null ? event.alarmType() : "dust_high";

        StringBuilder sb = new StringBuilder();

        if ("environment_score".equals(metric) || alarmType.startsWith("environment_score")) {
            sb.append("🌡️ 警报：笼舍环境适配度评分过低！\n");
            sb.append("当前环境评分：🔴 ").append((int)Math.round(val)).append(" 分 (安全警戒线: ").append((int)Math.round(boundary)).append("分)\n");
            sb.append("建议措施：笼舍内的温湿度或空气粉尘浓度偏离了该鹦鹉种类的理想区间。请登录系统查看具体的超标指标，并开启相应的降温/保暖/除湿/加湿或空气净化设备，防止鹦鹉产生应激或呼吸道疾病！\n");
        } else if ("temperature".equals(metric) || alarmType.startsWith("temperature")) {
            if (alarmType.endsWith("high")) {
                sb.append("🌡️ 警报：笼舍温度过高！\n");
                sb.append("当前温度：").append(val).append("℃ (阈值: ").append(boundary).append("℃)\n");
                sb.append("建议措施：请立即开启降温风扇，并检查饮水器中水是否充足，防止鹦鹉产生严重应激或中暑！\n");
            } else {
                sb.append("🥶 警报：笼舍温度过低！\n");
                sb.append("当前温度：").append(val).append("℃ (阈值: ").append(boundary).append("℃)\n");
                sb.append("建议措施：建议开启笼舍保温灯、空调，或加盖防风保暖罩，避免鹦鹉受凉生病。\n");
            }
        } else if ("humidity".equals(metric) || alarmType.startsWith("humidity")) {
            if (alarmType.endsWith("high")) {
                sb.append("💦 警报：笼舍湿度过高！\n");
                sb.append("当前湿度：").append(val).append("% (阈值: ").append(boundary).append("%)\n");
                sb.append("建议措施：请开启通风排湿，检查垫料是否受潮，预防霉菌与呼吸道寄生虫滋生。\n");
            } else {
                sb.append("🌵 警报：笼舍湿度过低！\n");
                sb.append("当前湿度：").append(val).append("% (阈值: ").append(boundary).append("%)\n");
                sb.append("建议措施：可在笼舍周围放置浅水盆或使用室内加湿器，保持羽毛和粘膜润泽。\n");
            }
        } else {
            // 烟雾或粉尘
            String levelText = "🔴 高危";
            if ("medium".equals(event.riskLevel())) {
                levelText = "🟠 中危";
            } else if ("low".equals(event.riskLevel())) {
                levelText = "🟡 低危";
            }
            sb.append(levelText).append(" 警报：烟雾/粉尘浓度超标！\n");
            sb.append("当前浓度：").append((int)Math.round(val)).append("ppm (阈值: ").append((int)Math.round(boundary)).append("ppm)\n");
            sb.append("建议措施：⚠️ 监测到空气粉尘或烟雾过高！请立刻排查周围是否有烟雾、火源或积尘，如有危险迅速打开排风系统并将宠物疏散到安全区域！\n");
        }

        sb.append("检测时间：").append(event.triggeredAt()).append("\n");
        if (event.simulated()) {
            sb.append("（本条为系统模拟测试告警）\n");
        }
        sb.append("回复“处理告警”可转前端处理");
        return sb.toString();
    }

    /**
     * 将 base64 格式的截图通过 CQ 码直接发送给 QQ 用户。
     */
    public void sendScreenshotToQq(long qqNumber, String base64Image) {
        if (base64Image == null || base64Image.isBlank()) {
            oneBotClient.sendPrivateMsg(qqNumber, "⚠️ 截图失败，获取的图像数据为空。");
            return;
        }
        String cleanBase64 = base64Image.trim();
        int commaIndex = cleanBase64.indexOf(",");
        if (commaIndex >= 0) {
            cleanBase64 = cleanBase64.substring(commaIndex + 1);
        }
        cleanBase64 = cleanBase64.replaceAll("\\s+", "");

        try {
            // 解码并写入本地临时文件，这是最稳定可靠的本地 QQ 返图方案
            byte[] bytes = java.util.Base64.getDecoder().decode(cleanBase64);
            java.io.File tempFile = java.io.File.createTempFile("parrot_screenshot_", ".jpg");
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
                fos.write(bytes);
            }
            tempFile.deleteOnExit();

            String filePath = tempFile.getAbsolutePath().replace("\\", "/");
            String message = "[CQ:image,file=file:///" + filePath + "]";
            log.info("生成临时截图成功，正在推送到 QQ: path={}", filePath);
            oneBotClient.sendPrivateMsg(qqNumber, message);
        } catch (Exception e) {
            log.warn("通过临时文件发送截图失败，尝试降级为 Base64 直送: reason={}", e.getMessage());
            // 降级：直接以 Base64 CQ 码发送
            try {
                String message = "[CQ:image,file=base64://" + cleanBase64 + "]";
                oneBotClient.sendPrivateMsg(qqNumber, message);
            } catch (Exception ex) {
                log.error("截图发送彻底失败: qqNumber={}, reason={}", qqNumber, ex.getMessage(), ex);
                oneBotClient.sendPrivateMsg(qqNumber, "⚠️ 截图返图失败，请检查网络或控制台日志。");
            }
        }
    }
}

package com.chinasoft.smokesensor.service.qq;

import com.chinasoft.smokesensor.config.OneBotProperties;
import com.chinasoft.smokesensor.dto.AlarmTodayStatResponse;
import com.chinasoft.smokesensor.dto.DeviceStatusResponse;
import com.chinasoft.smokesensor.dto.PetProfileResponse;
import com.chinasoft.smokesensor.dto.SmokeRealtimeResponse;
import com.chinasoft.smokesensor.service.AlarmService;
import com.chinasoft.smokesensor.service.DeviceOnlineStatusService;
import com.chinasoft.smokesensor.service.DeviceOnlineStatusService.DeviceOnlineStatus;
import com.chinasoft.smokesensor.service.DeviceService;
import com.chinasoft.smokesensor.service.PetProfileService;
import com.chinasoft.smokesensor.service.SmokeService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * QQ 定时推送调度器。
 *
 * <p>三个定时任务：
 * <ol>
 *   <li><b>每日 8:30 环境晨报</b> - 当前浓度温湿度 + 今日告警统计 + 设备在线状态</li>
 *   <li><b>每日 9:00 宠物成长日报</b> - 首只宠物档案摘要（名字 / 品种 / 体重 / 状态）</li>
 *   <li><b>每 60s 设备离线检测</b> - 在线状态翻转时推送（在线→离线 / 离线→在线），
 *       避免持续离线时重复刷屏</li>
 * </ol>
 *
 * <p>所有任务在 QQ 机器人未启用或未配置 push-target-user 时静默跳过。
 * 离线检测用 Redis 记录上次状态（key=qq:device:online:{deviceId}），
 * 仅在线↔离线翻转时推送；首次检测只记录不推送，避免应用启动时误报。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OneBotPushScheduler {

    private static final String DEFAULT_DEVICE_ID = "SMK-001";
    private static final String ONLINE_STATE_KEY = "qq:device:online:" + DEFAULT_DEVICE_ID;

    private final OneBotPushService pushService;
    private final SmokeService smokeService;
    private final AlarmService alarmService;
    private final DeviceService deviceService;
    private final DeviceOnlineStatusService deviceOnlineStatusService;
    private final PetProfileService petProfileService;
    private final OneBotProperties properties;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 每日 8:30 推送环境晨报。
     */
    @Scheduled(cron = "0 30 8 * * *")
    public void pushDailyReport() {
        if (!shouldPush()) {
            return;
        }
        try {
            pushService.pushMessage(buildDailyReport());
            log.info("每日环境晨报已推送");
        } catch (Exception e) {
            log.warn("每日环境晨报推送失败: {}", e.getMessage());
        }
    }

    /**
     * 每日 9:00 推送宠物成长日报。无宠物档案时跳过。
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void pushPetDailyReport() {
        if (!shouldPush()) {
            return;
        }
        try {
            String report = buildPetReport();
            if (report != null) {
                pushService.pushMessage(report);
                log.info("宠物成长日报已推送");
            }
        } catch (Exception e) {
            log.warn("宠物成长日报推送失败: {}", e.getMessage());
        }
    }

    /**
     * 每 60 秒检测设备在线状态，状态翻转时推送离线 / 恢复通知。
     */
    @Scheduled(fixedRate = 60000)
    public void checkDeviceOffline() {
        if (!shouldPush()) {
            return;
        }
        try {
            DeviceOnlineStatus status = deviceOnlineStatusService.getStatus(DEFAULT_DEVICE_ID);
            Boolean lastOnline = loadOnlineState();
            saveOnlineState(status.online());

            if (lastOnline == null) {
                // 首次检测只记录当前状态，避免应用启动时误报
                return;
            }
            if (lastOnline && !status.online()) {
                log.warn("设备 {} 转为离线，推送告警", DEFAULT_DEVICE_ID);
                pushService.pushDeviceOffline(DEFAULT_DEVICE_ID, status.lastDataAt());
            } else if (!lastOnline && status.online()) {
                log.info("设备 {} 恢复在线，推送通知", DEFAULT_DEVICE_ID);
                pushService.pushDeviceRecover(DEFAULT_DEVICE_ID);
            }
        } catch (Exception e) {
            log.warn("设备离线检测失败: {}", e.getMessage());
        }
    }

    // ========================================================================
    // 报告文本组装
    // ========================================================================

    /**
     * 组装环境晨报：设备状态 + 当前浓度温湿度 + 今日告警统计。
     */
    private String buildDailyReport() {
        SmokeRealtimeResponse realtime = smokeService.getRealtimeSmoke(DEFAULT_DEVICE_ID);
        AlarmTodayStatResponse stat = alarmService.getTodayStat();
        DeviceStatusResponse device = deviceService.getDeviceStatus(DEFAULT_DEVICE_ID);

        StringBuilder sb = new StringBuilder();
        sb.append("📊 每日环境晨报（").append(LocalDate.now()).append("）\n");
        sb.append("设备 ").append(DEFAULT_DEVICE_ID).append("：")
                .append(Boolean.TRUE.equals(device.getConnected()) ? "🟢 在线" : "🔴 离线").append("\n");
        if (Boolean.TRUE.equals(realtime.getConnected())) {
            sb.append("浓度：").append(realtime.getSmokeValue() == null ? "-" : realtime.getSmokeValue());
            if (realtime.getRiskLevel() != null) {
                sb.append("（").append(realtime.getRiskLevel()).append("）");
            }
            sb.append("\n");
            sb.append("温度：").append(realtime.getTemperature() == null ? "-" : realtime.getTemperature()).append("℃  ");
            sb.append("湿度：").append(realtime.getHumidity() == null ? "-" : realtime.getHumidity()).append("%RH\n");
        }
        sb.append("今日告警：").append(stat.getTodayCount()).append(" 次");
        sb.append("（昨日 ").append(stat.getYesterdayCount()).append(" 次）");
        return sb.toString();
    }

    /**
     * 组装宠物成长日报。无宠物档案返回 null（调用方据此跳过推送）。
     */
    private String buildPetReport() {
        List<PetProfileResponse> pets = petProfileService.listProfiles();
        if (pets == null || pets.isEmpty()) {
            return null;
        }
        PetProfileResponse pet = pets.get(0);
        StringBuilder sb = new StringBuilder();
        sb.append("🐦 宠物成长日报（").append(LocalDate.now()).append("）\n");
        sb.append("名字：").append(pet.getName()).append("\n");
        sb.append("品种：").append(pet.getSpecies());
        if (pet.getWeightGrams() != null) {
            sb.append("\n体重：").append(pet.getWeightGrams()).append("g");
        }
        if (pet.getCurrentStatus() != null) {
            sb.append("\n状态：").append(pet.getCurrentStatus());
        }
        return sb.toString();
    }

    // ========================================================================
    // Redis 在线状态存取（容错，参照项目缓存容错风格）
    // ========================================================================

    private boolean shouldPush() {
        return properties.isEnabled() && properties.getPushTargetUserId() != null;
    }

    private Boolean loadOnlineState() {
        try {
            Object value = redisTemplate.opsForValue().get(ONLINE_STATE_KEY);
            return value instanceof Boolean b ? b : null;
        } catch (Exception e) {
            log.warn("Redis 读取在线状态失败: {}", e.getMessage());
            return null;
        }
    }

    private void saveOnlineState(boolean online) {
        try {
            redisTemplate.opsForValue().set(ONLINE_STATE_KEY, online);
        } catch (Exception e) {
            log.warn("Redis 写入在线状态失败: {}", e.getMessage());
        }
    }
}

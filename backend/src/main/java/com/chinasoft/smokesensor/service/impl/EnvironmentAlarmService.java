package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.config.AlarmWebSocketSessionManager;
import com.chinasoft.smokesensor.dto.AlarmWebSocketPayload;
import com.chinasoft.smokesensor.entity.AlarmRecord;
import com.chinasoft.smokesensor.entity.PetProfile;
import com.chinasoft.smokesensor.repository.AlarmRecordRepository;
import com.chinasoft.smokesensor.repository.HumidityDataRepository;
import com.chinasoft.smokesensor.repository.PetProfileRepository;
import com.chinasoft.smokesensor.repository.SensorDataRepository;
import com.chinasoft.smokesensor.repository.TemperatureDataRepository;
import com.chinasoft.smokesensor.repository.UserPreferenceRepository;
import com.chinasoft.smokesensor.service.alarm.AlarmTriggeredEvent;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 按“用户 + 已绑定设备”持续检查环境范围。阈值只从 user_preference 读取，
 * 因此同一设备为不同用户提供不同的告警边界，不会串用设置。
 */
@Service
@RequiredArgsConstructor
public class EnvironmentAlarmService {
    private static final Set<String> OPEN = Set.of("unhandled", "pending", "processing");
    private static final Map<String, Double> DEFAULTS = Map.ofEntries(
            Map.entry("environment_temperature_lower", 18D), Map.entry("environment_temperature_upper", 30D),
            Map.entry("environment_humidity_lower", 40D), Map.entry("environment_humidity_upper", 70D),
            Map.entry("environment_dust_lower", 0D), Map.entry("environment_dust_upper", 35D));

    private final PetProfileRepository petProfileRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final TemperatureDataRepository temperatureDataRepository;
    private final HumidityDataRepository humidityDataRepository;
    private final SensorDataRepository sensorDataRepository;
    private final AlarmRecordRepository alarmRecordRepository;
    private final AlarmWebSocketSessionManager socketSessionManager;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void evaluateActiveBindings() {
        Set<String> seen = new HashSet<>();
        for (PetProfile profile : petProfileRepository.findByEnabledTrueOrderByUpdatedAtDesc()) {
            if (profile.getUserId() == null || profile.getDeviceId() == null || profile.getDeviceId().isBlank()) continue;
            String key = profile.getUserId() + ":" + profile.getDeviceId();
            if (!seen.add(key)) continue;
            evaluateUserDevice(profile.getUserId(), profile.getDeviceId());
        }
    }

    void evaluateUserDevice(Long userId, String deviceId) {
        Map<String, Double> thresholds = loadThresholds(userId);
        Double temp = temperatureDataRepository.findTopByDeviceIdOrderByRecordTimeDesc(deviceId)
                .map(row -> row.getTemperatureValue().doubleValue()).orElse(null);
        Double humidity = humidityDataRepository.findTopByDeviceIdOrderByRecordTimeDesc(deviceId)
                .map(row -> row.getHumidityValue().doubleValue()).orElse(null);
        Double dust = sensorDataRepository.findTopByDeviceIdOrderByRecordTimeDesc(deviceId)
                .map(row -> row.getSmokeValue().doubleValue()).orElse(null);

        Double tempLower = thresholds.get("environment_temperature_lower");
        Double tempUpper = thresholds.get("environment_temperature_upper");
        Double humidityLower = thresholds.get("environment_humidity_lower");
        Double humidityUpper = thresholds.get("environment_humidity_upper");
        Double dustLower = thresholds.get("environment_dust_lower");
        Double dustUpper = thresholds.get("environment_dust_upper");

        Integer tempScore = scoreRange(temp, tempLower, tempUpper);
        Integer humidityScore = scoreRange(humidity, humidityLower, humidityUpper);
        Integer dustScore = scoreThreshold(dust, dustUpper, 80.0);

        double wsum = 0.0;
        double scoreSum = 0.0;
        if (tempScore != null) {
            wsum += 0.35;
            scoreSum += tempScore * 0.35;
        }
        if (humidityScore != null) {
            wsum += 0.25;
            scoreSum += humidityScore * 0.25;
        }
        if (dustScore != null) {
            wsum += 0.4;
            scoreSum += dustScore * 0.4;
        }

        Integer envScore = wsum > 0.0 ? (int) Math.round(scoreSum / wsum) : null;

        if (temp != null) {
            evaluateMetric(userId, deviceId, "temperature", temp, "℃", tempLower, tempUpper);
        }
        if (humidity != null) {
            evaluateMetric(userId, deviceId, "humidity", humidity, "%RH", humidityLower, humidityUpper);
        }
        if (dust != null) {
            evaluateMetric(userId, deviceId, "dust", dust, "ppm", dustLower, dustUpper);
        }

        if (envScore != null) {
            evaluateEnvironmentScore(userId, deviceId, envScore, temp, humidity, dust);
        } else {
            resolveOpen(userId, deviceId, "environment_score_low");
        }
    }

    private Map<String, Double> loadThresholds(Long userId) {
        Map<String, Double> result = new java.util.HashMap<>(DEFAULTS);
        userPreferenceRepository.findByUserIdOrderByPrefGroupAscPrefKeyAsc(userId).forEach(pref -> {
            if (!DEFAULTS.containsKey(pref.getPrefKey())) return;
            try { result.put(pref.getPrefKey(), Double.parseDouble(pref.getPrefValue())); } catch (NumberFormatException ignored) { }
        });
        return result;
    }

    private void evaluateMetric(Long userId, String deviceId, String metric, double value, String unit, double lower, double upper) {
        String type = value < lower ? metric + "_low" : value > upper ? metric + "_high" : null;
        for (String side : List.of(metric + "_low", metric + "_high")) {
            if (!side.equals(type)) resolveOpen(userId, deviceId, side);
        }
        if (type == null || !alarmRecordRepository.findByUserIdAndDeviceIdAndAlarmTypeAndStatusIn(userId, deviceId, type, OPEN).isEmpty()) return;
        double boundary = type.endsWith("_low") ? lower : upper;
        String direction = type.endsWith("_low") ? "低于下界" : "高于上界";
        LocalDateTime now = LocalDateTime.now();
        String message = metricLabel(metric) + value + unit + direction + boundary + unit;
        AlarmRecord saved = alarmRecordRepository.save(AlarmRecord.builder()
                .alarmId(UUID.randomUUID().toString()).userId(userId).deviceId(deviceId).alarmType(type)
                .alarmMessage(message).alarmValue(value).thresholdValue(boundary)
                .smokeValue("dust".equals(metric) ? (int) Math.round(value) : null)
                .riskLevel("high").status("unhandled").remark(message).triggeredAt(now)
                .isSimulated(false).createTime(now).updatedAt(now).build());
        AlarmWebSocketPayload payload = AlarmWebSocketPayload.builder().type("environment_alarm")
                .alarmId(saved.getAlarmId()).userId(userId).deviceId(deviceId).metric(metric).metricValue(value)
                .thresholdValue(boundary).unit(unit).level("high").message(message).alarmTime(now).build();
        socketSessionManager.broadcastAlarmToUser(userId, payload);
        eventPublisher.publishEvent(new AlarmTriggeredEvent(saved.getAlarmId(), deviceId, type, saved.getSmokeValue(),
                "high", now, false, userId, metric, value, boundary, unit));
    }

    private void resolveOpen(Long userId, String deviceId, String type) {
        List<AlarmRecord> records = alarmRecordRepository.findByUserIdAndDeviceIdAndAlarmTypeAndStatusIn(userId, deviceId, type, OPEN);
        if (records.isEmpty()) return;
        LocalDateTime now = LocalDateTime.now();
        records.forEach(record -> { record.setStatus("resolved"); record.setResolvedAt(now); record.setUpdatedAt(now); });
        alarmRecordRepository.saveAll(records);
    }

    private String metricLabel(String metric) {
        return switch (metric) { case "temperature" -> "温度"; case "humidity" -> "湿度"; default -> "粉尘浓度"; };
    }

    private Integer scoreRange(Double value, Double min, Double max) {
        if (value == null || min == null || max == null) return null;
        if (value < min) return (int) Math.max(0, Math.round(100 - (min - value) * 8));
        if (value > max) return (int) Math.max(0, Math.round(100 - (value - max) * 8));
        return 100;
    }

    private Integer scoreThreshold(Double value, Double good, Double warn) {
        if (value == null || good == null || warn == null) return null;
        if (value < good) return 100;
        if (value < warn) return (int) Math.round(100 - ((value - good) / (warn - good)) * 40);
        return (int) Math.max(0, Math.round(60 - ((value - warn) / Math.max(warn, 1.0)) * 60));
    }

    private void evaluateEnvironmentScore(Long userId, String deviceId, int score, Double temp, Double humidity, Double dust) {
        String type = "environment_score_low";
        if (score >= 60) {
            resolveOpen(userId, deviceId, type);
            return;
        }

        if (!alarmRecordRepository.findByUserIdAndDeviceIdAndAlarmTypeAndStatusIn(userId, deviceId, type, OPEN).isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        String message = "环境适配度评分过低：" + score + "分。";
        AlarmRecord saved = alarmRecordRepository.save(AlarmRecord.builder()
                .alarmId(UUID.randomUUID().toString()).userId(userId).deviceId(deviceId).alarmType(type)
                .alarmMessage(message).alarmValue((double) score).thresholdValue(60.0)
                .riskLevel("medium").status("unhandled").remark(message).triggeredAt(now)
                .isSimulated(false).createTime(now).updatedAt(now).build());

        AlarmWebSocketPayload payload = AlarmWebSocketPayload.builder().type("environment_alarm")
                .alarmId(saved.getAlarmId()).userId(userId).deviceId(deviceId).metric("environment_score")
                .metricValue((double) score).thresholdValue(60.0).unit("分").level("medium")
                .message(message).alarmTime(now).build();
        socketSessionManager.broadcastAlarmToUser(userId, payload);

        eventPublisher.publishEvent(new AlarmTriggeredEvent(saved.getAlarmId(), deviceId, type, null,
                "medium", now, false, userId, "environment_score", (double) score, 60.0, "分"));
    }
}

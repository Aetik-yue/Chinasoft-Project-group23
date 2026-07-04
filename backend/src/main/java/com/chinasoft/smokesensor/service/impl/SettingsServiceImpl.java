package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.dto.ThresholdSettingsRequest;
import com.chinasoft.smokesensor.dto.ThresholdSettingsResponse;
import com.chinasoft.smokesensor.entity.SystemSetting;
import com.chinasoft.smokesensor.repository.SystemSettingRepository;
import com.chinasoft.smokesensor.service.SettingsService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 阈值配置统一来自 system_setting 表：
// warning_threshold：中风险阈值，同时作为告警触发阈值。  200
// danger_threshold：高风险阈值。                       400
// heartbeat_timeout：设备离线超时时间，单位秒。         300
// 注意：数据库当前没有 normal/low 分界配置，因此 normalMax 暂时固定为 100。

@Service
@RequiredArgsConstructor
public class SettingsServiceImpl implements SettingsService {

    private static final String KEY_WARNING_THRESHOLD = "warning_threshold";
    private static final String KEY_DANGER_THRESHOLD = "danger_threshold";
    private static final String KEY_UNIT = "unit";
    private static final String KEY_HEARTBEAT_TIMEOUT = "heartbeat_timeout";

    // 注意：当前 system_setting 表没有 normal/low 分界配置，暂时固定为 100。
    private static final int DEFAULT_NORMAL_MAX = 100;
    private static final int DEFAULT_WARNING_THRESHOLD = 200;
    private static final int DEFAULT_DANGER_THRESHOLD = 400;
    private static final int DEFAULT_HEARTBEAT_TIMEOUT = 300;
    private static final String DEFAULT_UNIT = "ppm";

    private final SystemSettingRepository systemSettingRepository;

    @Override
    @Transactional(readOnly = true)
    public ThresholdSettingsResponse getThresholdSettings() {
        return buildResponse(loadSettings());
    }

    @Override
    @Transactional
    public ThresholdSettingsResponse updateThresholdSettings(ThresholdSettingsRequest request) {
        ThresholdSettingsResponse current = getThresholdSettings();
        int warningThreshold = request.getWarningThreshold() == null
                ? current.getWarningThreshold()
                : request.getWarningThreshold();
        int dangerThreshold = request.getDangerThreshold() == null
                ? current.getDangerThreshold()
                : request.getDangerThreshold();
        int heartbeatTimeout = request.getHeartbeatTimeout() == null
                ? current.getHeartbeatTimeout()
                : request.getHeartbeatTimeout();

        // 注意：告警阈值直接使用 warning_threshold，smokeValue >= warning_threshold 时进入告警状态，
        // 因此 danger_threshold 必须大于 warning_threshold。
        if (warningThreshold <= DEFAULT_NORMAL_MAX) {
            throw new IllegalArgumentException("warningThreshold must be greater than " + DEFAULT_NORMAL_MAX);
        }
        if (dangerThreshold <= warningThreshold) {
            throw new IllegalArgumentException("dangerThreshold must be greater than warningThreshold");
        }

        updateSetting(KEY_WARNING_THRESHOLD, String.valueOf(warningThreshold));
        updateSetting(KEY_DANGER_THRESHOLD, String.valueOf(dangerThreshold));
        updateSetting(KEY_HEARTBEAT_TIMEOUT, String.valueOf(heartbeatTimeout));
        if (request.getUnit() != null && !request.getUnit().isBlank()) {
            updateSetting(KEY_UNIT, request.getUnit().trim());
        }
        return buildResponse(loadSettings());
    }

    private Map<String, SystemSetting> loadSettings() {
        return systemSettingRepository.findBySettingKeyIn(List.of(
                        KEY_WARNING_THRESHOLD,
                        KEY_DANGER_THRESHOLD,
                        KEY_UNIT,
                        KEY_HEARTBEAT_TIMEOUT))
                .stream()
                .collect(Collectors.toMap(SystemSetting::getSettingKey, Function.identity()));
    }

    private void updateSetting(String key, String value) {
        SystemSetting setting = systemSettingRepository.findBySettingKey(key)
                .orElseThrow(() -> new IllegalArgumentException("system_setting missing key: " + key));
        setting.setSettingValue(value);
        systemSettingRepository.save(setting);
    }

    private ThresholdSettingsResponse buildResponse(Map<String, SystemSetting> settings) {
        int warningThreshold = intValue(settings, KEY_WARNING_THRESHOLD, DEFAULT_WARNING_THRESHOLD);
        int dangerThreshold = intValue(settings, KEY_DANGER_THRESHOLD, DEFAULT_DANGER_THRESHOLD);
        int heartbeatTimeout = intValue(settings, KEY_HEARTBEAT_TIMEOUT, DEFAULT_HEARTBEAT_TIMEOUT);
        String unit = stringValue(settings, KEY_UNIT, DEFAULT_UNIT);
        LocalDateTime updatedAt = settings.values().stream()
                .map(SystemSetting::getUpdatedAt)
                .filter(value -> value != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return ThresholdSettingsResponse.builder()
                .normalMax(DEFAULT_NORMAL_MAX)
                .warningThreshold(warningThreshold)
                .dangerThreshold(dangerThreshold)
                .alarmThreshold(warningThreshold)
                .heartbeatTimeout(heartbeatTimeout)
                .unit(unit)
                .updatedAt(updatedAt)
                .build();
    }

    private int intValue(Map<String, SystemSetting> settings, String key, int defaultValue) {
        SystemSetting setting = settings.get(key);
        if (setting == null || setting.getSettingValue() == null || setting.getSettingValue().isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(setting.getSettingValue());
    }

    private String stringValue(Map<String, SystemSetting> settings, String key, String defaultValue) {
        SystemSetting setting = settings.get(key);
        if (setting == null || setting.getSettingValue() == null || setting.getSettingValue().isBlank()) {
            return defaultValue;
        }
        return setting.getSettingValue();
    }
}

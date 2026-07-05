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

/**
 * 系统阈值配置业务实现。
 *
 * <p>阈值配置统一读取 system_setting 表：
 * warning_threshold 用于中风险阈值，同时作为烟雾告警触发阈值；
 * danger_threshold 用于高风险阈值；
 * heartbeat_timeout 字段为兼容现有配置接口继续保留；设备在线判断现已固定使用 smoke_data 的 10 秒窗口。
 *
 * <p>注意：当前数据库没有 normal/low 分界配置，因此 normalMax 暂时固定为 100。
 */
@Service
@RequiredArgsConstructor
public class SettingsServiceImpl implements SettingsService {

    private static final String KEY_WARNING_THRESHOLD = "warning_threshold";
    private static final String KEY_DANGER_THRESHOLD = "danger_threshold";
    private static final String KEY_UNIT = "unit";
    private static final String KEY_HEARTBEAT_TIMEOUT = "heartbeat_timeout";

    private static final int DEFAULT_NORMAL_MAX = 100;
    private static final int DEFAULT_WARNING_THRESHOLD = 200;
    private static final int DEFAULT_DANGER_THRESHOLD = 400;
    private static final int DEFAULT_HEARTBEAT_TIMEOUT = 300;
    private static final String DEFAULT_UNIT = "ppm";

    private final SystemSettingRepository systemSettingRepository;

    /**
     * 读取当前阈值配置。
     *
     * <p>如果数据库缺少某个配置项，会使用代码内默认值兜底，
     * 但不会自动新增数据库配置项。
     */
    @Override
    @Transactional(readOnly = true)
    public ThresholdSettingsResponse getThresholdSettings() {
        return buildResponse(loadSettings());
    }

    /**
     * 更新阈值配置。
     *
     * <p>处理流程：
     * 1. 读取当前配置作为默认值；
     * 2. 校验 warningThreshold 必须大于 normalMax；
     * 3. 校验 dangerThreshold 必须大于 warningThreshold；
     * 4. 只更新 system_setting 中已有 key，不创建新表或新字段。
     */
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

        // 告警触发阈值直接使用 warning_threshold，smokeValue >= warning_threshold 时进入告警状态。
        if (warningThreshold <= DEFAULT_NORMAL_MAX) {
            throw new IllegalArgumentException("warningThreshold 必须大于 " + DEFAULT_NORMAL_MAX);
        }
        if (dangerThreshold <= warningThreshold) {
            throw new IllegalArgumentException("dangerThreshold 必须大于 warningThreshold");
        }

        updateSetting(KEY_WARNING_THRESHOLD, String.valueOf(warningThreshold));
        updateSetting(KEY_DANGER_THRESHOLD, String.valueOf(dangerThreshold));
        updateSetting(KEY_HEARTBEAT_TIMEOUT, String.valueOf(heartbeatTimeout));
        if (request.getUnit() != null && !request.getUnit().isBlank()) {
            updateSetting(KEY_UNIT, request.getUnit().trim());
        }
        return buildResponse(loadSettings());
    }

    /**
     * 从 system_setting 表读取当前后端关心的配置项。
     */
    private Map<String, SystemSetting> loadSettings() {
        return systemSettingRepository.findBySettingKeyIn(List.of(
                        KEY_WARNING_THRESHOLD,
                        KEY_DANGER_THRESHOLD,
                        KEY_UNIT,
                        KEY_HEARTBEAT_TIMEOUT))
                .stream()
                .collect(Collectors.toMap(SystemSetting::getSettingKey, Function.identity()));
    }

    /**
     * 更新指定配置项；配置项不存在时返回明确错误，不自动新增。
     */
    private void updateSetting(String key, String value) {
        SystemSetting setting = systemSettingRepository.findBySettingKey(key)
                .orElseThrow(() -> new IllegalArgumentException("system_setting 缺少配置项: " + key));
        setting.setSettingValue(value);
        systemSettingRepository.save(setting);
    }

    /**
     * 将数据库配置项组装为前端阈值配置响应。
     */
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

    /**
     * 读取整数配置；缺失或为空时使用默认值兜底。
     */
    private int intValue(Map<String, SystemSetting> settings, String key, int defaultValue) {
        SystemSetting setting = settings.get(key);
        if (setting == null || setting.getSettingValue() == null || setting.getSettingValue().isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(setting.getSettingValue());
    }

    /**
     * 读取字符串配置；缺失或为空时使用默认值兜底。
     */
    private String stringValue(Map<String, SystemSetting> settings, String key, String defaultValue) {
        SystemSetting setting = settings.get(key);
        if (setting == null || setting.getSettingValue() == null || setting.getSettingValue().isBlank()) {
            return defaultValue;
        }
        return setting.getSettingValue();
    }
}

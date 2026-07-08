package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.common.CacheKeys;
import com.chinasoft.smokesensor.dto.ThresholdSettingsRequest;
import com.chinasoft.smokesensor.dto.ThresholdSettingsResponse;
import com.chinasoft.smokesensor.entity.SystemSetting;
import com.chinasoft.smokesensor.repository.SystemSettingRepository;
import com.chinasoft.smokesensor.service.SettingsService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统阈值配置业务实现。
 *
 * <p>阈值配置统一读取 system_setting 表。
 * 由于阈值几乎不被修改但被频繁读取（每次请求/烟雾模拟/数据上传都会调用），
 * 新增了 Redis 缓存层以减少数据库查询压力。
 *
 * <p>缓存策略：Cache-Aside 模式
 * <ul>
 *   <li><b>读</b>：先查 Redis → 命中直接返回；未命中查 DB → 写入 Redis（TTL=10 分钟）</li>
 *   <li><b>写</b>：更新 DB → 主动刷新 Redis 缓存，保证下次读取为最新</li>
 * </ul>
 */
@Slf4j
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
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 读取当前阈值配置（优先从 Redis 缓存读取）。
     *
     * <p>如果数据库缺少某个配置项，会使用代码内默认值兜底，
     * 但不会自动新增数据库配置项。
     */
    @Override
    @Transactional(readOnly = true)
    public ThresholdSettingsResponse getThresholdSettings() {
        // 1. 从 Redis 缓存读取
        String key = CacheKeys.settingsThreshold();
        Object cached = getCacheValue(key);
        if (cached instanceof ThresholdSettingsResponse cachedResp) {
            return cachedResp;
        }

        // 2. 缓存未命中 → 从数据库加载
        ThresholdSettingsResponse response = buildResponse(loadSettings());

        // 3. 写入 Redis 缓存（TTL=10 分钟），避免短时间内重复查表
        setCacheValue(key, response, Duration.ofSeconds(CacheKeys.TTL_SETTINGS_THRESHOLD));
        return response;
    }

    /**
     * 更新阈值配置。
     *
     * <p>处理流程：
     * 1. 读取当前配置作为默认值；
     * 2. 校验 warningThreshold 必须大于 normalMax；
     * 3. 校验 dangerThreshold 必须大于 warningThreshold；
     * 4. 只更新 system_setting 中已有 key，不创建新表或新字段；
     * 5. 更新完成后主动刷新 Redis 缓存，保证后续读取为最新值。
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

        // 更新数据库
        updateSetting(KEY_WARNING_THRESHOLD, String.valueOf(warningThreshold));
        updateSetting(KEY_DANGER_THRESHOLD, String.valueOf(dangerThreshold));
        updateSetting(KEY_HEARTBEAT_TIMEOUT, String.valueOf(heartbeatTimeout));
        if (request.getUnit() != null && !request.getUnit().isBlank()) {
            updateSetting(KEY_UNIT, request.getUnit().trim());
        }

        // 重新构建并刷新 Redis 缓存（Cache-Aside 写后刷新）
        ThresholdSettingsResponse updated = buildResponse(loadSettings());
        setCacheValue(
                CacheKeys.settingsThreshold(), updated,
                Duration.ofSeconds(CacheKeys.TTL_SETTINGS_THRESHOLD));
        log.info("阈值配置已更新，Redis 缓存已刷新，warning={}, danger={}",
                updated.getWarningThreshold(), updated.getDangerThreshold());
        return updated;
    }

    /**
     * 从 system_setting 表读取当前后端关心的配置项。
     */
    /**
     * Redis 只作为阈值配置缓存；读取失败时返回 null，让业务继续读取 system_setting。
     */
    private Object getCacheValue(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
            log.warn("Redis 阈值缓存读取失败，改为查询 MySQL，key={}, reason={}", key, e.getMessage());
            log.warn("Redis 阈值缓存读取失败，改为查询 MySQL，key={}, reason={}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 阈值配置以 MySQL 为准；Redis 写入失败只记录日志，不影响接口成功返回。
     */
    private void setCacheValue(String key, Object value, Duration ttl) {
        try {
            log.warn("Redis 阈值缓存写入失败，已忽略，key={}, reason={}", key, e.getMessage());
        } catch (Exception e) {
            log.warn("Redis 阈值缓存写入失败，已忽略，key={}, reason={}", key, e.getMessage());
        }
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

    /**
     * 更新指定配置项；配置项不存在时返回明确错误，不自动新增。
     */
                .orElseThrow(() -> new IllegalArgumentException("system_setting 缺少配置项: " + key));
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

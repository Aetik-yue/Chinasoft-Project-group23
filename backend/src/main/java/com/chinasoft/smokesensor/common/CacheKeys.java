package com.chinasoft.smokesensor.common;

/**
 * Redis 缓存键常量类。
 *
 * <p>全项目统一管理缓存键和过期时间，避免散落在各 Service 中的魔法字符串。
 * 所有方法返回完整的缓存 Key 字符串，外部调用时直接：{@code CacheKeys.smokeLatest("SMK-001")}。
 *
 * <p>缓存分层说明：
 * <ul>
 *   <li><b>5 秒级 TTL</b> — 传感器实时数据，配合前端 3 秒轮询，保证 ≤8 秒数据新鲜度</li>
 *   <li><b>60 秒级 TTL</b> — 聚合统计数据，1 分钟内的延迟可接受</li>
 *   <li><b>5~10 分钟级 TTL</b> — 配置类数据，变化不频繁但读取量大</li>
 * </ul>
 */
public final class CacheKeys {

    /** 私有构造器，防止实例化 */
    private CacheKeys() {}

    // ========================================================================
    // 缓存键前缀常量
    // ========================================================================

    /** 最新烟雾数据前缀：smoke:latest:{deviceId} → SmokeLatestResponse JSON */
    private static final String PREFIX_SMOKE_LATEST = "smoke:latest:";
    /** 最新温度前缀：temp:latest:{deviceId} → Double */
    private static final String PREFIX_TEMP_LATEST = "temp:latest:";
    /** 最新湿度前缀：humidity:latest:{deviceId} → Double */
    private static final String PREFIX_HUMIDITY_LATEST = "humidity:latest:";
    /** 今日告警统计：alarm:stat:today → AlarmTodayStatResponse JSON */
    private static final String KEY_ALARM_STAT_TODAY = "alarm:stat:today";
    /** 系统阈值配置：settings:threshold → ThresholdSettingsResponse JSON */
    private static final String KEY_SETTINGS_THRESHOLD = "settings:threshold";

    // ========================================================================
    // TTL（过期时间）常量（单位：秒）
    // ========================================================================

    /** 传感器实时数据过期时间：5 秒（配合前端 3 秒轮询，避免数据库过载） */
    public static final long TTL_SENSOR_LATEST = 5;
    /** 告警统计过期时间：60 秒（1 分钟内的统计延迟可接受） */
    public static final long TTL_ALARM_STAT = 60;
    /** 系统配置过期时间：10 分钟（阈值几乎不改，但每次请求都读） */
    public static final long TTL_SETTINGS_THRESHOLD = 600;

    // ========================================================================
    // 缓存键生成方法
    // ========================================================================

    /**
     * 获取最新烟雾数据的缓存键。
     * 格式：smoke:latest:{deviceId}
     *
     * @param deviceId 设备编号，如 "SMK-001"
     * @return 完整的 Redis Key
     */
    public static String smokeLatest(String deviceId) {
        return PREFIX_SMOKE_LATEST + deviceId;
    }

    /**
     * 获取最新温度的缓存键。
     * 格式：temp:latest:{deviceId}
     *
     * @param deviceId 设备编号，如 "SMK-001"
     * @return 完整的 Redis Key
     */
    public static String tempLatest(String deviceId) {
        return PREFIX_TEMP_LATEST + deviceId;
    }

    /**
     * 获取最新湿度的缓存键。
     * 格式：humidity:latest:{deviceId}
     *
     * @param deviceId 设备编号，如 "SMK-001"
     * @return 完整的 Redis Key
     */
    public static String humidityLatest(String deviceId) {
        return PREFIX_HUMIDITY_LATEST + deviceId;
    }

    /**
     * 获取今日告警统计的缓存键。
     * 固定值：alarm:stat:today
     *
     * @return 完整的 Redis Key
     */
    public static String alarmStatToday() {
        return KEY_ALARM_STAT_TODAY;
    }

    /**
     * 获取系统阈值配置的缓存键。
     * 固定值：settings:threshold
     *
     * @return 完整的 Redis Key
     */
    public static String settingsThreshold() {
        return KEY_SETTINGS_THRESHOLD;
    }
}
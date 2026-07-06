package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.common.CacheKeys;
import com.chinasoft.smokesensor.dto.AlarmTodayStatResponse;
import com.chinasoft.smokesensor.dto.ThresholdSettingsResponse;
import com.chinasoft.smokesensor.repository.HumidityDataRepository;
import com.chinasoft.smokesensor.repository.TemperatureDataRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Redis 缓存定时刷新任务。
 *
 * <p>为了避免每次前端轮询都穿透到 MySQL，本定时器负责定期将数据库中的最新数据写入 Redis。
 * 前端请求时先查 Redis，命中直接返回，大幅降低 MySQL 负载。
 *
 * <p>刷新策略：
 * <ul>
 *   <li><b>传感器数据</b> — 每 3 秒查一次最新温湿度，写入 Redis（TTL=5 秒）</li>
 *   <li><b>告警统计</b> — 每 60 秒刷新今日告警次数（COUNT 查询），写入 Redis（TTL=60 秒）</li>
 * </ul>
 */
@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
public class CacheRefreshScheduler {

    /** 默认设备编号，与业务层统一定义 */
    private static final String DEFAULT_DEVICE_ID = "SMK-001";

    private final TemperatureDataRepository temperatureDataRepository;
    private final HumidityDataRepository humidityDataRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    // ========================================================================
    // 1. 传感器实时数据 — 每 3 秒刷新一次
    // ========================================================================

    /**
     * 每 3 秒将数据库中的最新温湿度数据写入 Redis。
     *
     * <p>配合前端 3 秒轮询 /smoke/realtime 接口，保证数据新鲜度 ≤6 秒。
     * 修改為 fixedRate=3000：不论上一次任务是否完成，每 3 秒执行一次。
     */
    @Scheduled(fixedRate = 3000)
    public void refreshSensorLatest() {
        try {
            // 1. 查询数据库最新温度
            LocalDateTime now = LocalDateTime.now();
            Double latestTemp = temperatureDataRepository
                    .findTopByDeviceIdOrderByRecordTimeDesc(DEFAULT_DEVICE_ID)
                    .map(data -> Double.valueOf(data.getTemperatureValue()))
                    .orElse(null);

            // 2. 查询数据库最新湿度
            Double latestHumidity = humidityDataRepository
                    .findTopByDeviceIdOrderByRecordTimeDesc(DEFAULT_DEVICE_ID)
                    .map(data -> Double.valueOf(data.getHumidityValue()))
                    .orElse(null);

            // 3. 写入 Redis（TTL=5秒）
            if (latestTemp != null) {
                redisTemplate.opsForValue().set(
                        CacheKeys.tempLatest(DEFAULT_DEVICE_ID),
                        latestTemp,
                        Duration.ofSeconds(CacheKeys.TTL_SENSOR_LATEST));
            }
            if (latestHumidity != null) {
                redisTemplate.opsForValue().set(
                        CacheKeys.humidityLatest(DEFAULT_DEVICE_ID),
                        latestHumidity,
                        Duration.ofSeconds(CacheKeys.TTL_SENSOR_LATEST));
            }
        } catch (Exception e) {
            // 定时任务异常不能中断主流程，只打印警告日志
            log.warn("刷新传感器缓存失败（3秒任务）: {}", e.getMessage());
        }
    }
}
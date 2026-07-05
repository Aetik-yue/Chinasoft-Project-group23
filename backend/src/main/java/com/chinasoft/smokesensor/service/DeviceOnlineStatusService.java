package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.entity.SensorData;
import com.chinasoft.smokesensor.repository.SensorDataRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 统一根据 smoke_data 的真实数据入库时间判断设备在线状态。
 *
 * <p>在线窗口固定为 10 秒，且只认可 source=sensor 或 source 为空的兼容数据。
 * smoke_device.online、last_heartbeat 以及温湿度表均不参与判断。</p>
 */
@Service
public class DeviceOnlineStatusService {

    static final long ONLINE_TIMEOUT_SECONDS = 10L;

    private final SensorDataRepository sensorDataRepository;
    private final Clock clock;

    /**
     * 生产环境使用系统默认时区；保留 Clock 注入点便于精确测试 10 秒边界。
     */
    @Autowired
    public DeviceOnlineStatusService(SensorDataRepository sensorDataRepository) {
        this(sensorDataRepository, Clock.systemDefaultZone());
    }

    DeviceOnlineStatusService(SensorDataRepository sensorDataRepository, Clock clock) {
        this.sensorDataRepository = sensorDataRepository;
        this.clock = clock;
    }

    /**
     * 查询指定设备的统一在线状态。
     */
    public DeviceOnlineStatus getStatus(String deviceId) {
        return toStatus(sensorDataRepository.findLatestRealDataByDeviceId(deviceId));
    }

    /**
     * 查询全局最新真实烟雾记录对应的在线状态，供未指定 deviceId 的 latest 接口使用。
     */
    public Optional<DeviceOnlineStatus> getLatestStatus() {
        return sensorDataRepository.findLatestRealData().map(this::toStatus);
    }

    /**
     * 统计当前在线的已登记设备数量。
     */
    public long countOnlineDevices() {
        return sensorDataRepository.countOnlineDevices(onlineThreshold());
    }

    private DeviceOnlineStatus toStatus(Optional<SensorData> latestData) {
        return latestData.map(this::toStatus)
                .orElseGet(() -> new DeviceOnlineStatus(false, null, null));
    }

    private DeviceOnlineStatus toStatus(SensorData latestData) {
        LocalDateTime lastDataAt = latestData.getCreatedAt();
        boolean online = lastDataAt != null && !lastDataAt.isBefore(onlineThreshold());
        return new DeviceOnlineStatus(online, lastDataAt, latestData);
    }

    /**
     * MySQL DATETIME 通常精确到秒，因此先截断纳秒，确保恰好 10 秒时仍判定在线。
     */
    private LocalDateTime onlineThreshold() {
        return LocalDateTime.now(clock)
                .truncatedTo(ChronoUnit.SECONDS)
                .minusSeconds(ONLINE_TIMEOUT_SECONDS);
    }

    /**
     * 在线判断结果，同时携带最后入库时间及对应烟雾记录，避免调用方重复查询。
     */
    public record DeviceOnlineStatus(boolean online, LocalDateTime lastDataAt, SensorData latestData) {
    }
}

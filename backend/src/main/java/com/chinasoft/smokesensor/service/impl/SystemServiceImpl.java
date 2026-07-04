package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.dto.SystemStatusResponse;
import com.chinasoft.smokesensor.repository.DeviceRepository;
import com.chinasoft.smokesensor.service.SettingsService;
import com.chinasoft.smokesensor.service.SystemService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统状态业务实现。
 *
 * <p>用于前端系统状态卡片，统计系统当前时间、设备总数和在线设备数。
 */
@Service
@RequiredArgsConstructor
public class SystemServiceImpl implements SystemService {

    private final DeviceRepository deviceRepository;
    private final SettingsService settingsService;

    /**
     * 查询系统运行状态。
     *
     * <p>在线设备数量不直接相信 smoke_device.online 字段，
     * 而是统一使用 lastHeartbeat + heartbeat_timeout 判断。
     */
    @Override
    @Transactional(readOnly = true)
    public SystemStatusResponse getSystemStatus() {
        LocalDateTime currentTime = LocalDateTime.now();
        // 注意：设备在线统计统一使用 system_setting.heartbeat_timeout。
        LocalDateTime onlineThreshold = currentTime.minusSeconds(
                settingsService.getThresholdSettings().getHeartbeatTimeout());
        return SystemStatusResponse.builder()
                .systemOnline(true)
                .currentTime(currentTime)
                .onlineDeviceCount(deviceRepository.countByLastHeartbeatGreaterThanEqual(onlineThreshold))
                .totalDeviceCount(deviceRepository.count())
                .build();
    }
}

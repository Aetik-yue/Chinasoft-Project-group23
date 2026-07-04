package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.dto.SystemStatusResponse;
import com.chinasoft.smokesensor.repository.DeviceRepository;
import com.chinasoft.smokesensor.service.SettingsService;
import com.chinasoft.smokesensor.service.SystemService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemServiceImpl implements SystemService {

    private final DeviceRepository deviceRepository;
    private final SettingsService settingsService;

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

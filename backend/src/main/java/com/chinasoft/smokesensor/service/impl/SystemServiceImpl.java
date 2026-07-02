package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.dto.SystemStatusResponse;
import com.chinasoft.smokesensor.repository.DeviceRepository;
import com.chinasoft.smokesensor.service.SystemService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemServiceImpl implements SystemService {

    private static final long OFFLINE_TIMEOUT_SECONDS = 60;

    private final DeviceRepository deviceRepository;

    @Override
    @Transactional(readOnly = true)
    public SystemStatusResponse getSystemStatus() {
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime onlineThreshold = currentTime.minusSeconds(OFFLINE_TIMEOUT_SECONDS);
        return SystemStatusResponse.builder()
                .systemOnline(true)
                .currentTime(currentTime)
                .onlineDeviceCount(deviceRepository.countByLastHeartbeatGreaterThanEqual(onlineThreshold))
                .totalDeviceCount(deviceRepository.count())
                .build();
    }
}

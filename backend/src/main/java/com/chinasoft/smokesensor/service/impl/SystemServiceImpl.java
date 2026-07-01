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

    private final DeviceRepository deviceRepository;

    @Override
    @Transactional(readOnly = true)
    public SystemStatusResponse getSystemStatus() {
        return SystemStatusResponse.builder()
                .systemOnline(true)
                .currentTime(LocalDateTime.now())
                .onlineDeviceCount(deviceRepository.countByOnlineTrue())
                .totalDeviceCount(deviceRepository.count())
                .build();
    }
}

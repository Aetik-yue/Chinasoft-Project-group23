package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.dto.SystemStatusResponse;
import com.chinasoft.smokesensor.repository.DeviceRepository;
import com.chinasoft.smokesensor.service.DeviceOnlineStatusService;
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
    private final DeviceOnlineStatusService deviceOnlineStatusService;

    /**
     * 查询系统运行状态。
     *
     * <p>在线设备数量统一根据 smoke_data 最新真实数据的 created_at 判断。
     */
    @Override
    @Transactional(readOnly = true)
    public SystemStatusResponse getSystemStatus() {
        LocalDateTime currentTime = LocalDateTime.now();
        return SystemStatusResponse.builder()
                .systemOnline(true)
                .currentTime(currentTime)
                .onlineDeviceCount(deviceOnlineStatusService.countOnlineDevices())
                .totalDeviceCount(deviceRepository.count())
                .build();
    }
}

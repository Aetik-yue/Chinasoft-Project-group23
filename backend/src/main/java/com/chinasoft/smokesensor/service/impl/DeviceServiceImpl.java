package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.common.BusinessException;
import com.chinasoft.smokesensor.dto.DeviceStatusResponse;
import com.chinasoft.smokesensor.entity.Device;
import com.chinasoft.smokesensor.repository.DeviceRepository;
import com.chinasoft.smokesensor.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;

    @Override
    @Transactional(readOnly = true)
    public DeviceStatusResponse getDeviceStatus(String deviceId) {
        Device device = findDevice(deviceId);
        return DeviceStatusResponse.builder()
                .deviceId(device.getDeviceId())
                .online(device.getOnline())
                .lastHeartbeat(device.getLastHeartbeat())
                .status(resolveStatus(device))
                .build();
    }

    private Device findDevice(String deviceId) {
        if (deviceId != null && !deviceId.isBlank()) {
            return deviceRepository.findByDeviceId(deviceId)
                    .orElseThrow(() -> BusinessException.notFound("Device not found: " + deviceId));
        }
        return deviceRepository.findTopByOrderByUpdatedAtDesc()
                .orElseThrow(() -> BusinessException.notFound("No device found"));
    }

    private String resolveStatus(Device device) {
        if (Boolean.FALSE.equals(device.getOnline())) {
            return "offline";
        }
        if ("alarm".equalsIgnoreCase(device.getCurrentAlarmStatus())) {
            return "alarm";
        }
        return Boolean.TRUE.equals(device.getOnline()) ? "online" : "unknown";
    }
}

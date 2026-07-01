package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.common.BusinessException;
import com.chinasoft.smokesensor.dto.DeviceControlRequest;
import com.chinasoft.smokesensor.dto.DeviceControlResponse;
import com.chinasoft.smokesensor.dto.DeviceStatusResponse;
import com.chinasoft.smokesensor.entity.Device;
import com.chinasoft.smokesensor.repository.DeviceRepository;
import com.chinasoft.smokesensor.service.DeviceService;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private static final Set<String> SUPPORTED_DEVICE_TYPES = Set.of("buzzer", "alarm_light", "fan");
    private static final Set<String> SUPPORTED_STATUSES = Set.of("on", "off");
    private static final String CONTROL_SUCCESS_MESSAGE = "设备控制指令已下发";

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

    @Override
    @Transactional(readOnly = true)
    public DeviceControlResponse controlDevice(DeviceControlRequest request) {
        String deviceId = normalizeRequired(request.getDeviceId(), "deviceId不能为空");
        String deviceType = normalizeRequired(request.getDeviceType(), "deviceType不能为空");
        String status = normalizeRequired(request.getStatus(), "status不能为空");

        if (!SUPPORTED_DEVICE_TYPES.contains(deviceType)) {
            throw new IllegalArgumentException("deviceType只能是 buzzer、alarm_light、fan");
        }
        if (!SUPPORTED_STATUSES.contains(status)) {
            throw new IllegalArgumentException("status只能是 on 或 off");
        }
        if (!deviceRepository.existsByDeviceId(deviceId)) {
            throw BusinessException.notFound("Device not found: " + deviceId);
        }

        return DeviceControlResponse.builder()
                .success(true)
                .message(CONTROL_SUCCESS_MESSAGE)
                .deviceId(deviceId)
                .deviceType(deviceType)
                .status(status)
                .operatedAt(LocalDateTime.now())
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

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}

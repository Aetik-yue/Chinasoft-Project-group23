package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.common.BusinessException;
import com.chinasoft.smokesensor.dto.DeviceControlRequest;
import com.chinasoft.smokesensor.dto.DeviceControlResponse;
import com.chinasoft.smokesensor.dto.DeviceInfoResponse;
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

    private static final long OFFLINE_TIMEOUT_SECONDS = 60;
    private static final String OFFLINE_MESSAGE = "设备未连接";

    private static final Set<String> SUPPORTED_DEVICE_TYPES = Set.of("buzzer", "alarm_light", "fan");
    private static final Set<String> SUPPORTED_STATUSES = Set.of("on", "off");
    private static final String CONTROL_SUCCESS_MESSAGE = "设备控制指令已下发";

    private final DeviceRepository deviceRepository;

    @Override
    @Transactional(readOnly = true)
    public DeviceStatusResponse getDeviceStatus(String deviceId) {
        Device device = findDevice(deviceId);
        boolean offline = isDeviceOffline(device);
        return DeviceStatusResponse.builder()
                .deviceId(device.getDeviceId())
                .deviceName(resolveDeviceName(device))
                .online(!offline)
                .connected(!offline)
                .lastHeartbeat(device.getLastHeartbeat())
                .status(offline ? "offline" : resolveStatus(device))
                .message(offline ? OFFLINE_MESSAGE : null)
                .progress(offline ? 0 : 100)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceInfoResponse getDeviceInfo(String deviceId) {
        Device device = findDevice(deviceId);
        boolean offline = isDeviceOffline(device);
        return DeviceInfoResponse.builder()
                .deviceId(device.getDeviceId())
                .deviceName(resolveDeviceName(device))
                .model(null)
                .firmwareVersion(null)
                .location(device.getLocation())
                .lastHeartbeat(device.getLastHeartbeat())
                .connected(!offline)
                .message(offline ? OFFLINE_MESSAGE : null)
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

    private String resolveDeviceName(Device device) {
        if (device.getName() == null || device.getName().isBlank()) {
            return device.getDeviceId();
        }
        return device.getName();
    }

    private boolean isDeviceOffline(Device device) {
        return device.getLastHeartbeat() == null
                || device.getLastHeartbeat().isBefore(LocalDateTime.now().minusSeconds(OFFLINE_TIMEOUT_SECONDS));
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}

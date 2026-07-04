package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.common.BusinessException;
import com.chinasoft.smokesensor.dto.DeviceControlRequest;
import com.chinasoft.smokesensor.dto.DeviceControlResponse;
import com.chinasoft.smokesensor.dto.DeviceCreateRequest;
import com.chinasoft.smokesensor.dto.DeviceDeleteResponse;
import com.chinasoft.smokesensor.dto.DeviceInfoResponse;
import com.chinasoft.smokesensor.dto.DeviceManageResponse;
import com.chinasoft.smokesensor.dto.DeviceStatusResponse;
import com.chinasoft.smokesensor.dto.DeviceUpdateRequest;
import com.chinasoft.smokesensor.entity.Device;
import com.chinasoft.smokesensor.entity.DeviceControl;
import com.chinasoft.smokesensor.repository.DeviceControlRepository;
import com.chinasoft.smokesensor.repository.DeviceRepository;
import com.chinasoft.smokesensor.service.DeviceService;
import com.chinasoft.smokesensor.service.SettingsService;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private static final String OFFLINE_MESSAGE = "设备未连接";

    private static final Set<String> SUPPORTED_DEVICE_TYPES = Set.of("switch", "buzzer", "alarm_light");
    private static final Set<String> SUPPORTED_STATUSES = Set.of("on", "off");
    private static final String CONTROL_SUCCESS_MESSAGE = "设备控制指令已下发";
    private static final String CONTROL_OPERATOR = "backend";

    private static final String DEVICE_DELETE_MESSAGE = "Device disabled";

    private final DeviceRepository deviceRepository;
    private final DeviceControlRepository deviceControlRepository;
    // 设备是否离线通过 SettingsService 读取 heartbeat_timeout 判断。
    private final SettingsService settingsService;

    @Override
    @Transactional(readOnly = true)
    public DeviceStatusResponse getDeviceStatus(String deviceId) {
        Device device = findDevice(deviceId);
        boolean offline = isDeviceOffline(device);
        return DeviceStatusResponse.builder()
                .deviceId(device.getDeviceId())
                .deviceName(resolveDeviceName(device))
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
    @Transactional
    public DeviceControlResponse controlDevice(DeviceControlRequest request) {
        String deviceId = normalizeRequired(request.getDeviceId(), "deviceId不能为空");
        String deviceType = normalizeRequired(request.getTarget(), "target不能为空").toLowerCase(Locale.ROOT);
        String status = normalizeRequired(request.getAction(), "action不能为空").toLowerCase(Locale.ROOT);

        if (!SUPPORTED_DEVICE_TYPES.contains(deviceType)) {
            throw new IllegalArgumentException("target只能是 switch、buzzer、alarm_light");
        }
        if (!SUPPORTED_STATUSES.contains(status)) {
            throw new IllegalArgumentException("action只能是 on 或 off");
        }
        if (!deviceRepository.existsByDeviceId(deviceId)) {
            throw BusinessException.notFound("Device not found: " + deviceId);
        }

        LocalDateTime operatedAt = LocalDateTime.now();
        DeviceControl control = deviceControlRepository.findByDeviceIdAndControlType(deviceId, deviceType)
                .orElseGet(() -> DeviceControl.builder()
                        .deviceId(deviceId)
                        .controlType(deviceType)
                        .controlName(resolveControlName(deviceType))
                        .autoLinkage(true)
                        .build());
        control.setStatus(status);
        control.setLastOperatedAt(operatedAt);
        control.setLastOperatedBy(CONTROL_OPERATOR);
        if (control.getControlName() == null || control.getControlName().isBlank()) {
            control.setControlName(resolveControlName(deviceType));
        }
        if (control.getAutoLinkage() == null) {
            control.setAutoLinkage(true);
        }
        deviceControlRepository.save(control);

        return DeviceControlResponse.builder()
                .success(true)
                .message(CONTROL_SUCCESS_MESSAGE)
                .deviceId(deviceId)
                .target(deviceType)
                .action(status)
                .operatedAt(operatedAt)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    // 实现设备列表查询 支持关键字keyword模糊搜索和启用状态enabled筛选
    public List<DeviceManageResponse> listDevices(String keyword, Boolean enabled) {
        Specification<Device> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String likeKeyword = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("deviceId"), likeKeyword),
                        cb.like(root.get("name"), likeKeyword),
                        cb.like(root.get("location"), likeKeyword)));
            }
            if (enabled != null) {
                predicates.add(cb.equal(root.get("enabled"), enabled));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return deviceRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "updatedAt"))
                .stream()
                .map(this::toDeviceManageResponse)
                .toList();
    }

    @Override
    @Transactional
    // 实现新增设备 新增时校验 deviceId 不能为空。如果设备编号已经存在，返回错误，不覆盖原设备
    public DeviceManageResponse createDevice(DeviceCreateRequest request) {
        String deviceId = normalizeRequired(request.getDeviceId(), "deviceId can not be blank");
        if (deviceRepository.existsByDeviceId(deviceId)) {
            throw new IllegalArgumentException("Device already exists: " + deviceId);
        }

        Device device = Device.builder()
                .deviceId(deviceId)
                .name(resolveText(request.getName(), deviceId))
                .location(trimToNull(request.getLocation()))
                .online(false)
                .currentRiskLevel("unknown")
                .currentAlarmStatus("offline")
                .enabled(true)
                .remark(trimToNull(request.getRemark()))
                .build();
        return toDeviceManageResponse(deviceRepository.save(device));
    }

    @Override
    @Transactional
    // 实现设备信息更新
    public DeviceManageResponse updateDevice(String deviceId, DeviceUpdateRequest request) {
        Device device = findDevice(deviceId);
        if (request.getName() != null) {
            device.setName(resolveText(request.getName(), device.getDeviceId()));
        }
        if (request.getLocation() != null) {
            device.setLocation(trimToNull(request.getLocation()));
        }
        if (request.getRemark() != null) {
            device.setRemark(trimToNull(request.getRemark()));
        }
        if (request.getEnabled() != null) {
            device.setEnabled(request.getEnabled());
        }
        return toDeviceManageResponse(deviceRepository.save(device));
    }

    @Override
    @Transactional
    // 解绑设备
    public DeviceDeleteResponse deleteDevice(String deviceId) {
        Device device = findDevice(deviceId);
        LocalDateTime deletedAt = LocalDateTime.now();

        // 不删除数据库记录。只把 enabled 设置为 false。历史数据 smoke_data 和告警记录 alarm_record 仍然能追溯到原设备
        device.setEnabled(false);
        deviceRepository.save(device);

        return DeviceDeleteResponse.builder()
                .deviceId(device.getDeviceId())
                .enabled(device.getEnabled())
                .deletedAt(deletedAt)
                .message(DEVICE_DELETE_MESSAGE)
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
                // 注意：设备状态接口离线判断统一使用 system_setting.heartbeat_timeout。
                || device.getLastHeartbeat().isBefore(LocalDateTime.now()
                .minusSeconds(settingsService.getThresholdSettings().getHeartbeatTimeout()));
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private DeviceManageResponse toDeviceManageResponse(Device device) {
        return DeviceManageResponse.builder()
                .deviceId(device.getDeviceId())
                .name(resolveDeviceName(device))
                .location(device.getLocation())
                .online(device.getOnline())
                .lastHeartbeat(device.getLastHeartbeat())
                .currentSmokeValue(device.getCurrentSmokeValue())
                .currentRiskLevel(device.getCurrentRiskLevel())
                .currentAlarmStatus(device.getCurrentAlarmStatus())
                .enabled(device.getEnabled())
                .remark(device.getRemark())
                .createdAt(device.getCreatedAt())
                .updatedAt(device.getUpdatedAt())
                .build();
    }

    private String resolveText(String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed == null ? fallback : trimmed;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String resolveControlName(String deviceType) {
        return switch (deviceType) {
            case "switch" -> "开关";
            case "buzzer" -> "蜂鸣器";
            case "alarm_light" -> "报警灯";
            default -> deviceType;
        };
    }
}

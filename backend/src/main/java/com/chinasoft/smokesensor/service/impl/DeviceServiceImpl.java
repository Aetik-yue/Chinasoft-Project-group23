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
import com.chinasoft.smokesensor.service.DeviceOnlineStatusService;
import com.chinasoft.smokesensor.service.DeviceOnlineStatusService.DeviceOnlineStatus;
import com.chinasoft.smokesensor.service.DeviceService;
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

/**
 * 设备状态、设备信息、设备控制和设备管理业务实现。
 *
 * <p>该类主要操作 smoke_device 和 device_control：
 * smoke_device 用于设备基础信息和当前状态，device_control 用于保存前端下发的控制指令。
 */
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private static final String OFFLINE_MESSAGE = "设备未连接";
    private static final Set<String> SUPPORTED_DEVICE_TYPES = Set.of("switch", "buzzer", "alarm_light");
    private static final Set<String> SUPPORTED_STATUSES = Set.of("on", "off");
    private static final String CONTROL_SUCCESS_MESSAGE = "设备控制指令已下发";
    private static final String CONTROL_OPERATOR = "backend";
    private static final String DEVICE_DELETE_MESSAGE = "设备已解绑";

    private final DeviceRepository deviceRepository;
    private final DeviceControlRepository deviceControlRepository;
    private final DeviceOnlineStatusService deviceOnlineStatusService;

    /**
     * 查询设备当前状态。
     *
     * <p>处理流程：
     * 1. 根据 deviceId 查询设备，deviceId 为空时使用最近更新设备；
     * 2. 根据 smoke_data 最新真实数据的 created_at 判断是否离线；
     * 3. 离线时返回 status=offline，在线时根据告警状态返回 alarm/online。
     */
    @Override
    @Transactional(readOnly = true)
    public DeviceStatusResponse getDeviceStatus(String deviceId) {
        Device device = findDevice(deviceId);
        DeviceOnlineStatus onlineStatus = deviceOnlineStatusService.getStatus(device.getDeviceId());
        boolean offline = !onlineStatus.online();
        return DeviceStatusResponse.builder()
                .deviceId(device.getDeviceId())
                .deviceName(resolveDeviceName(device))
                .connected(!offline)
                .lastHeartbeat(onlineStatus.lastDataAt())
                .status(offline ? "offline" : resolveStatus(device))
                .message(offline ? OFFLINE_MESSAGE : null)
                .progress(offline ? 0 : 100)
                .build();
    }

    /**
     * 查询设备基础信息。
     *
     * <p>用于前端设备信息区域展示设备名称、位置、最后心跳和连接状态。
     */
    @Override
    @Transactional(readOnly = true)
    public DeviceInfoResponse getDeviceInfo(String deviceId) {
        Device device = findDevice(deviceId);
        DeviceOnlineStatus onlineStatus = deviceOnlineStatusService.getStatus(device.getDeviceId());
        boolean offline = !onlineStatus.online();
        return DeviceInfoResponse.builder()
                .deviceId(device.getDeviceId())
                .deviceName(resolveDeviceName(device))
                .model(null)
                .firmwareVersion(null)
                .location(device.getLocation())
                .lastHeartbeat(onlineStatus.lastDataAt())
                .connected(!offline)
                .message(offline ? OFFLINE_MESSAGE : null)
                .build();
    }

    /**
     * 保存设备控制指令。
     *
     * <p>处理流程：
     * 1. 校验 deviceId、target、action；
     * 2. 确认 smoke_device 中存在该设备；
     * 3. 新增或更新 device_control 表中的控制状态。
     *
     * <p>注意：当前后端只保存控制状态，不直接连接硬件；后续可由 MQTT/硬件侧读取并执行。
     */
    @Override
    @Transactional
    public DeviceControlResponse controlDevice(DeviceControlRequest request) {
        String deviceId = normalizeRequired(request.getDeviceId(), "deviceId 不能为空");
        String deviceType = normalizeRequired(request.getTarget(), "target 不能为空").toLowerCase(Locale.ROOT);
        String status = normalizeRequired(request.getAction(), "action 不能为空").toLowerCase(Locale.ROOT);

        if (!SUPPORTED_DEVICE_TYPES.contains(deviceType)) {
            throw new IllegalArgumentException("target 只能是 switch、buzzer、alarm_light");
        }
        if (!SUPPORTED_STATUSES.contains(status)) {
            throw new IllegalArgumentException("action 只能是 on 或 off");
        }
        if (!deviceRepository.existsByDeviceId(deviceId)) {
            throw BusinessException.notFound("设备不存在: " + deviceId);
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

    /**
     * 查询设备管理列表。
     *
     * <p>支持按设备编号、名称、位置做模糊搜索，也支持按 enabled 状态筛选。
     */
    @Override
    @Transactional(readOnly = true)
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

    /**
     * 新增设备。
     *
     * <p>新增时只写 smoke_device，设备编号必须唯一。
     * 新设备默认离线，等待 smoke_data 收到真实硬件数据后再由查询逻辑判定在线。
     */
    @Override
    @Transactional
    public DeviceManageResponse createDevice(DeviceCreateRequest request) {
        String deviceId = normalizeRequired(request.getDeviceId(), "deviceId 不能为空");
        if (deviceRepository.existsByDeviceId(deviceId)) {
            throw new IllegalArgumentException("设备已存在: " + deviceId);
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

    /**
     * 编辑设备基础信息。
     *
     * <p>只允许修改名称、位置、备注和启用状态；不修改设备业务编号 deviceId，避免影响历史数据关联。
     */
    @Override
    @Transactional
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

    /**
     * 解绑设备。
     *
     * <p>当前采用软删除：只将 enabled 设置为 false，不物理删除 smoke_device，
     * 避免影响 smoke_data 和 alarm_record 的历史追溯。
     */
    @Override
    @Transactional
    public DeviceDeleteResponse deleteDevice(String deviceId) {
        Device device = findDevice(deviceId);
        LocalDateTime deletedAt = LocalDateTime.now();

        device.setEnabled(false);
        deviceRepository.save(device);

        return DeviceDeleteResponse.builder()
                .deviceId(device.getDeviceId())
                .enabled(device.getEnabled())
                .deletedAt(deletedAt)
                .message(DEVICE_DELETE_MESSAGE)
                .build();
    }

    /**
     * 按设备编号查询设备；设备编号为空时返回最近更新的设备。
     */
    private Device findDevice(String deviceId) {
        if (deviceId != null && !deviceId.isBlank()) {
            return deviceRepository.findByDeviceId(deviceId)
                    .orElseThrow(() -> BusinessException.notFound("设备不存在: " + deviceId));
        }
        return deviceRepository.findTopByOrderByUpdatedAtDesc()
                .orElseThrow(() -> BusinessException.notFound("未找到设备"));
    }

    /**
     * 在线状态已由 smoke_data 确认，此处只区分告警和普通在线状态。
     */
    private String resolveStatus(Device device) {
        if ("alarm".equalsIgnoreCase(device.getCurrentAlarmStatus())) {
            return "alarm";
        }
        return "online";
    }

    /**
     * 解析设备展示名称；名称为空时使用设备编号兜底。
     */
    private String resolveDeviceName(Device device) {
        if (device.getName() == null || device.getName().isBlank()) {
            return device.getDeviceId();
        }
        return device.getName();
    }

    /**
     * 校验必填字符串并去除首尾空格。
     */
    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    /**
     * 将设备实体转换为设备管理列表/详情使用的响应对象。
     */
    private DeviceManageResponse toDeviceManageResponse(Device device) {
        DeviceOnlineStatus onlineStatus = deviceOnlineStatusService.getStatus(device.getDeviceId());
        return DeviceManageResponse.builder()
                .deviceId(device.getDeviceId())
                .name(resolveDeviceName(device))
                .location(device.getLocation())
                .online(onlineStatus.online())
                .lastHeartbeat(onlineStatus.lastDataAt())
                .currentSmokeValue(device.getCurrentSmokeValue())
                .currentRiskLevel(device.getCurrentRiskLevel())
                .currentAlarmStatus(device.getCurrentAlarmStatus())
                .enabled(device.getEnabled())
                .remark(device.getRemark())
                .createdAt(device.getCreatedAt())
                .updatedAt(device.getUpdatedAt())
                .build();
    }

    /**
     * 解析可选文本；为空时使用兜底值。
     */
    private String resolveText(String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed == null ? fallback : trimmed;
    }

    /**
     * 去除字符串首尾空格；空字符串统一转为 null。
     */
    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /**
     * 根据控制设备类型生成 device_control 表中的中文控制名称。
     */
    private String resolveControlName(String deviceType) {
        return switch (deviceType) {
            case "switch" -> "开关";
            case "buzzer" -> "蜂鸣器";
            case "alarm_light" -> "报警灯";
            default -> deviceType;
        };
    }
}

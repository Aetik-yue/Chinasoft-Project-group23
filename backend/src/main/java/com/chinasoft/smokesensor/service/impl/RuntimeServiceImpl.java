package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.dto.RuntimeLinkSnapshotResponse;
import com.chinasoft.smokesensor.entity.Device;
import com.chinasoft.smokesensor.repository.DeviceRepository;
import com.chinasoft.smokesensor.service.DeviceOnlineStatusService;
import com.chinasoft.smokesensor.service.DeviceOnlineStatusService.DeviceOnlineStatus;
import com.chinasoft.smokesensor.service.RuntimeService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 运行态快照业务实现。
 *
 * <p>用于前端打开页面时判断硬件是否在线，以及应该显示大屏还是未连接页面。
 */
@Service
@RequiredArgsConstructor
public class RuntimeServiceImpl implements RuntimeService {

    private static final String LINK_STATE_ONLINE = "online";
    private static final String LINK_STATE_OFFLINE = "offline";
    private static final String DISPLAY_MODE_DASHBOARD = "dashboard";
    private static final String DISPLAY_MODE_UNCONNECTED_PAGE = "unconnected_page";
    private static final String OFFLINE_REASON_NO_DEVICE = "未找到设备";
    private static final String OFFLINE_REASON_DEVICE_UNCONNECTED = "设备未连接";

    private final DeviceRepository deviceRepository;
    private final DeviceOnlineStatusService deviceOnlineStatusService;

    /**
     * 查询设备连接快照。
     *
     * <p>处理流程：
     * 1. deviceId 为空时选择最近更新设备；
     * 2. 没有设备时返回离线和未连接页面模式；
     * 3. 有设备时根据 smoke_data 最新真实数据的 created_at 判断硬件在线状态。
     */
    @Override
    @Transactional(readOnly = true)
    public RuntimeLinkSnapshotResponse getLinkSnapshot(String deviceId) {
        Optional<Device> optionalDevice = findSnapshotDevice(deviceId);
        if (optionalDevice.isEmpty()) {
            return RuntimeLinkSnapshotResponse.builder()
                    .linkState(LINK_STATE_OFFLINE)
                    .hardwareOnline(false)
                    .mqttOnline(false)
                    .lastSeenAt(null)
                    .offlineReason(OFFLINE_REASON_NO_DEVICE)
                    .displayMode(DISPLAY_MODE_UNCONNECTED_PAGE)
                    .build();
        }

        Device device = optionalDevice.get();
        DeviceOnlineStatus onlineStatus = deviceOnlineStatusService.getStatus(device.getDeviceId());
        boolean hardwareOnline = onlineStatus.online();
        return RuntimeLinkSnapshotResponse.builder()
                .linkState(hardwareOnline ? LINK_STATE_ONLINE : LINK_STATE_OFFLINE)
                .hardwareOnline(hardwareOnline)
                // mqttOnline 仅用于前端判断是否显示 mqtt 连接状态，暂时使用 hardwareOnline 代替。
                // 后续可更改为真正的 mqtt 在线状态。
                .mqttOnline(hardwareOnline)
                .lastSeenAt(onlineStatus.lastDataAt())
                .offlineReason(hardwareOnline ? null : OFFLINE_REASON_DEVICE_UNCONNECTED)
                .displayMode(hardwareOnline ? DISPLAY_MODE_DASHBOARD : DISPLAY_MODE_UNCONNECTED_PAGE)
                .build();
    }

    /**
     * 查找运行态快照对应设备；未指定 deviceId 时使用最近更新设备。
     */
    private Optional<Device> findSnapshotDevice(String deviceId) {
        if (deviceId != null && !deviceId.isBlank()) {
            return deviceRepository.findByDeviceId(deviceId);
        }
        return deviceRepository.findTopByOrderByUpdatedAtDesc();
    }

}

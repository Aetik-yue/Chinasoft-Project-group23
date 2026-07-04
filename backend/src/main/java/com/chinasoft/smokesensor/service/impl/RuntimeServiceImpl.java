package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.dto.RuntimeLinkSnapshotResponse;
import com.chinasoft.smokesensor.entity.Device;
import com.chinasoft.smokesensor.repository.DeviceRepository;
import com.chinasoft.smokesensor.service.SettingsService;
import com.chinasoft.smokesensor.service.RuntimeService;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    // 运行态快照的在线状态判断也使用 heartbeat_timeout。
    private final SettingsService settingsService;

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
        boolean hardwareOnline = !isDeviceOffline(device);
        return RuntimeLinkSnapshotResponse.builder()
                .linkState(hardwareOnline ? LINK_STATE_ONLINE : LINK_STATE_OFFLINE)
                .hardwareOnline(hardwareOnline)
                .mqttOnline(hardwareOnline)
                .lastSeenAt(device.getLastHeartbeat())
                .offlineReason(hardwareOnline ? null : OFFLINE_REASON_DEVICE_UNCONNECTED)
                .displayMode(hardwareOnline ? DISPLAY_MODE_DASHBOARD : DISPLAY_MODE_UNCONNECTED_PAGE)
                .build();
    }

    private Optional<Device> findSnapshotDevice(String deviceId) {
        if (deviceId != null && !deviceId.isBlank()) {
            return deviceRepository.findByDeviceId(deviceId);
        }
        return deviceRepository.findTopByOrderByUpdatedAtDesc();
    }

    private boolean isDeviceOffline(Device device) {
        return device.getLastHeartbeat() == null
                // 注意：运行态快照离线判断统一使用 system_setting.heartbeat_timeout。
                || device.getLastHeartbeat().isBefore(LocalDateTime.now()
                .minusSeconds(settingsService.getThresholdSettings().getHeartbeatTimeout()));
    }
}

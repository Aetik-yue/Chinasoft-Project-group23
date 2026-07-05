package com.chinasoft.smokesensor.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chinasoft.smokesensor.dto.DeviceInfoResponse;
import com.chinasoft.smokesensor.dto.DeviceLatestDataResponse;
import com.chinasoft.smokesensor.dto.DeviceManageResponse;
import com.chinasoft.smokesensor.dto.DeviceStatusResponse;
import com.chinasoft.smokesensor.dto.RuntimeLinkSnapshotResponse;
import com.chinasoft.smokesensor.dto.SmokeLatestResponse;
import com.chinasoft.smokesensor.dto.SmokeRealtimeResponse;
import com.chinasoft.smokesensor.dto.SystemStatusResponse;
import com.chinasoft.smokesensor.entity.Device;
import com.chinasoft.smokesensor.entity.SensorData;
import com.chinasoft.smokesensor.repository.AlarmRecordRepository;
import com.chinasoft.smokesensor.repository.DeviceControlRepository;
import com.chinasoft.smokesensor.repository.DeviceRepository;
import com.chinasoft.smokesensor.repository.HumidityDataRepository;
import com.chinasoft.smokesensor.repository.SensorDataRepository;
import com.chinasoft.smokesensor.repository.TemperatureDataRepository;
import com.chinasoft.smokesensor.config.AlarmWebSocketSessionManager;
import com.chinasoft.smokesensor.service.DeviceOnlineStatusService;
import com.chinasoft.smokesensor.service.DeviceOnlineStatusService.DeviceOnlineStatus;
import com.chinasoft.smokesensor.service.SettingsService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

class OnlineStatusConsistencyTest {

    @Test
    void allQueryServicesShouldUseSmokeDataStatusAndPreserveAlarmState() {
        String deviceId = "SMK-001";
        LocalDateTime lastDataAt = LocalDateTime.of(2026, 7, 5, 12, 0, 0);
        Device device = Device.builder()
                .deviceId(deviceId)
                .name("烟雾传感器")
                .online(false)
                .lastHeartbeat(lastDataAt.minusHours(1))
                .currentSmokeValue(450)
                .currentRiskLevel("high")
                .currentAlarmStatus("alarm")
                .enabled(true)
                .build();
        SensorData latestData = SensorData.builder()
                .deviceId(deviceId)
                .smokeValue(450)
                .riskLevel("high")
                .source("sensor")
                .createdAt(lastDataAt)
                .build();
        DeviceOnlineStatus onlineStatus = new DeviceOnlineStatus(true, lastDataAt, latestData);

        DeviceRepository deviceRepository = mock(DeviceRepository.class);
        DeviceOnlineStatusService onlineStatusService = mock(DeviceOnlineStatusService.class);
        when(deviceRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(device));
        when(deviceRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<Device>>any(), any(Sort.class)))
                .thenReturn(List.of(device));
        when(deviceRepository.count()).thenReturn(1L);
        when(onlineStatusService.getStatus(deviceId)).thenReturn(onlineStatus);
        when(onlineStatusService.countOnlineDevices()).thenReturn(1L);

        DeviceServiceImpl deviceService = new DeviceServiceImpl(
                deviceRepository, mock(DeviceControlRepository.class), onlineStatusService);
        DeviceStatusResponse deviceStatus = deviceService.getDeviceStatus(deviceId);
        DeviceInfoResponse deviceInfo = deviceService.getDeviceInfo(deviceId);
        DeviceManageResponse managedDevice = deviceService.listDevices(null, null).get(0);

        SensorDataRepository sensorDataRepository = mock(SensorDataRepository.class);
        DeviceDataServiceImpl deviceDataService = new DeviceDataServiceImpl(
                deviceRepository,
                sensorDataRepository,
                mock(AlarmRecordRepository.class),
                mock(SettingsService.class),
                onlineStatusService,
                mock(AlarmWebSocketSessionManager.class));
        DeviceLatestDataResponse deviceLatestData = deviceDataService.getLatestData(deviceId);

        TemperatureDataRepository temperatureDataRepository = mock(TemperatureDataRepository.class);
        HumidityDataRepository humidityDataRepository = mock(HumidityDataRepository.class);
        when(temperatureDataRepository.findTopByDeviceIdOrderByRecordTimeDesc(deviceId))
                .thenReturn(Optional.empty());
        when(humidityDataRepository.findTopByDeviceIdOrderByRecordTimeDesc(deviceId))
                .thenReturn(Optional.empty());
        SmokeServiceImpl smokeService = new SmokeServiceImpl(
                deviceRepository,
                sensorDataRepository,
                mock(AlarmRecordRepository.class),
                temperatureDataRepository,
                humidityDataRepository,
                mock(SettingsService.class),
                onlineStatusService,
                mock(AlarmWebSocketSessionManager.class));
        SmokeLatestResponse latest = smokeService.getLatestSmoke(deviceId);
        SmokeRealtimeResponse realtime = smokeService.getRealtimeSmoke(deviceId);

        RuntimeServiceImpl runtimeService = new RuntimeServiceImpl(deviceRepository, onlineStatusService);
        RuntimeLinkSnapshotResponse runtime = runtimeService.getLinkSnapshot(deviceId);

        SystemServiceImpl systemService = new SystemServiceImpl(deviceRepository, onlineStatusService);
        SystemStatusResponse system = systemService.getSystemStatus();

        assertThat(deviceStatus.getConnected()).isTrue();
        assertThat(deviceStatus.getStatus()).isEqualTo("alarm");
        assertThat(deviceStatus.getLastHeartbeat()).isEqualTo(lastDataAt);
        assertThat(deviceInfo.getConnected()).isTrue();
        assertThat(deviceInfo.getLastHeartbeat()).isEqualTo(lastDataAt);
        assertThat(managedDevice.getOnline()).isTrue();
        assertThat(managedDevice.getLastHeartbeat()).isEqualTo(lastDataAt);
        assertThat(deviceLatestData.getOnline()).isTrue();
        assertThat(deviceLatestData.getLastHeartbeat()).isEqualTo(lastDataAt);
        assertThat(latest.getConnected()).isTrue();
        assertThat(latest.getUpdateTime()).isEqualTo(lastDataAt);
        assertThat(realtime.getConnected()).isTrue();
        assertThat(realtime.getUpdateTime()).isEqualTo(lastDataAt);
        assertThat(runtime.getHardwareOnline()).isTrue();
        assertThat(runtime.getLastSeenAt()).isEqualTo(lastDataAt);
        assertThat(system.getOnlineDeviceCount()).isEqualTo(1L);
    }
}

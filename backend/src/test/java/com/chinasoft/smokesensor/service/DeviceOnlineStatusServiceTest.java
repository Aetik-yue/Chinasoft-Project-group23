package com.chinasoft.smokesensor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chinasoft.smokesensor.entity.SensorData;
import com.chinasoft.smokesensor.repository.SensorDataRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeviceOnlineStatusServiceTest {

    private static final String DEVICE_ID = "SMK-001";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 5, 12, 0, 0);

    @Mock
    private SensorDataRepository sensorDataRepository;

    private DeviceOnlineStatusService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new DeviceOnlineStatusService(sensorDataRepository, fixedClock);
    }

    @Test
    void shouldBeOnlineWhenLatestRealDataIsWithinTenSeconds() {
        when(sensorDataRepository.findLatestRealDataByDeviceId(DEVICE_ID))
                .thenReturn(Optional.of(sensorDataAt(NOW.minusSeconds(9))));

        DeviceOnlineStatusService.DeviceOnlineStatus status = service.getStatus(DEVICE_ID);

        assertThat(status.online()).isTrue();
        assertThat(status.lastDataAt()).isEqualTo(NOW.minusSeconds(9));
    }

    @Test
    void shouldIncludeExactTenSecondBoundary() {
        when(sensorDataRepository.findLatestRealDataByDeviceId(DEVICE_ID))
                .thenReturn(Optional.of(sensorDataAt(NOW.minusSeconds(10))));

        assertThat(service.getStatus(DEVICE_ID).online()).isTrue();
    }

    @Test
    void shouldBeOfflineWhenLatestRealDataIsOlderThanTenSeconds() {
        when(sensorDataRepository.findLatestRealDataByDeviceId(DEVICE_ID))
                .thenReturn(Optional.of(sensorDataAt(NOW.minusSeconds(11))));

        assertThat(service.getStatus(DEVICE_ID).online()).isFalse();
    }

    @Test
    void shouldBeOfflineWhenNoRealDataExists() {
        when(sensorDataRepository.findLatestRealDataByDeviceId(DEVICE_ID)).thenReturn(Optional.empty());

        DeviceOnlineStatusService.DeviceOnlineStatus status = service.getStatus(DEVICE_ID);

        assertThat(status.online()).isFalse();
        assertThat(status.lastDataAt()).isNull();
        assertThat(status.latestData()).isNull();
    }

    @Test
    void shouldBeOfflineWhenCreatedAtIsMissing() {
        when(sensorDataRepository.findLatestRealDataByDeviceId(DEVICE_ID))
                .thenReturn(Optional.of(sensorDataAt(null)));

        assertThat(service.getStatus(DEVICE_ID).online()).isFalse();
    }

    @Test
    void shouldUseCreatedAtInsteadOfRecordTime() {
        SensorData data = sensorDataAt(NOW.minusSeconds(11));
        data.setRecordTime(NOW);
        when(sensorDataRepository.findLatestRealDataByDeviceId(DEVICE_ID)).thenReturn(Optional.of(data));

        assertThat(service.getStatus(DEVICE_ID).online()).isFalse();
    }

    @Test
    void shouldCountOnlineDevicesFromTheSameTenSecondThreshold() {
        when(sensorDataRepository.countOnlineDevices(NOW.minusSeconds(10))).thenReturn(2L);

        assertThat(service.countOnlineDevices()).isEqualTo(2L);
        verify(sensorDataRepository).countOnlineDevices(NOW.minusSeconds(10));
    }

    private SensorData sensorDataAt(LocalDateTime createdAt) {
        return SensorData.builder()
                .deviceId(DEVICE_ID)
                .smokeValue(100)
                .source("sensor")
                .recordTime(NOW.minusMinutes(1))
                .createdAt(createdAt)
                .build();
    }
}

package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.dto.DeviceLatestDataResponse;
import com.chinasoft.smokesensor.dto.SensorDataResponse;
import com.chinasoft.smokesensor.dto.SmokeDataUploadRequest;
import com.chinasoft.smokesensor.entity.AlarmRecord;
import com.chinasoft.smokesensor.entity.Device;
import com.chinasoft.smokesensor.entity.SensorData;
import com.chinasoft.smokesensor.repository.AlarmRecordRepository;
import com.chinasoft.smokesensor.repository.DeviceRepository;
import com.chinasoft.smokesensor.repository.SensorDataRepository;
import com.chinasoft.smokesensor.service.DeviceDataService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceDataServiceImpl implements DeviceDataService {

    private static final double SMOKE_THRESHOLD = 0.10;
    private static final String RISK_LEVEL_NORMAL = "normal";
    private static final String RISK_LEVEL_HIGH = "high";
    private static final String ALARM_STATUS_NORMAL = "normal";
    private static final String ALARM_STATUS_ALARM = "alarm";
    private static final String ALARM_TYPE_SMOKE = "smoke";
    private static final String ALARM_STATUS_UNHANDLED = "unhandled";
    private static final String DEFAULT_SOURCE = "http";

    private final DeviceRepository deviceRepository;
    private final SensorDataRepository sensorDataRepository;
    private final AlarmRecordRepository alarmRecordRepository;

    @Override
    @Transactional
    public DeviceLatestDataResponse uploadSmokeData(SmokeDataUploadRequest request) {
        LocalDateTime uploadTime = LocalDateTime.now();
        boolean alarm = request.getSmokeValue() > SMOKE_THRESHOLD;
        String riskLevel = alarm ? RISK_LEVEL_HIGH : RISK_LEVEL_NORMAL;
        String alarmStatus = alarm ? ALARM_STATUS_ALARM : ALARM_STATUS_NORMAL;
        String source = request.getSource() == null || request.getSource().isBlank()
                ? DEFAULT_SOURCE
                : request.getSource();

        SensorData sensorData = SensorData.builder()
                .deviceId(request.getDeviceId())
                .smokeValue(request.getSmokeValue())
                .riskLevel(riskLevel)
                .recordTime(uploadTime)
                .source(source)
                .build();
        sensorDataRepository.save(sensorData);

        Device device = deviceRepository.findByDeviceId(request.getDeviceId())
                .orElseGet(() -> Device.builder()
                        .deviceId(request.getDeviceId())
                        .name(request.getDeviceId())
                        .build());
        device.setOnline(true);
        device.setLastHeartbeat(uploadTime);
        device.setCurrentSmokeValue(request.getSmokeValue());
        device.setCurrentRiskLevel(riskLevel);
        device.setCurrentAlarmStatus(alarmStatus);
        if (device.getEnabled() == null) {
            device.setEnabled(true);
        }
        deviceRepository.save(device);

        if (alarm) {
            AlarmRecord alarmRecord = AlarmRecord.builder()
                    .alarmId(UUID.randomUUID().toString())
                    .deviceId(request.getDeviceId())
                    .alarmType(ALARM_TYPE_SMOKE)
                    .smokeValue(request.getSmokeValue())
                    .riskLevel(riskLevel)
                    .status(ALARM_STATUS_UNHANDLED)
                    .triggeredAt(uploadTime)
                    .isSimulated(true)
                    .createdAt(uploadTime)
                    .build();
            alarmRecordRepository.save(alarmRecord);
        }

        return toDeviceLatestDataResponse(device);
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceLatestDataResponse getLatestData(String deviceId) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));
        sensorDataRepository.findTopByDeviceIdOrderByRecordTimeDesc(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Sensor data not found for device: " + deviceId));

        return toDeviceLatestDataResponse(device);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SensorDataResponse> getHistoryData(String deviceId, int limit) {
        if (!deviceRepository.existsByDeviceId(deviceId)) {
            throw new IllegalArgumentException("Device not found: " + deviceId);
        }
        int pageSize = Math.max(limit, 1);

        return sensorDataRepository.findByDeviceIdOrderByRecordTimeDesc(deviceId, PageRequest.of(0, pageSize))
                .stream()
                .map(this::toSensorDataResponse)
                .toList();
    }

    private DeviceLatestDataResponse toDeviceLatestDataResponse(Device device) {
        return DeviceLatestDataResponse.builder()
                .deviceId(device.getDeviceId())
                .online(device.getOnline())
                .lastHeartbeat(device.getLastHeartbeat())
                .currentSmokeValue(device.getCurrentSmokeValue())
                .currentRiskLevel(device.getCurrentRiskLevel())
                .currentAlarmStatus(device.getCurrentAlarmStatus())
                .enabled(device.getEnabled())
                .build();
    }

    private SensorDataResponse toSensorDataResponse(SensorData sensorData) {
        return SensorDataResponse.builder()
                .deviceId(sensorData.getDeviceId())
                .smokeValue(sensorData.getSmokeValue())
                .riskLevel(sensorData.getRiskLevel())
                .recordTime(sensorData.getRecordTime())
                .source(sensorData.getSource())
                .createdAt(sensorData.getCreatedAt())
                .build();
    }
}

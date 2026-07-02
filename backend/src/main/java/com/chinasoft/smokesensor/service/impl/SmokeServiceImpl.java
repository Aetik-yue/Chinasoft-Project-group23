package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.common.BusinessException;
import com.chinasoft.smokesensor.dto.SmokeHistoryPointResponse;
import com.chinasoft.smokesensor.dto.SmokeLatestResponse;
import com.chinasoft.smokesensor.dto.SmokeRealtimeResponse;
import com.chinasoft.smokesensor.dto.SmokeRestoreRequest;
import com.chinasoft.smokesensor.dto.SmokeRestoreResponse;
import com.chinasoft.smokesensor.dto.SmokeSimulateRequest;
import com.chinasoft.smokesensor.dto.SmokeSimulateResponse;
import com.chinasoft.smokesensor.entity.AlarmRecord;
import com.chinasoft.smokesensor.entity.Device;
import com.chinasoft.smokesensor.entity.SensorData;
import com.chinasoft.smokesensor.repository.AlarmRecordRepository;
import com.chinasoft.smokesensor.repository.DeviceRepository;
import com.chinasoft.smokesensor.repository.SensorDataRepository;
import com.chinasoft.smokesensor.service.SmokeService;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SmokeServiceImpl implements SmokeService {

    private static final String UNIT = "ppm";
    private static final int WARNING_THRESHOLD = 200;
    private static final int ALARM_THRESHOLD = 150;
    private static final long OFFLINE_TIMEOUT_SECONDS = 60;
    private static final int HISTORY_LIMIT = 200;
    private static final int DEFAULT_SIMULATE_SMOKE_VALUE = 450;
    private static final int RESTORE_SMOKE_VALUE = 35;
    private static final String DEFAULT_DEVICE_ID = "device-001";
    private static final String DEFAULT_SIMULATE_SOURCE = "simulate";
    private static final String SOURCE_SENSOR = "sensor";
    private static final String SOURCE_SIMULATE = "simulate";
    private static final String SOURCE_ALL = "all";
    private static final String RISK_LEVEL_NORMAL = "normal";
    private static final String ALARM_STATUS_SAFE = "safe";
    private static final String DEVICE_OFFLINE_MESSAGE = "设备未连接";
    private static final String RESTORE_MESSAGE = "环境已恢复正常";

    private final DeviceRepository deviceRepository;
    private final SensorDataRepository sensorDataRepository;
    private final AlarmRecordRepository alarmRecordRepository;

    @Override
    @Transactional(readOnly = true)
    public SmokeLatestResponse getLatestSmoke(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return getLatestSmokeFromLatestRecord();
        }
        Device device = findDevice(deviceId);
        if (isDeviceOffline(device)) {
            return toOfflineLatestResponse(device.getDeviceId(), device.getLastHeartbeat());
        }
        return SmokeLatestResponse.builder()
                .deviceId(device.getDeviceId())
                .smokeValue(device.getCurrentSmokeValue())
                .online(true)
                .unit(UNIT)
                .updatedAt(resolveLatestTime(device))
                .riskLevel(device.getCurrentRiskLevel())
                .riskScore(toRiskScore(device.getCurrentRiskLevel()))
                .alarmStatus(device.getCurrentAlarmStatus())
                .alarmType("alarm".equalsIgnoreCase(device.getCurrentAlarmStatus()) ? "smoke_high" : null)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SmokeRealtimeResponse getRealtimeSmoke(String deviceId) {
        SmokeLatestResponse latest = getLatestSmoke(deviceId);
        return SmokeRealtimeResponse.builder()
                .deviceId(latest.getDeviceId())
                .connected(Boolean.TRUE.equals(latest.getOnline()))
                .smokeValue(latest.getSmokeValue())
                .temperature(null)
                .humidity(null)
                .riskLevel(latest.getRiskLevel())
                .alarmStatus(latest.getAlarmStatus())
                .themeType(resolveThemeType(latest))
                .updateTime(latest.getUpdatedAt())
                .message(latest.getMessage())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SmokeHistoryPointResponse> getHistory(
            String deviceId,
            String range,
            LocalDateTime start,
            LocalDateTime end,
            String source) {
        if (deviceId != null && !deviceId.isBlank() && !deviceRepository.existsByDeviceId(deviceId)) {
            throw BusinessException.notFound("Device not found: " + deviceId);
        }
        TimeRange timeRange = resolveTimeRange(range, start, end);
        String resolvedSource = resolveHistorySource(source);
        Specification<SensorData> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (deviceId != null && !deviceId.isBlank()) {
                predicates.add(cb.equal(root.get("deviceId"), deviceId));
            }
            if (SOURCE_SENSOR.equals(resolvedSource)) {
                predicates.add(cb.or(
                        cb.equal(root.get("source"), SOURCE_SENSOR),
                        cb.isNull(root.get("source"))));
            } else if (SOURCE_SIMULATE.equals(resolvedSource)) {
                predicates.add(cb.equal(root.get("source"), SOURCE_SIMULATE));
            }
            if (timeRange.start() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("recordTime"), timeRange.start()));
            }
            if (timeRange.end() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<LocalDateTime>get("recordTime"), timeRange.end()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return sensorDataRepository.findAll(
                        specification,
                        PageRequest.of(0, HISTORY_LIMIT, Sort.by(Sort.Direction.ASC, "recordTime")))
                .stream()
                .map(this::toHistoryPoint)
                .toList();
    }

    @Override
    @Transactional
    public SmokeSimulateResponse simulateSmoke(SmokeSimulateRequest request) {
        LocalDateTime simulateTime = LocalDateTime.now();
        String deviceId = resolveSimulateDeviceId(request);
        int smokeValue = resolveSimulateSmokeValue(request);
        String riskLevel = mapRiskLevel(smokeValue);
        String alarmStatus = smokeValue > ALARM_THRESHOLD ? "alarm" : "safe";
        String alarmType = "alarm".equals(alarmStatus) ? "smoke" : null;
        String source = request.getSource() == null || request.getSource().isBlank()
                ? DEFAULT_SIMULATE_SOURCE
                : request.getSource();

        SensorData sensorData = SensorData.builder()
                .deviceId(deviceId)
                .smokeValue(smokeValue)
                .riskLevel(riskLevel)
                .recordTime(simulateTime)
                .source(source)
                .build();
        sensorDataRepository.save(sensorData);

        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseGet(() -> Device.builder()
                        .deviceId(deviceId)
                        .name(deviceId)
                        .build());
        device.setOnline(true);
        device.setLastHeartbeat(simulateTime);
        device.setCurrentSmokeValue(smokeValue);
        device.setCurrentRiskLevel(riskLevel);
        device.setCurrentAlarmStatus(alarmStatus);
        if (device.getEnabled() == null) {
            device.setEnabled(true);
        }
        deviceRepository.save(device);

        String createdAlarmId = null;
        if ("alarm".equals(alarmStatus)) {
            createdAlarmId = UUID.randomUUID().toString();
            AlarmRecord alarmRecord = AlarmRecord.builder()
                    .alarmId(createdAlarmId)
                    .deviceId(deviceId)
                    .alarmType(alarmType)
                    .smokeValue(smokeValue)
                    .riskLevel(riskLevel)
                    .status("unhandled")
                    .triggeredAt(simulateTime)
                    .isSimulated(true)
                    .createTime(simulateTime)
                    .updatedAt(simulateTime)
                    .build();
            alarmRecordRepository.save(alarmRecord);
        }

        return SmokeSimulateResponse.builder()
                .deviceId(deviceId)
                .smokeValue(smokeValue)
                .unit(UNIT)
                .updatedAt(simulateTime)
                .riskLevel(riskLevel)
                .riskScore(toRiskScore(riskLevel))
                .alarmStatus(alarmStatus)
                .alarmType(alarmType)
                .createdAlarmId(createdAlarmId)
                .build();
    }

    @Override
    @Transactional
    public SmokeRestoreResponse restoreSmoke(SmokeRestoreRequest request) {
        LocalDateTime restoreTime = LocalDateTime.now();
        Device device = resolveRestoreDevice(request);
        String deviceId = device.getDeviceId();

        SensorData sensorData = SensorData.builder()
                .deviceId(deviceId)
                .smokeValue(RESTORE_SMOKE_VALUE)
                .riskLevel(RISK_LEVEL_NORMAL)
                .recordTime(restoreTime)
                .source(DEFAULT_SIMULATE_SOURCE)
                .build();
        sensorDataRepository.save(sensorData);

        device.setCurrentSmokeValue(RESTORE_SMOKE_VALUE);
        device.setCurrentRiskLevel(RISK_LEVEL_NORMAL);
        device.setCurrentAlarmStatus(ALARM_STATUS_SAFE);
        device.setOnline(true);
        device.setLastHeartbeat(restoreTime);
        deviceRepository.save(device);

        List<AlarmRecord> pendingAlarms = alarmRecordRepository.findByDeviceIdAndStatusIn(
                deviceId, List.of("pending", "processing"));
        for (AlarmRecord alarmRecord : pendingAlarms) {
            alarmRecord.setStatus("resolved");
            alarmRecord.setResolvedAt(restoreTime);
            alarmRecord.setUpdatedAt(restoreTime);
            if (alarmRecord.getRemark() == null || alarmRecord.getRemark().isBlank()) {
                alarmRecord.setRemark(RESTORE_MESSAGE);
            }
        }
        alarmRecordRepository.saveAll(pendingAlarms);

        return SmokeRestoreResponse.builder()
                .deviceId(deviceId)
                .smokeValue(RESTORE_SMOKE_VALUE)
                .unit(UNIT)
                .updatedAt(restoreTime)
                .riskLevel(RISK_LEVEL_NORMAL)
                .riskScore(toRiskScore(RISK_LEVEL_NORMAL))
                .alarmStatus(ALARM_STATUS_SAFE)
                .alarmType(null)
                .resolvedAlarmCount(pendingAlarms.size())
                .message(RESTORE_MESSAGE)
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

    private SmokeLatestResponse getLatestSmokeFromLatestRecord() {
        SensorData sensorData = sensorDataRepository.findTopByOrderByRecordTimeDesc()
                .orElseThrow(() -> BusinessException.notFound("No smoke data found"));
        Device device = deviceRepository.findByDeviceId(sensorData.getDeviceId()).orElse(null);
        if (device == null || isDeviceOffline(device)) {
            return toOfflineLatestResponse(sensorData.getDeviceId(), device == null ? null : device.getLastHeartbeat());
        }
        String alarmStatus = device == null ? null : device.getCurrentAlarmStatus();
        return SmokeLatestResponse.builder()
                .deviceId(sensorData.getDeviceId())
                .smokeValue(sensorData.getSmokeValue())
                .online(true)
                .unit(UNIT)
                .updatedAt(sensorData.getRecordTime())
                .riskLevel(sensorData.getRiskLevel())
                .riskScore(toRiskScore(sensorData.getRiskLevel()))
                .alarmStatus(alarmStatus)
                .alarmType("alarm".equalsIgnoreCase(alarmStatus) ? "smoke_high" : null)
                .build();
    }

    private SmokeLatestResponse toOfflineLatestResponse(String deviceId, LocalDateTime lastHeartbeat) {
        return SmokeLatestResponse.builder()
                .deviceId(deviceId)
                .smokeValue(null)
                .online(false)
                .unit(UNIT)
                .updatedAt(lastHeartbeat)
                .riskLevel("unknown")
                .riskScore(0)
                .alarmStatus("offline")
                .alarmType(null)
                .message(DEVICE_OFFLINE_MESSAGE)
                .build();
    }

    private boolean isDeviceOffline(Device device) {
        return device.getLastHeartbeat() == null
                || device.getLastHeartbeat().isBefore(LocalDateTime.now().minusSeconds(OFFLINE_TIMEOUT_SECONDS));
    }

    private LocalDateTime resolveLatestTime(Device device) {
        return sensorDataRepository.findTopByDeviceIdOrderByRecordTimeDesc(device.getDeviceId())
                .map(SensorData::getRecordTime)
                .orElse(device.getLastHeartbeat());
    }

    private SmokeHistoryPointResponse toHistoryPoint(SensorData sensorData) {
        return SmokeHistoryPointResponse.builder()
                .time(sensorData.getRecordTime())
                .value(sensorData.getSmokeValue())
                .threshold(WARNING_THRESHOLD)
                .build();
    }

    private Integer toRiskScore(String riskLevel) {
        if ("high".equalsIgnoreCase(riskLevel)) {
            return 100;
        }
        if ("medium".equalsIgnoreCase(riskLevel)) {
            return 70;
        }
        if ("low".equalsIgnoreCase(riskLevel)) {
            return 40;
        }
        if ("normal".equalsIgnoreCase(riskLevel)) {
            return 10;
        }
        return 0;
    }

    private String resolveThemeType(SmokeLatestResponse latest) {
        if (Boolean.FALSE.equals(latest.getOnline()) || "offline".equalsIgnoreCase(latest.getAlarmStatus())) {
            return "offline";
        }
        if ("alarm".equalsIgnoreCase(latest.getAlarmStatus())) {
            return "danger";
        }
        return "normal";
    }

    private String resolveHistorySource(String source) {
        if (source == null || source.isBlank()) {
            return SOURCE_SENSOR;
        }
        String normalizedSource = source.trim().toLowerCase();
        if (SOURCE_SENSOR.equals(normalizedSource)
                || SOURCE_SIMULATE.equals(normalizedSource)
                || SOURCE_ALL.equals(normalizedSource)) {
            return normalizedSource;
        }
        throw new IllegalArgumentException("source只能是 sensor、simulate 或 all");
    }

    private String mapRiskLevel(int smokeValue) {
        if (smokeValue >= 400) {
            return "high";
        }
        if (smokeValue >= 200) {
            return "medium";
        }
        if (smokeValue >= 100) {
            return "low";
        }
        return "normal";
    }

    private String resolveSimulateDeviceId(SmokeSimulateRequest request) {
        if (request.getDeviceId() != null && !request.getDeviceId().isBlank()) {
            return request.getDeviceId();
        }
        return deviceRepository.findTopByOrderByUpdatedAtDesc()
                .map(Device::getDeviceId)
                .orElse(DEFAULT_DEVICE_ID);
    }

    private Device resolveRestoreDevice(SmokeRestoreRequest request) {
        if (request.getDeviceId() != null && !request.getDeviceId().isBlank()) {
            return deviceRepository.findByDeviceId(request.getDeviceId())
                    .orElseThrow(() -> BusinessException.notFound("Device not found: " + request.getDeviceId()));
        }
        return deviceRepository.findTopByOrderByUpdatedAtDesc()
                .orElseThrow(() -> BusinessException.notFound("No device found for restore"));
    }

    private int resolveSimulateSmokeValue(SmokeSimulateRequest request) {
        if (request.getSmokeValue() != null) {
            return request.getSmokeValue();
        }
        if ("smoke_high".equalsIgnoreCase(request.getScenario())) {
            return DEFAULT_SIMULATE_SMOKE_VALUE;
        }
        return DEFAULT_SIMULATE_SMOKE_VALUE;
    }

    private TimeRange resolveTimeRange(String range, LocalDateTime start, LocalDateTime end) {
        if (start != null || end != null) {
            return new TimeRange(start, end == null ? LocalDateTime.now() : end);
        }
        LocalDateTime rangeEnd = LocalDateTime.now();
        LocalDateTime rangeStart = switch (range == null ? "24h" : range.toLowerCase()) {
            case "realtime", "1h" -> rangeEnd.minusHours(1);
            case "6h" -> rangeEnd.minusHours(6);
            case "12h" -> rangeEnd.minusHours(12);
            case "7d" -> rangeEnd.minusDays(7);
            case "24h" -> rangeEnd.minusHours(24);
            default -> rangeEnd.minusHours(24);
        };
        return new TimeRange(rangeStart, rangeEnd);
    }

    private record TimeRange(LocalDateTime start, LocalDateTime end) {
    }
}

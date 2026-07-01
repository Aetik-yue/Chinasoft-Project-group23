package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.common.BusinessException;
import com.chinasoft.smokesensor.dto.SmokeHistoryPointResponse;
import com.chinasoft.smokesensor.dto.SmokeLatestResponse;
import com.chinasoft.smokesensor.entity.Device;
import com.chinasoft.smokesensor.entity.SensorData;
import com.chinasoft.smokesensor.repository.DeviceRepository;
import com.chinasoft.smokesensor.repository.SensorDataRepository;
import com.chinasoft.smokesensor.service.SmokeService;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    private static final int HISTORY_LIMIT = 200;

    private final DeviceRepository deviceRepository;
    private final SensorDataRepository sensorDataRepository;

    @Override
    @Transactional(readOnly = true)
    public SmokeLatestResponse getLatestSmoke(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return getLatestSmokeFromLatestRecord();
        }
        Device device = findDevice(deviceId);
        return SmokeLatestResponse.builder()
                .deviceId(device.getDeviceId())
                .smokeValue(device.getCurrentSmokeValue())
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
    public List<SmokeHistoryPointResponse> getHistory(
            String deviceId,
            String range,
            LocalDateTime start,
            LocalDateTime end) {
        if (deviceId != null && !deviceId.isBlank() && !deviceRepository.existsByDeviceId(deviceId)) {
            throw BusinessException.notFound("Device not found: " + deviceId);
        }
        TimeRange timeRange = resolveTimeRange(range, start, end);
        Specification<SensorData> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (deviceId != null && !deviceId.isBlank()) {
                predicates.add(cb.equal(root.get("deviceId"), deviceId));
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
        String alarmStatus = device == null ? null : device.getCurrentAlarmStatus();
        return SmokeLatestResponse.builder()
                .deviceId(sensorData.getDeviceId())
                .smokeValue(sensorData.getSmokeValue())
                .unit(UNIT)
                .updatedAt(sensorData.getRecordTime())
                .riskLevel(sensorData.getRiskLevel())
                .riskScore(toRiskScore(sensorData.getRiskLevel()))
                .alarmStatus(alarmStatus)
                .alarmType("alarm".equalsIgnoreCase(alarmStatus) ? "smoke_high" : null)
                .build();
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

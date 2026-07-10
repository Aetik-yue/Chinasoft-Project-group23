package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.common.BusinessException;
import com.chinasoft.smokesensor.dto.EnvironmentHistoryResponse;
import com.chinasoft.smokesensor.entity.HumidityData;
import com.chinasoft.smokesensor.entity.SensorData;
import com.chinasoft.smokesensor.entity.TemperatureData;
import com.chinasoft.smokesensor.repository.DeviceRepository;
import com.chinasoft.smokesensor.repository.HumidityDataRepository;
import com.chinasoft.smokesensor.repository.SensorDataRepository;
import com.chinasoft.smokesensor.repository.TemperatureDataRepository;
import com.chinasoft.smokesensor.service.EnvironmentHistoryService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 成长报告环境历史实现。
 *
 * 从 temperature_data / humidity_data / smoke_data 三张表读取真实采样，按 recordTime 对齐合并。
 * 同一分钟内多项采样取均值（保留 1 位温度/湿度精度），缺项留 null。
 */
@Service
@RequiredArgsConstructor
public class EnvironmentHistoryServiceImpl implements EnvironmentHistoryService {

    private static final int HISTORY_LIMIT = 500;

    private final DeviceRepository deviceRepository;
    private final TemperatureDataRepository temperatureDataRepository;
    private final HumidityDataRepository humidityDataRepository;
    private final SensorDataRepository sensorDataRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EnvironmentHistoryResponse> getHistory(String deviceId, String range) {
        if (deviceId != null && !deviceId.isBlank() && !deviceRepository.existsByDeviceId(deviceId)) {
            throw BusinessException.notFound("设备不存在: " + deviceId);
        }
        TimeRange timeRange = resolveTimeRange(range);
        // deviceId 为空时无法按设备过滤，返回空序列（前端会回落到 mock）。
        String id = (deviceId == null || deviceId.isBlank()) ? null : deviceId;

        List<TemperatureData> temperatures = id == null ? List.of()
                : temperatureDataRepository.findByDeviceIdOrderByRecordTimeDesc(id).stream()
                        .filter(d -> inRange(d.getRecordTime(), timeRange))
                        .limit(HISTORY_LIMIT)
                        .toList();

        List<HumidityData> humidities = id == null ? List.of()
                : humidityDataRepository.findByDeviceIdOrderByRecordTimeDesc(id).stream()
                        .filter(d -> inRange(d.getRecordTime(), timeRange))
                        .limit(HISTORY_LIMIT)
                        .toList();

        List<SensorData> dusts = id == null ? List.of()
                : sensorDataRepository.findByDeviceIdOrderByRecordTimeDesc(id).stream()
                        .filter(d -> (d.getSource() == null || "sensor".equalsIgnoreCase(d.getSource()))
                                && inRange(d.getRecordTime(), timeRange))
                        .limit(HISTORY_LIMIT)
                        .toList();

        // 按「分钟」聚合：同分钟内取均值，保持时间升序。
        TreeMap<LocalDateTime, Accum> byMinute = new TreeMap<>();
        for (TemperatureData d : temperatures) {
            bucket(byMinute, minuteKey(d.getRecordTime())).addTemperature(d.getTemperatureValue());
        }
        for (HumidityData d : humidities) {
            bucket(byMinute, minuteKey(d.getRecordTime())).addHumidity(d.getHumidityValue());
        }
        for (SensorData d : dusts) {
            bucket(byMinute, minuteKey(d.getRecordTime())).addDust(d.getSmokeValue());
        }

        List<EnvironmentHistoryResponse> result = new ArrayList<>(byMinute.size());
        for (Map.Entry<LocalDateTime, Accum> entry : byMinute.entrySet()) {
            result.add(entry.getValue().toResponse(entry.getKey()));
        }
        return result;
    }

    private boolean inRange(LocalDateTime time, TimeRange range) {
        if (time == null) return false;
        return !time.isBefore(range.start()) && !time.isAfter(range.end());
    }

    private LocalDateTime minuteKey(LocalDateTime time) {
        return time.withSecond(0).withNano(0);
    }

    private Accum bucket(TreeMap<LocalDateTime, Accum> map, LocalDateTime key) {
        return map.computeIfAbsent(key, k -> new Accum());
    }

    private TimeRange resolveTimeRange(String range) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = switch (range == null ? "24h" : range.toLowerCase()) {
            case "30d" -> end.minusDays(30);
            case "7d" -> end.minusDays(7);
            case "24h" -> end.minusHours(24);
            default -> end.minusHours(24);
        };
        return new TimeRange(start, end);
    }

    private record TimeRange(LocalDateTime start, LocalDateTime end) {}

    /** 累积器：用于同分钟多采样的均值聚合。 */
    private static final class Accum {
        private final List<Double> temperatures = new ArrayList<>();
        private final List<Double> humidities = new ArrayList<>();
        private final List<Integer> dusts = new ArrayList<>();

        void addTemperature(Float v) {
            if (v != null) temperatures.add(v.doubleValue());
        }

        void addHumidity(Float v) {
            if (v != null) humidities.add(v.doubleValue());
        }

        void addDust(Integer v) {
            if (v != null) dusts.add(v);
        }

        EnvironmentHistoryResponse toResponse(LocalDateTime time) {
            return EnvironmentHistoryResponse.builder()
                    .time(time)
                    .temperature(temperatures.isEmpty() ? null : avg(temperatures))
                    .humidity(humidities.isEmpty() ? null : avg(humidities))
                    .dust(dusts.isEmpty() ? null : (int) Math.round(avgInt(dusts)))
                    .build();
        }

        private double avg(List<Double> vals) {
            return vals.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        }

        private double avgInt(List<Integer> vals) {
            return vals.stream().mapToInt(Integer::intValue).average().orElse(0);
        }
    }

}

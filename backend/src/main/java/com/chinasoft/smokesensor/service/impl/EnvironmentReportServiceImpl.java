package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.dto.EnvironmentHistoryResponse;
import com.chinasoft.smokesensor.entity.EnvironmentReportHourly;
import com.chinasoft.smokesensor.entity.HumidityData;
import com.chinasoft.smokesensor.entity.SensorData;
import com.chinasoft.smokesensor.entity.TemperatureData;
import com.chinasoft.smokesensor.repository.DeviceRepository;
import com.chinasoft.smokesensor.repository.EnvironmentReportHourlyRepository;
import com.chinasoft.smokesensor.repository.HumidityDataRepository;
import com.chinasoft.smokesensor.repository.SensorDataRepository;
import com.chinasoft.smokesensor.repository.TemperatureDataRepository;
import com.chinasoft.smokesensor.service.EnvironmentReportService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 成长报告预聚合实现。
 *
 * <p>定时任务每小时聚合上一小时的温度 / 湿度 / 粉尘，按"分钟"粒度对齐后写入
 * environment_report_hourly。成长报告直接读这张表，不再现场聚合。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnvironmentReportServiceImpl implements EnvironmentReportService {

    private static final int AGGREGATE_LIMIT = 2000;

    private final DeviceRepository deviceRepository;
    private final EnvironmentReportHourlyRepository reportRepository;
    private final TemperatureDataRepository temperatureDataRepository;
    private final HumidityDataRepository humidityDataRepository;
    private final SensorDataRepository sensorDataRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EnvironmentHistoryResponse> getHourlyHistory(String deviceId, String range) {
        if (deviceId == null || deviceId.isBlank()) {
            return List.of();
        }
        TimeRange timeRange = resolveTimeRange(range);
        List<EnvironmentReportHourly> rows =
                reportRepository.findByDeviceIdAndHourTimeBetweenOrderByHourTimeAsc(
                        deviceId, timeRange.start(), timeRange.end());
        List<EnvironmentHistoryResponse> result = new ArrayList<>(rows.size());
        for (EnvironmentReportHourly row : rows) {
            result.add(EnvironmentHistoryResponse.builder()
                    .time(row.getHourTime())
                    .temperature(row.getAvgTemperature() != null ? row.getAvgTemperature().doubleValue() : null)
                    .humidity(row.getAvgHumidity() != null ? row.getAvgHumidity().doubleValue() : null)
                    .dust(row.getAvgDust() != null ? row.getAvgDust().intValue() : null)
                    .build());
        }
        return result;
    }

    @Override
    @Transactional
    public boolean aggregatePreviousHour(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return false;
        }
        // 聚合"上一个完整小时"：如当前 14:05，则聚合 13:00:00 ~ 13:59:59。
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thisHour = now.withMinute(0).withSecond(0).withNano(0);
        LocalDateTime hourStart = thisHour.minusHours(1);
        LocalDateTime hourEnd = thisHour.minusSeconds(1);

        List<TemperatureData> temperatures = temperatureDataRepository
                .findByDeviceIdOrderByRecordTimeDesc(deviceId).stream()
                .filter(d -> inRange(d.getRecordTime(), hourStart, hourEnd))
                .limit(AGGREGATE_LIMIT)
                .toList();
        List<HumidityData> humidities = humidityDataRepository
                .findByDeviceIdOrderByRecordTimeDesc(deviceId).stream()
                .filter(d -> inRange(d.getRecordTime(), hourStart, hourEnd))
                .limit(AGGREGATE_LIMIT)
                .toList();
        List<SensorData> dusts = sensorDataRepository
                .findByDeviceIdOrderByRecordTimeDesc(deviceId).stream()
                .filter(d -> (d.getSource() == null || "sensor".equalsIgnoreCase(d.getSource()))
                        && inRange(d.getRecordTime(), hourStart, hourEnd))
                .limit(AGGREGATE_LIMIT)
                .toList();

        int sampleCount = temperatures.size() + humidities.size() + dusts.size();
        if (sampleCount == 0) {
            log.info("[report-hourly] device={} hour={} 无采样，跳过", deviceId, hourStart);
            return false;
        }

        Map<LocalDateTime, Accum> byMinute = new TreeMap<>();
        for (TemperatureData d : temperatures) {
            bucket(byMinute, minuteKey(d.getRecordTime())).addTemperature(d.getTemperatureValue());
        }
        for (HumidityData d : humidities) {
            bucket(byMinute, minuteKey(d.getRecordTime())).addHumidity(d.getHumidityValue());
        }
        for (SensorData d : dusts) {
            bucket(byMinute, minuteKey(d.getRecordTime())).addDust(d.getSmokeValue());
        }

        // 把该小时所有分钟桶再聚合成一个"小时均值"。
        List<Double> tAvgs = new ArrayList<>();
        List<Double> hAvgs = new ArrayList<>();
        List<Integer> dAvgs = new ArrayList<>();
        for (Accum acc : byMinute.values()) {
            acc.collectAvgs(tAvgs, hAvgs, dAvgs);
        }

        EnvironmentReportHourly row = reportRepository
                .findAll().stream() // 兜底查找；正常走唯一索引
                .filter(r -> deviceId.equals(r.getDeviceId()) && hourStart.equals(r.getHourTime()))
                .findFirst()
                .orElse(EnvironmentReportHourly.builder()
                        .deviceId(deviceId)
                        .hourTime(hourStart)
                        .build());

        row.setAvgTemperature(tAvgs.isEmpty() ? null : (float) avgDouble(tAvgs));
        row.setAvgHumidity(hAvgs.isEmpty() ? null : (float) avgDouble(hAvgs));
        row.setAvgDust(dAvgs.isEmpty() ? null : (float) avgInt(dAvgs));
        row.setSampleCount(sampleCount);
        reportRepository.save(row);

        log.info("[report-hourly] device={} hour={} samples={} temp={} hum={} dust={}",
                deviceId, hourStart, sampleCount, row.getAvgTemperature(), row.getAvgHumidity(), row.getAvgDust());
        return true;
    }

    @Override
    @Transactional
    public void aggregatePreviousHourForAll() {
        List<String> deviceIds = deviceRepository.findAll().stream()
                .map(com.chinasoft.smokesensor.entity.Device::getDeviceId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .collect(Collectors.toList());
        for (String deviceId : deviceIds) {
            try {
                aggregatePreviousHour(deviceId);
            } catch (Exception e) {
                log.warn("[report-hourly] 聚合失败 device={} : {}", deviceId, e.getMessage());
            }
        }
    }

    private boolean inRange(LocalDateTime time, LocalDateTime start, LocalDateTime end) {
        return time != null && !time.isBefore(start) && !time.isAfter(end);
    }

    private LocalDateTime minuteKey(LocalDateTime time) {
        return time.withSecond(0).withNano(0);
    }

    private Accum bucket(Map<LocalDateTime, Accum> map, LocalDateTime key) {
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

    private double avgDouble(List<Double> vals) {
        return vals.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private double avgInt(List<Integer> vals) {
        return vals.stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    /** 分钟级累积器：同分钟多采样取均值，再汇总到小时级。 */
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

        void collectAvgs(List<Double> tOut, List<Double> hOut, List<Integer> dOut) {
            if (!temperatures.isEmpty()) {
                tOut.add(temperatures.stream().mapToDouble(Double::doubleValue).average().orElse(0));
            }
            if (!humidities.isEmpty()) {
                hOut.add(humidities.stream().mapToDouble(Double::doubleValue).average().orElse(0));
            }
            if (!dusts.isEmpty()) {
                dOut.add((int) Math.round(dusts.stream().mapToInt(Integer::intValue).average().orElse(0)));
            }
        }
    }
}

package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.common.BusinessException;
import com.chinasoft.smokesensor.dto.SmokeHistoryPointResponse;
import com.chinasoft.smokesensor.dto.SmokeLatestResponse;
import com.chinasoft.smokesensor.dto.SmokeRealtimeResponse;
import com.chinasoft.smokesensor.dto.SmokeRestoreRequest;
import com.chinasoft.smokesensor.dto.SmokeRestoreResponse;
import com.chinasoft.smokesensor.dto.SmokeSimulateRequest;
import com.chinasoft.smokesensor.dto.SmokeSimulateResponse;
import com.chinasoft.smokesensor.dto.ThresholdSettingsResponse;
import com.chinasoft.smokesensor.entity.AlarmRecord;
import com.chinasoft.smokesensor.entity.Device;
import com.chinasoft.smokesensor.entity.HumidityData;
import com.chinasoft.smokesensor.entity.SensorData;
import com.chinasoft.smokesensor.entity.TemperatureData;
import com.chinasoft.smokesensor.repository.AlarmRecordRepository;
import com.chinasoft.smokesensor.repository.DeviceRepository;
import com.chinasoft.smokesensor.repository.HumidityDataRepository;
import com.chinasoft.smokesensor.repository.SensorDataRepository;
import com.chinasoft.smokesensor.repository.TemperatureDataRepository;
import com.chinasoft.smokesensor.service.SettingsService;
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

/**
 * 烟雾数据查询和模拟业务实现。
 *
 * <p>这一层负责把前端烟雾看板接口转换为数据库读写：
 * 查询 smoke_device 的最新状态、查询 smoke_data 的历史趋势、模拟写入烟雾数据，
 * 并在恢复正常时解除未处理告警。
 */
@Service
@RequiredArgsConstructor
public class SmokeServiceImpl implements SmokeService {

    private static final String UNIT = "ppm";
    // 当前 system_setting 表没有 normal/low 分界配置，因此 100ppm 暂时作为 low 风险起点。
    private static final int LOW_THRESHOLD = 100;
    private static final int HISTORY_LIMIT = 200;
    private static final int DEFAULT_SIMULATE_SMOKE_VALUE = 450;
    private static final int RESTORE_SMOKE_VALUE = 35;
    private static final String DEFAULT_DEVICE_ID = "SMK-001";
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
    private final TemperatureDataRepository temperatureDataRepository;
    private final HumidityDataRepository humidityDataRepository;
    private final SettingsService settingsService;

    /**
     * 查询设备最新烟雾状态。
     *
     * <p>处理流程：
     * 1. deviceId 为空时，使用最新一条烟雾记录反查设备；
     * 2. deviceId 不为空时，直接查询 smoke_device；
     * 3. 根据 lastHeartbeat 和 system_setting.heartbeat_timeout 判断设备是否离线；
     * 4. 离线时不返回旧烟雾值，避免前端把历史值误认为实时值。
     */
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
        return syncLatestAliases(SmokeLatestResponse.builder()
                .deviceId(device.getDeviceId())
                .smokeValue(device.getCurrentSmokeValue())
                .connected(true)
                .unit(UNIT)
                .updateTime(resolveLatestTime(device))
                .riskLevel(device.getCurrentRiskLevel())
                .riskScore(toRiskScore(device.getCurrentRiskLevel()))
                .alarmStatus(device.getCurrentAlarmStatus())
                .alarmType("alarm".equalsIgnoreCase(device.getCurrentAlarmStatus()) ? "smoke_high" : null)
                .build());
    }

    /**
     * 查询实时烟雾状态，同时附带最新温度和湿度。
     *
     * <p>处理流程：
     * 1. 复用 getLatestSmoke 获取烟雾状态和离线判断；
     * 2. 分别查询 temperature_data 和 humidity_data 最新一条记录；
     * 3. 有数据时填入对应值，无数据时保持 null，不影响字段结构。
     *
     * <p>温度、湿度供前端仪表盘实时卡片展示，不参与风险等级计算。
     */
    @Override
    @Transactional(readOnly = true)
    public SmokeRealtimeResponse getRealtimeSmoke(String deviceId) {
        SmokeLatestResponse latest = getLatestSmoke(deviceId);
        Double temperature = queryLatestTemperature(deviceId);
        Double humidity = queryLatestHumidity(deviceId);
        return SmokeRealtimeResponse.builder()
                .deviceId(latest.getDeviceId())
                .connected(Boolean.TRUE.equals(latest.getConnected()))
                .smokeValue(latest.getSmokeValue())
                .unit(latest.getUnit())
                .temperature(temperature)
                .humidity(humidity)
                .riskLevel(latest.getRiskLevel())
                .riskScore(latest.getRiskScore())
                .alarmStatus(latest.getAlarmStatus())
                .alarmType(latest.getAlarmType())
                .themeType(resolveThemeType(latest))
                .updateTime(latest.getUpdateTime())
                .message(latest.getMessage())
                .build();
    }

    /**
     * 查询指定设备最新温度值（℃）。
     *
     * <p>从 temperature_data 表取 recordTime 最新的数据，设备离线或无数据时返回 null。
     */
    private Double queryLatestTemperature(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return null;
        }
        return temperatureDataRepository.findTopByDeviceIdOrderByRecordTimeDesc(deviceId)
                .map(data -> Double.valueOf(data.getTemperatureValue()))
                .orElse(null);
    }

    /**
     * 查询指定设备最新湿度值（%RH）。
     *
     * <p>从 humidity_data 表取 recordTime 最新的数据，设备离线或无数据时返回 null。
     */
    private Double queryLatestHumidity(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return null;
        }
        return humidityDataRepository.findTopByDeviceIdOrderByRecordTimeDesc(deviceId)
                .map(data -> Double.valueOf(data.getHumidityValue()))
                .orElse(null);
    }

    /**
     * 查询烟雾历史趋势数据。
     *
     * <p>处理流程：
     * 1. 校验设备是否存在；
     * 2. 根据 range/start/end 计算查询时间范围；
     * 3. 默认只查询 source=sensor 或 source 为空的数据，避免模拟数据污染真实历史趋势；
     * 4. 最多返回 HISTORY_LIMIT 条，按 recordTime 升序返回给前端绘图。
     */
    @Override
    @Transactional(readOnly = true)
    public List<SmokeHistoryPointResponse> getHistory(
            String deviceId,
            String range,
            LocalDateTime start,
            LocalDateTime end,
            String source) {
        if (deviceId != null && !deviceId.isBlank() && !deviceRepository.existsByDeviceId(deviceId)) {
            throw BusinessException.notFound("设备不存在: " + deviceId);
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

    /**
     * 模拟烟雾升高。
     *
     * <p>处理流程：
     * 1. 生成一条 smoke_data 模拟记录；
     * 2. 更新 smoke_device 当前烟雾值、风险等级、告警状态和 lastHeartbeat；
     * 3. 当 smokeValue 达到 warning_threshold 时生成 alarm_record。
     *
     * <p>注意：该接口用于演示和联调，source 默认是 simulate。
     */
    @Override
    @Transactional
    public SmokeSimulateResponse simulateSmoke(SmokeSimulateRequest request) {
        LocalDateTime simulateTime = LocalDateTime.now();
        String deviceId = resolveSimulateDeviceId(request);
        int smokeValue = resolveSimulateSmokeValue(request);
        ThresholdSettingsResponse thresholdSettings = settingsService.getThresholdSettings();
        String riskLevel = mapRiskLevel(smokeValue, thresholdSettings);
        String alarmStatus = smokeValue >= thresholdSettings.getWarningThreshold() ? "alarm" : "safe";
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

    /**
     * 模拟恢复正常环境。
     *
     * <p>处理流程：
     * 1. 固定写入 35ppm 的恢复数据；
     * 2. 将设备当前状态更新为 normal/safe；
     * 3. 将该设备未处理或处理中的告警改为 resolved。
     *
     * <p>注意：恢复接口会写 smoke_data，并修改 alarm_record 的状态。
     */
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
                deviceId, List.of("unhandled", "pending", "processing"));
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
     * latest 接口未指定 deviceId 时，先取最新烟雾记录，再反查对应设备状态。
     */
    private SmokeLatestResponse getLatestSmokeFromLatestRecord() {
        SensorData sensorData = sensorDataRepository.findTopByOrderByRecordTimeDesc()
                .orElseThrow(() -> BusinessException.notFound("未找到烟雾数据"));
        Device device = deviceRepository.findByDeviceId(sensorData.getDeviceId()).orElse(null);
        if (device == null || isDeviceOffline(device)) {
            return toOfflineLatestResponse(sensorData.getDeviceId(), device == null ? null : device.getLastHeartbeat());
        }
        String alarmStatus = device == null ? null : device.getCurrentAlarmStatus();
        return syncLatestAliases(SmokeLatestResponse.builder()
                .deviceId(sensorData.getDeviceId())
                .smokeValue(sensorData.getSmokeValue())
                .connected(true)
                .unit(UNIT)
                .updateTime(sensorData.getRecordTime())
                .riskLevel(sensorData.getRiskLevel())
                .riskScore(toRiskScore(sensorData.getRiskLevel()))
                .alarmStatus(alarmStatus)
                .alarmType("alarm".equalsIgnoreCase(alarmStatus) ? "smoke_high" : null)
                .build());
    }

    /**
     * 构造离线状态响应；离线时不返回旧烟雾浓度，避免前端误展示历史数据。
     */
    private SmokeLatestResponse toOfflineLatestResponse(String deviceId, LocalDateTime lastHeartbeat) {
        return syncLatestAliases(SmokeLatestResponse.builder()
                .deviceId(deviceId)
                .smokeValue(null)
                .connected(false)
                .unit(UNIT)
                .updateTime(lastHeartbeat)
                .riskLevel("unknown")
                .riskScore(0)
                .alarmStatus("offline")
                .alarmType(null)
                .message(DEVICE_OFFLINE_MESSAGE)
                .build());
    }

    /**
     * 判断设备是否离线，统一使用 system_setting.heartbeat_timeout 作为超时时间。
     */
    private boolean isDeviceOffline(Device device) {
        return device.getLastHeartbeat() == null
                || device.getLastHeartbeat().isBefore(LocalDateTime.now()
                .minusSeconds(settingsService.getThresholdSettings().getHeartbeatTimeout()));
    }

    /**
     * 解析最新更新时间，优先使用 smoke_data 最新 recordTime，兜底使用设备最后心跳时间。
     */
    private LocalDateTime resolveLatestTime(Device device) {
        return sensorDataRepository.findTopByDeviceIdOrderByRecordTimeDesc(device.getDeviceId())
                .map(SensorData::getRecordTime)
                .orElse(device.getLastHeartbeat());
    }

    /**
     * 将 smoke_data 记录转换为前端趋势图需要的点位数据。
     */
    private SmokeHistoryPointResponse toHistoryPoint(SensorData sensorData) {
        return SmokeHistoryPointResponse.builder()
                .time(sensorData.getRecordTime())
                .value(sensorData.getSmokeValue())
                .threshold(settingsService.getThresholdSettings().getWarningThreshold())
                .build();
    }

    /**
     * 将风险等级转换为前端展示用分值。
     */
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

    /**
     * 根据连接状态和告警状态计算前端主题类型。
     */
    private String resolveThemeType(SmokeLatestResponse latest) {
        if (Boolean.FALSE.equals(latest.getConnected()) || "offline".equalsIgnoreCase(latest.getAlarmStatus())) {
            return "offline";
        }
        if ("alarm".equalsIgnoreCase(latest.getAlarmStatus())) {
            return "danger";
        }
        return "normal";
    }

    /**
     * 同步最新烟雾响应中的派生字段，当前主要用于补充 themeType。
     */
    private SmokeLatestResponse syncLatestAliases(SmokeLatestResponse response) {
        response.setThemeType(resolveThemeType(response));
        return response;
    }

    /**
     * 解析历史数据来源；默认 sensor，支持 sensor、simulate、all。
     */
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
        throw new IllegalArgumentException("source 只能是 sensor、simulate 或 all");
    }

    /**
     * 根据烟雾值和阈值配置映射风险等级。
     */
    private String mapRiskLevel(int smokeValue, ThresholdSettingsResponse thresholdSettings) {
        if (smokeValue >= thresholdSettings.getDangerThreshold()) {
            return "high";
        }
        if (smokeValue >= thresholdSettings.getWarningThreshold()) {
            return "medium";
        }
        if (smokeValue >= LOW_THRESHOLD) {
            return "low";
        }
        return "normal";
    }

    /**
     * 解析模拟接口使用的设备编号；未传 deviceId 时优先选择最近更新设备。
     */
    private String resolveSimulateDeviceId(SmokeSimulateRequest request) {
        if (request.getDeviceId() != null && !request.getDeviceId().isBlank()) {
            return request.getDeviceId();
        }
        return deviceRepository.findTopByOrderByUpdatedAtDesc()
                .map(Device::getDeviceId)
                .orElse(DEFAULT_DEVICE_ID);
    }

    /**
     * 解析恢复接口作用的设备；未传 deviceId 时选择最近更新设备。
     */
    private Device resolveRestoreDevice(SmokeRestoreRequest request) {
        if (request.getDeviceId() != null && !request.getDeviceId().isBlank()) {
            return deviceRepository.findByDeviceId(request.getDeviceId())
                    .orElseThrow(() -> BusinessException.notFound("设备不存在: " + request.getDeviceId()));
        }
        return deviceRepository.findTopByOrderByUpdatedAtDesc()
                .orElseThrow(() -> BusinessException.notFound("没有可恢复的设备"));
    }

    /**
     * 解析模拟烟雾值；请求未传 smokeValue 时使用默认高烟雾值。
     */
    private int resolveSimulateSmokeValue(SmokeSimulateRequest request) {
        if (request.getSmokeValue() != null) {
            return request.getSmokeValue();
        }
        if ("smoke_high".equalsIgnoreCase(request.getScenario())) {
            return DEFAULT_SIMULATE_SMOKE_VALUE;
        }
        return DEFAULT_SIMULATE_SMOKE_VALUE;
    }

    /**
     * 将前端传入的 range/start/end 转换为数据库查询时间范围。
     */
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

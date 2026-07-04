package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.common.BusinessException;
import com.chinasoft.smokesensor.dto.DeviceLatestDataResponse;
import com.chinasoft.smokesensor.dto.SensorDataResponse;
import com.chinasoft.smokesensor.dto.SmokeDataUploadRequest;
import com.chinasoft.smokesensor.dto.ThresholdSettingsResponse;
import com.chinasoft.smokesensor.entity.AlarmRecord;
import com.chinasoft.smokesensor.entity.Device;
import com.chinasoft.smokesensor.entity.SensorData;
import com.chinasoft.smokesensor.repository.AlarmRecordRepository;
import com.chinasoft.smokesensor.repository.DeviceRepository;
import com.chinasoft.smokesensor.repository.SensorDataRepository;
import com.chinasoft.smokesensor.service.DeviceDataService;
import com.chinasoft.smokesensor.service.SettingsService;
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

    // 风险等级阈值（ppm），对应 API 文档 3.1
    // 注意：system_setting 表当前没有 normal/low 分界配置，这里暂时固定为 100。
    private static final int LOW_THRESHOLD = 100;

    // 枚举值，对应 API 文档 3.1 / 3.2 / 3.4 / 3.7
    private static final String RISK_LEVEL_NORMAL = "normal";
    private static final String RISK_LEVEL_LOW = "low";
    private static final String RISK_LEVEL_MEDIUM = "medium";
    private static final String RISK_LEVEL_HIGH = "high";
    private static final String ALARM_STATUS_SAFE = "safe";
    private static final String ALARM_STATUS_ALARM = "alarm";
    private static final String ALARM_TYPE_SMOKE = "smoke";
    private static final String ALARM_STATUS_UNHANDLED = "unhandled";
    private static final String DEFAULT_SOURCE = "sensor";

    private final DeviceRepository deviceRepository;
    private final SensorDataRepository sensorDataRepository;
    private final AlarmRecordRepository alarmRecordRepository;
    private final SettingsService settingsService;

    @Override
    @Transactional
    // 设备不存在时返回明确错误，不再自动创建设备
    // source 为空时默认 sensor
    public DeviceLatestDataResponse uploadSmokeData(SmokeDataUploadRequest request) {
        LocalDateTime uploadTime = LocalDateTime.now();
        int smokeValue = request.getSmokeValue();
        ThresholdSettingsResponse thresholdSettings = settingsService.getThresholdSettings();
        String riskLevel = mapRiskLevel(smokeValue, thresholdSettings);
        // 注意：告警触发阈值统一使用 system_setting.warning_threshold。
        boolean alarm = smokeValue >= thresholdSettings.getWarningThreshold();
        String alarmStatus = alarm ? ALARM_STATUS_ALARM : ALARM_STATUS_SAFE;
        String source = request.getSource() == null || request.getSource().isBlank()
                ? DEFAULT_SOURCE
                : request.getSource();

        SensorData sensorData = SensorData.builder()
                .deviceId(request.getDeviceId())
                .smokeValue(smokeValue)
                .riskLevel(riskLevel)
                .recordTime(uploadTime)
                .source(source)
                .build();
        sensorDataRepository.save(sensorData);

        Device device = deviceRepository.findByDeviceId(request.getDeviceId())
                .orElseThrow(() -> BusinessException.notFound("Device not found: " + request.getDeviceId()));
        device.setOnline(true);
        device.setLastHeartbeat(uploadTime);
        device.setCurrentSmokeValue(smokeValue);
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
                    .smokeValue(smokeValue)
                    .riskLevel(riskLevel)
                    .status(ALARM_STATUS_UNHANDLED)
                    .triggeredAt(uploadTime)
                    .isSimulated("simulate".equalsIgnoreCase(source))
                    .createTime(uploadTime)
                    .updatedAt(uploadTime)
                    .build();
            alarmRecordRepository.save(alarmRecord);
        }

        return toDeviceLatestDataResponse(device);
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceLatestDataResponse getLatestData(String deviceId) {
        // device 表 current_* 字段即最新值冗余字段（见表结构设计 2），直接返回即可
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> BusinessException.notFound("Device not found: " + deviceId));

        return toDeviceLatestDataResponse(device);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SensorDataResponse> getHistoryData(String deviceId, int limit) {
        if (!deviceRepository.existsByDeviceId(deviceId)) {
            throw BusinessException.notFound("Device not found: " + deviceId);
        }
        int pageSize = Math.max(limit, 1);

        return sensorDataRepository.findByDeviceIdOrderByRecordTimeDesc(deviceId, PageRequest.of(0, pageSize))
                .stream()
                .map(this::toSensorDataResponse)
                .toList();
    }

    /**
     * 按 ppm 映射风险等级，对应 API 文档 3.1：
     * 0–100 normal / 101–199 low / 200–400 medium / >400 high。
     * 200 归 medium（今日告警统计口径：≥200 为中风险及以上）。
     */
    private String mapRiskLevel(int smokeValue, ThresholdSettingsResponse thresholdSettings) {
        if (smokeValue >= thresholdSettings.getDangerThreshold()) {
            return RISK_LEVEL_HIGH;
        }
        if (smokeValue >= thresholdSettings.getWarningThreshold()) {
            return RISK_LEVEL_MEDIUM;
        }
        if (smokeValue >= LOW_THRESHOLD) {
            return RISK_LEVEL_LOW;
        }
        return RISK_LEVEL_NORMAL;
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

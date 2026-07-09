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
import com.chinasoft.smokesensor.service.alarm.AlarmTriggeredEvent;
import com.chinasoft.smokesensor.config.AlarmWebSocketSessionManager;
import com.chinasoft.smokesensor.dto.AlarmWebSocketPayload;
import com.chinasoft.smokesensor.service.DeviceOnlineStatusService;
import com.chinasoft.smokesensor.service.DeviceOnlineStatusService.DeviceOnlineStatus;
import com.chinasoft.smokesensor.service.SettingsService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 硬件烟雾数据上传业务实现。
 *
 * <p>该类面向真实设备或硬件模拟器上传数据，负责写入 smoke_data、更新 smoke_device 最新状态，
 * 并在超过阈值时生成 alarm_record。
 */
@Service
@RequiredArgsConstructor
public class DeviceDataServiceImpl implements DeviceDataService {

    // 当前 system_setting 表没有 normal/low 分界配置，因此 100ppm 暂时作为 low 风险起点。
    private static final int LOW_THRESHOLD = 100;

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
    private final DeviceOnlineStatusService deviceOnlineStatusService;
    private final AlarmWebSocketSessionManager alarmWebSocketSessionManager;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 接收硬件上传的烟雾数据。
     *
     * <p>处理流程：
     * 1. 根据 system_setting 中的 warning_threshold/danger_threshold 计算风险等级；
     * 2. 将本次上报写入 smoke_data；
     * 3. 根据 deviceId 查找 smoke_device 并更新最新状态；
     * 4. 达到 warning_threshold 时生成一条 alarm_record。
     *
     * <p>注意：设备不存在时返回明确错误，不自动创建设备。
     */
    @Override
    @Transactional
    public DeviceLatestDataResponse uploadSmokeData(SmokeDataUploadRequest request) {
        LocalDateTime uploadTime = LocalDateTime.now();
        int smokeValue = request.getSmokeValue();
        ThresholdSettingsResponse thresholdSettings = settingsService.getThresholdSettings();
        String riskLevel = mapRiskLevel(smokeValue, thresholdSettings);
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
                .orElseThrow(() -> BusinessException.notFound("设备不存在: " + request.getDeviceId()));
        device.setOnline(true);
        device.setLastHeartbeat(uploadTime);
        device.setCurrentSmokeValue(smokeValue);
        device.setCurrentRiskLevel(riskLevel);
        device.setCurrentAlarmStatus(alarmStatus);
        if (device.getEnabled() == null) {
            device.setEnabled(true);
        }
        deviceRepository.save(device);

        String createdAlarmId = null;
        if (alarm) {
            // 同一个 alarmId 同时用于数据库记录和 WebSocket 推送，保证前端后续处理告警时能查到同一条记录。
            createdAlarmId = UUID.randomUUID().toString();
            AlarmRecord alarmRecord = AlarmRecord.builder()
                    .alarmId(createdAlarmId)
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

        if (alarm) {
            AlarmWebSocketPayload wsPayload = AlarmWebSocketPayload.builder()
                    .type("alarm")
                    .alarmId(createdAlarmId)
                    .deviceId(request.getDeviceId())
                    .level(riskLevel)
                    .smokeValue(smokeValue)
                    .alarmTime(uploadTime)
                    .build();
            alarmWebSocketSessionManager.broadcastAlarm(wsPayload);
            // 发布告警事件，由 AlarmEventListener 触发 QQ 推送（事件驱动，与告警业务解耦）
            applicationEventPublisher.publishEvent(new AlarmTriggeredEvent(
                    createdAlarmId, request.getDeviceId(), ALARM_TYPE_SMOKE, smokeValue, riskLevel,
                    uploadTime, "simulate".equalsIgnoreCase(source)));
        }

        return toDeviceLatestDataResponse(device);
    }

    /**
     * 查询指定设备最新状态。
     *
     * <p>该方法直接读取 smoke_device 的 current_* 字段，适合硬件上传接口之后返回设备当前状态。
     */
    /**
     * 查询指定设备最新状态。
     *
     * <p>该方法直接读取 smoke_device 的 current_* 字段，适合硬件上传接口之后返回设备当前状态。
     */
    @Override
    @Transactional(readOnly = true)
    public DeviceLatestDataResponse getLatestData(String deviceId) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> BusinessException.notFound("设备不存在: " + deviceId));

        return toDeviceLatestDataResponse(device);
    }
    /**
     * 查询指定设备历史上传数据。
     *
     * <p>该方法从 smoke_data 按 recordTime 倒序分页读取，用于旧接口或调试场景查看设备历史原始数据。
     */
    @Override
    @Transactional(readOnly = true)
    public List<SensorDataResponse> getHistoryData(String deviceId, int limit) {
        if (!deviceRepository.existsByDeviceId(deviceId)) {
            throw BusinessException.notFound("设备不存在: " + deviceId);
        }
        int pageSize = Math.max(limit, 1);

        return sensorDataRepository.findByDeviceIdOrderByRecordTimeDesc(deviceId, PageRequest.of(0, pageSize))
                .stream()
                .map(this::toSensorDataResponse)
                .toList();
    }
    /**
     * 根据烟雾值和阈值配置计算风险等级。
     *
     * <p>当前规则：smokeValue >= dangerThreshold 为 high；
     * smokeValue >= warningThreshold 为 medium；
     * smokeValue >= 100 为 low；其余为 normal。
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

    /**
     * 将设备实体转换为上传接口和最新数据接口使用的响应对象。
     */
    private DeviceLatestDataResponse toDeviceLatestDataResponse(Device device) {
        DeviceOnlineStatus onlineStatus = deviceOnlineStatusService.getStatus(device.getDeviceId());
        return DeviceLatestDataResponse.builder()
                .deviceId(device.getDeviceId())
                .online(onlineStatus.online())
                .lastHeartbeat(onlineStatus.lastDataAt())
                .currentSmokeValue(device.getCurrentSmokeValue())
                .currentRiskLevel(device.getCurrentRiskLevel())
                .currentAlarmStatus(device.getCurrentAlarmStatus())
                .enabled(device.getEnabled())
                .build();
    }

    /**
     * 将历史传感器数据实体转换为接口响应对象。
     */
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

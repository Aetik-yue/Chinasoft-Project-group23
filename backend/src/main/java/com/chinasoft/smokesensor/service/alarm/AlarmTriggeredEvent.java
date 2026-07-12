package com.chinasoft.smokesensor.service.alarm;

import java.time.LocalDateTime;

/**
 * 告警触发事件。
 *
 * <p>当烟雾/粉尘超过阈值生成 alarm_record 时发布此事件，
 * 由 {@link AlarmEventListener} 监听并触发 QQ 推送等副作用，
 * 以事件驱动方式将告警业务与外部推送解耦，避免侵入告警入库主流程。
 *
 * @param alarmId     告警业务编号
 * @param deviceId    设备编号
 * @param alarmType   告警类型（如 smoke）
 * @param smokeValue  触发时浓度（ppm）
 * @param riskLevel   风险等级（low/medium/high）
 * @param triggeredAt 触发时间
 * @param simulated   是否模拟触发
 */
public record AlarmTriggeredEvent(
        String alarmId,
        String deviceId,
        String alarmType,
        Integer smokeValue,
        String riskLevel,
        LocalDateTime triggeredAt,
        boolean simulated,
        Long userId,
        String metric,
        Double metricValue,
        Double thresholdValue,
        String unit) {

    /** 兼容既有烟雾告警调用；它们仍是系统级广播。 */
    public AlarmTriggeredEvent(String alarmId, String deviceId, String alarmType, Integer smokeValue,
            String riskLevel, LocalDateTime triggeredAt, boolean simulated) {
        this(alarmId, deviceId, alarmType, smokeValue, riskLevel, triggeredAt, simulated,
                null, null, smokeValue == null ? null : smokeValue.doubleValue(), null, "ppm");
    }
}

package com.chinasoft.smokesensor.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket 告警推送消息 DTO。
 *
 * <p>当后端触发告警时，通过 WebSocket 将本条消息推送至所有连接的前端页面，
 * 前端收到后弹出告警弹窗。
 *
 * <p>消息示例（JSON 格式）：
 * <pre>
 * {
 *   "type": "alarm",
 *   "alarmId": "ALM-20260705-001",
 *   "deviceId": "SMK-001",
 *   "level": "high",
 *   "smokeValue": 520,
 *   "alarmTime": "2026-07-05 16:00:00"
 * }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmWebSocketPayload {

    /** 消息类型，固定为 "alarm"，前端根据此字段判断是否为告警推送。 */
    private String type;

    /** 告警编号，对应 alarm_record.alarm_id。 */
    private String alarmId;

    /** 触发告警的设备编号。 */
    private String deviceId;

    /** 风险等级：medium / high。 */
    private String level;

    /** 触发告警时的烟雾浓度（ppm）。 */
    private Integer smokeValue;

    /** 告警触发时间。 */
    private LocalDateTime alarmTime;
}
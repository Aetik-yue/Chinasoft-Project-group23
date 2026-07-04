package com.chinasoft.smokesensor.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模拟烟雾升高响应 DTO。
 *
 * 返回模拟后的设备状态，以及本次是否创建了告警记录。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmokeSimulateResponse {

    private String deviceId;

    private Integer smokeValue;

    private String unit;

    private LocalDateTime updatedAt;

    private String riskLevel;

    private Integer riskScore;

    private String alarmStatus;

    private String alarmType;

    /**
     * 本次模拟触发告警时生成的 alarmId；未触发告警时为 null。
     */
    private String createdAlarmId;
}

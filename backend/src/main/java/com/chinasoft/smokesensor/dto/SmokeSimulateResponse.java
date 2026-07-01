package com.chinasoft.smokesensor.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    private String createdAlarmId;
}

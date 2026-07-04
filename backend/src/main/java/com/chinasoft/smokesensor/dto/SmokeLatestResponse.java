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
public class SmokeLatestResponse {

    private String deviceId;

    private Integer smokeValue;

    private Boolean connected;

    private String unit;

    private LocalDateTime updateTime;

    private String riskLevel;

    private Integer riskScore;

    private String alarmStatus;

    private String alarmType;

    private String themeType;

    private String message;
}

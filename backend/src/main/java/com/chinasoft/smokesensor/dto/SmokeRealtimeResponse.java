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
public class SmokeRealtimeResponse {

    private String deviceId;

    private Boolean connected;

    private Integer smokeValue;

    private Double temperature;

    private Double humidity;

    private String riskLevel;

    private String alarmStatus;

    private String themeType;

    private LocalDateTime updateTime;

    private String message;
}

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
public class SensorDataResponse {

    private String deviceId;

    private Double smokeValue;

    private String riskLevel;

    private LocalDateTime recordTime;

    private String source;

    private LocalDateTime createdAt;
}

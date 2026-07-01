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
public class DeviceLatestDataResponse {

    private String deviceId;

    private Boolean online;

    private LocalDateTime lastHeartbeat;

    private Double currentSmokeValue;

    private String currentRiskLevel;

    private String currentAlarmStatus;

    private Boolean enabled;
}

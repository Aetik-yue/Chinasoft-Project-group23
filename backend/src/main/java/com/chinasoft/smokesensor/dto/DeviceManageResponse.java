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
public class DeviceManageResponse {

    private String deviceId;

    private String name;

    private String location;

    private Boolean online;

    private LocalDateTime lastHeartbeat;

    private Integer currentSmokeValue;

    private String currentRiskLevel;

    private String currentAlarmStatus;

    private Boolean enabled;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

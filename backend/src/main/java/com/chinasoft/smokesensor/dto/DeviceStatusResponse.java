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
public class DeviceStatusResponse {

    private String deviceId;

    private String deviceName;

    private Boolean connected;

    private LocalDateTime lastHeartbeat;

    private String status;

    private String message;

    private Integer progress;
}

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
public class DeviceInfoResponse {

    private String deviceId;

    private String deviceName;

    private String model;

    private String firmwareVersion;

    private String location;

    private LocalDateTime lastHeartbeat;

    private Boolean connected;

    private String message;
}

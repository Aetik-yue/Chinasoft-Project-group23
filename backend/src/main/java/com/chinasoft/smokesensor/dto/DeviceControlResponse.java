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
public class DeviceControlResponse {

    private Boolean success;

    private String message;

    private String deviceId;

    private String target;

    private String action;

    private LocalDateTime operatedAt;
}

package com.chinasoft.smokesensor.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备控制响应 DTO。
 *
 * 表示控制指令已被后端接收并写入 device_control 表。
 */
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

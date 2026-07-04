package com.chinasoft.smokesensor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备控制请求 DTO。
 *
 * 用于 /api/device/control，前端传入设备编号、控制对象和目标动作。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceControlRequest {

    private String deviceId;

    /**
     * 控制对象，例如 switch、buzzer、alarm_light。
     */
    private String target;

    /**
     * 控制动作，例如 on 或 off。
     */
    private String action;
}

package com.chinasoft.smokesensor.controller;

import com.chinasoft.smokesensor.common.ApiResult;
import com.chinasoft.smokesensor.dto.DeviceControlRequest;
import com.chinasoft.smokesensor.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/device")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    /**
     * 查询设备运行状态，用于前端判断设备在线、离线或告警状态。
     */
    @GetMapping("/status")
    public ApiResult getDeviceStatus(@RequestParam(required = false) String deviceId) {
        return ApiResult.ok(deviceService.getDeviceStatus(deviceId));
    }

    /**
     * 查询设备基础信息，用于前端展示设备名称、位置、固件和连接状态。
     */
    @GetMapping("/info")
    public ApiResult getDeviceInfo(@RequestParam(required = false) String deviceId) {
        return ApiResult.ok(deviceService.getDeviceInfo(deviceId));
    }

    /**
     * 下发设备控制指令，当前通过 device_control 表保存控制状态，后续可由 MQTT/硬件侧消费。
     */
    @PostMapping("/control")
    public ApiResult controlDevice(@RequestBody DeviceControlRequest request) {
        return ApiResult.ok(deviceService.controlDevice(request));
    }
}

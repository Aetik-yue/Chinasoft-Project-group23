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

    @GetMapping("/status")
    public ApiResult getDeviceStatus(@RequestParam(required = false) String deviceId) {
        return ApiResult.ok(deviceService.getDeviceStatus(deviceId));
    }

    @PostMapping("/control")
    public ApiResult controlDevice(@RequestBody DeviceControlRequest request) {
        return ApiResult.ok(deviceService.controlDevice(request));
    }
}

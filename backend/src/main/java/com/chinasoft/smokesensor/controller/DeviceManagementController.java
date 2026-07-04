package com.chinasoft.smokesensor.controller;

import com.chinasoft.smokesensor.common.ApiResult;
import com.chinasoft.smokesensor.dto.DeviceCreateRequest;
import com.chinasoft.smokesensor.dto.DeviceUpdateRequest;
import com.chinasoft.smokesensor.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 设备管理接口控制器
 * 负责 /api/devices 这一组接口：
 * - 查询设备列表
 * - 新增设备
 * - 编辑设备
 * - 解绑设备
 */
@RestController
@RequestMapping("/api/devices")  
@RequiredArgsConstructor
public class DeviceManagementController {

    private final DeviceService deviceService;

    @GetMapping
    // 
    public ApiResult listDevices(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled) {
        return ApiResult.ok(deviceService.listDevices(keyword, enabled));
    }

    @PostMapping
    public ApiResult createDevice(@Valid @RequestBody DeviceCreateRequest request) {
        return ApiResult.ok(deviceService.createDevice(request));
    }

    @PutMapping("/{deviceId}")
    public ApiResult updateDevice(
            @PathVariable String deviceId,
            @RequestBody DeviceUpdateRequest request) {
        // 编辑设备时，路径里的 deviceId 是设备业务编号，不允许通过编辑接口修改。
        return ApiResult.ok(deviceService.updateDevice(deviceId, request));
    }

    @DeleteMapping("/{deviceId}")
    public ApiResult deleteDevice(@PathVariable String deviceId) {
        // 解绑设备时不做物理删除，只调用业务层执行软删除，避免影响历史烟雾数据和告警记录。
        return ApiResult.ok(deviceService.deleteDevice(deviceId));
    }
}

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
 * 设备管理接口控制器，负责 /api/devices 下的列表、新增、编辑和解绑。
 */
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceManagementController {

    private final DeviceService deviceService;

    /**
     * 查询设备列表，可按关键字和启用状态筛选，用于前端设备管理页。
     */
    @GetMapping
    public ApiResult listDevices(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled) {
        return ApiResult.ok(deviceService.listDevices(keyword, enabled));
    }

    /**
     * 新增设备主表记录，设备编号必须唯一。
     */
    @PostMapping
    public ApiResult createDevice(@Valid @RequestBody DeviceCreateRequest request) {
        return ApiResult.ok(deviceService.createDevice(request));
    }

    /**
     * 编辑设备基础信息，路径中的 deviceId 是设备业务编号，不通过请求体修改。
     */
    @PutMapping("/{deviceId}")
    public ApiResult updateDevice(
            @PathVariable String deviceId,
            @RequestBody DeviceUpdateRequest request) {
        return ApiResult.ok(deviceService.updateDevice(deviceId, request));
    }

    /**
     * 解绑设备，业务层执行软删除，只将 enabled 置为 false，避免影响历史烟雾数据和告警记录。
     */
    @DeleteMapping("/{deviceId}")
    public ApiResult deleteDevice(@PathVariable String deviceId) {
        return ApiResult.ok(deviceService.deleteDevice(deviceId));
    }
}

package com.chinasoft.smokesensor.controller;

import com.chinasoft.smokesensor.common.ApiResult;
import com.chinasoft.smokesensor.dto.SmokeDataUploadRequest;
import com.chinasoft.smokesensor.service.DeviceDataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sensor")
@RequiredArgsConstructor
public class DeviceDataController {

    private final DeviceDataService deviceDataService;

    /**
     * 接收硬件或模拟硬件上传的烟雾数据，具体入库、设备状态更新和告警生成由业务层完成。
     */
    @PostMapping("/upload")
    public ApiResult uploadSmokeData(@Valid @RequestBody SmokeDataUploadRequest request) {
        return ApiResult.ok(deviceDataService.uploadSmokeData(request));
    }
}

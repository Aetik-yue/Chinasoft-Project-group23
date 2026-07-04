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

    @PostMapping("/upload")
    public ApiResult uploadSmokeData(@Valid @RequestBody SmokeDataUploadRequest request) {
        return ApiResult.ok(deviceDataService.uploadSmokeData(request));
    }
}

package com.chinasoft.smokesensor.controller;

import com.chinasoft.smokesensor.common.ApiResult;
import com.chinasoft.smokesensor.dto.ThresholdSettingsRequest;
import com.chinasoft.smokesensor.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    /**
     * 读取系统阈值配置，数据来源于 system_setting 表。
     */
    @GetMapping("/threshold")
    public ApiResult getThresholdSettings() {
        return ApiResult.ok(settingsService.getThresholdSettings());
    }

    /**
     * 保存系统阈值配置，只更新已有配置项，不创建新表或新字段。
     */
    @PostMapping("/threshold")
    public ApiResult updateThresholdSettings(@Valid @RequestBody ThresholdSettingsRequest request) {
        return ApiResult.ok(settingsService.updateThresholdSettings(request));
    }
}

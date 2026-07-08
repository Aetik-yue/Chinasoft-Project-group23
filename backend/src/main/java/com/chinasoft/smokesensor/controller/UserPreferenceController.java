package com.chinasoft.smokesensor.controller;

import com.chinasoft.smokesensor.common.ApiResult;
import com.chinasoft.smokesensor.dto.UserPreferencesRequest;
import com.chinasoft.smokesensor.service.UserPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/preferences")
@RequiredArgsConstructor
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;

    /**
     * 读取当前用户的页面偏好配置。
     */
    @GetMapping
    public ApiResult getPreferences() {
        return ApiResult.ok(userPreferenceService.getCurrentUserPreferences());
    }

    /**
     * 保存当前用户的页面偏好配置。支持部分更新。
     */
    @PutMapping
    public ApiResult updatePreferences(@RequestBody(required = false) UserPreferencesRequest request) {
        return ApiResult.ok(userPreferenceService.updateCurrentUserPreferences(request));
    }
}

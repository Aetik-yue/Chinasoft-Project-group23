package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.dto.ThresholdSettingsRequest;
import com.chinasoft.smokesensor.dto.ThresholdSettingsResponse;

public interface SettingsService {

    /**
     * 读取阈值和单位配置。
     */
    ThresholdSettingsResponse getThresholdSettings();

    /**
     * 更新阈值和单位配置。
     */
    ThresholdSettingsResponse updateThresholdSettings(ThresholdSettingsRequest request);
}

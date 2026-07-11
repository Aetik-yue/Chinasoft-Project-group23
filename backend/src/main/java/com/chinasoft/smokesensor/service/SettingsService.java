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

    /**
     * 读取 API Keys。
     */
    com.chinasoft.smokesensor.dto.ApiKeysResponse getApiKeys();

    /**
     * 更新 API Keys。
     */
    com.chinasoft.smokesensor.dto.ApiKeysResponse updateApiKeys(com.chinasoft.smokesensor.dto.ApiKeysRequest request);

    /**
     * 读取 QQ 白名单配置。
     */
    com.chinasoft.smokesensor.dto.QqWhitelistResponse getQqWhitelist();

    /**
     * 更新 QQ 白名单配置。
     */
    com.chinasoft.smokesensor.dto.QqWhitelistResponse updateQqWhitelist(com.chinasoft.smokesensor.dto.QqWhitelistRequest request);
}

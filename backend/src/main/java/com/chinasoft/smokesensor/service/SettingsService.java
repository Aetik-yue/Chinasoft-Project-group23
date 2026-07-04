package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.dto.ThresholdSettingsRequest;
import com.chinasoft.smokesensor.dto.ThresholdSettingsResponse;

public interface SettingsService {

    ThresholdSettingsResponse getThresholdSettings();

    ThresholdSettingsResponse updateThresholdSettings(ThresholdSettingsRequest request);
}

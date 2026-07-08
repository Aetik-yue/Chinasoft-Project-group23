package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.dto.UserPreferencesRequest;
import com.chinasoft.smokesensor.dto.UserPreferencesResponse;

public interface UserPreferenceService {

    /**
     * 读取当前用户偏好。鉴权接入前，当前用户固定为 userId=1。
     */
    UserPreferencesResponse getCurrentUserPreferences();

    /**
     * 保存当前用户偏好。鉴权接入前，当前用户固定为 userId=1。
     */
    UserPreferencesResponse updateCurrentUserPreferences(UserPreferencesRequest request);
}

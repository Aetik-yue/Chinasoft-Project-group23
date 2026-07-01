package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.dto.DeviceStatusResponse;

public interface DeviceService {

    DeviceStatusResponse getDeviceStatus(String deviceId);
}

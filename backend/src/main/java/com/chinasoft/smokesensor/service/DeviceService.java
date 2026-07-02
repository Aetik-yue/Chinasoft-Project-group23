package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.dto.DeviceControlRequest;
import com.chinasoft.smokesensor.dto.DeviceControlResponse;
import com.chinasoft.smokesensor.dto.DeviceInfoResponse;
import com.chinasoft.smokesensor.dto.DeviceStatusResponse;

public interface DeviceService {

    DeviceStatusResponse getDeviceStatus(String deviceId);

    DeviceInfoResponse getDeviceInfo(String deviceId);

    DeviceControlResponse controlDevice(DeviceControlRequest request);
}

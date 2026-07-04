package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.dto.DeviceControlRequest;
import com.chinasoft.smokesensor.dto.DeviceControlResponse;
import com.chinasoft.smokesensor.dto.DeviceCreateRequest;
import com.chinasoft.smokesensor.dto.DeviceDeleteResponse;
import com.chinasoft.smokesensor.dto.DeviceInfoResponse;
import com.chinasoft.smokesensor.dto.DeviceManageResponse;
import com.chinasoft.smokesensor.dto.DeviceStatusResponse;
import com.chinasoft.smokesensor.dto.DeviceUpdateRequest;
import java.util.List;

public interface DeviceService {

    DeviceStatusResponse getDeviceStatus(String deviceId);

    DeviceInfoResponse getDeviceInfo(String deviceId);

    DeviceControlResponse controlDevice(DeviceControlRequest request);

    List<DeviceManageResponse> listDevices(String keyword, Boolean enabled);

    DeviceManageResponse createDevice(DeviceCreateRequest request);

    DeviceManageResponse updateDevice(String deviceId, DeviceUpdateRequest request);

    DeviceDeleteResponse deleteDevice(String deviceId);
}

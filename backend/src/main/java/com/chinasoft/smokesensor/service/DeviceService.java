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

    /**
     * 查询设备当前状态。
     */
    DeviceStatusResponse getDeviceStatus(String deviceId);

    /**
     * 查询设备基础信息。
     */
    DeviceInfoResponse getDeviceInfo(String deviceId);

    /**
     * 下发或保存设备控制指令。
     */
    DeviceControlResponse controlDevice(DeviceControlRequest request);

    /**
     * 查询设备管理列表，支持关键字和启用状态筛选。
     */
    List<DeviceManageResponse> listDevices(String keyword, Boolean enabled);

    /**
     * 新增设备。
     */
    DeviceManageResponse createDevice(DeviceCreateRequest request);

    /**
     * 编辑设备基础信息。
     */
    DeviceManageResponse updateDevice(String deviceId, DeviceUpdateRequest request);

    /**
     * 解绑设备，当前采用软删除方式。
     */
    DeviceDeleteResponse deleteDevice(String deviceId);
}

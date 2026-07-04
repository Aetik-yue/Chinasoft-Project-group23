package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.dto.DeviceLatestDataResponse;
import com.chinasoft.smokesensor.dto.SensorDataResponse;
import com.chinasoft.smokesensor.dto.SmokeDataUploadRequest;
import java.util.List;

public interface DeviceDataService {

    /**
     * 接收设备上传的烟雾数据，完成数据入库、设备状态更新和告警判断。
     */
    DeviceLatestDataResponse uploadSmokeData(SmokeDataUploadRequest request);

    /**
     * 查询指定设备最新上传的数据。
     */
    DeviceLatestDataResponse getLatestData(String deviceId);

    /**
     * 查询指定设备历史传感器数据。
     */
    List<SensorDataResponse> getHistoryData(String deviceId, int limit);
}

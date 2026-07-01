package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.dto.DeviceLatestDataResponse;
import com.chinasoft.smokesensor.dto.SensorDataResponse;
import com.chinasoft.smokesensor.dto.SmokeDataUploadRequest;
import java.util.List;

public interface DeviceDataService {

    DeviceLatestDataResponse uploadSmokeData(SmokeDataUploadRequest request);

    DeviceLatestDataResponse getLatestData(String deviceId);

    List<SensorDataResponse> getHistoryData(String deviceId, int limit);
}

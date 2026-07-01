package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.dto.SmokeHistoryPointResponse;
import com.chinasoft.smokesensor.dto.SmokeLatestResponse;
import com.chinasoft.smokesensor.dto.SmokeRestoreRequest;
import com.chinasoft.smokesensor.dto.SmokeRestoreResponse;
import com.chinasoft.smokesensor.dto.SmokeSimulateRequest;
import com.chinasoft.smokesensor.dto.SmokeSimulateResponse;
import java.time.LocalDateTime;
import java.util.List;

public interface SmokeService {

    SmokeLatestResponse getLatestSmoke(String deviceId);

    List<SmokeHistoryPointResponse> getHistory(
            String deviceId,
            String range,
            LocalDateTime start,
            LocalDateTime end,
            String source);

    SmokeSimulateResponse simulateSmoke(SmokeSimulateRequest request);

    SmokeRestoreResponse restoreSmoke(SmokeRestoreRequest request);
}

package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.dto.SmokeHistoryPointResponse;
import com.chinasoft.smokesensor.dto.SmokeLatestResponse;
import com.chinasoft.smokesensor.dto.SmokeRealtimeResponse;
import com.chinasoft.smokesensor.dto.SmokeRestoreRequest;
import com.chinasoft.smokesensor.dto.SmokeRestoreResponse;
import com.chinasoft.smokesensor.dto.SmokeSimulateRequest;
import com.chinasoft.smokesensor.dto.SmokeSimulateResponse;
import java.time.LocalDateTime;
import java.util.List;

public interface SmokeService {

    /**
     * 查询设备最新烟雾状态。
     */
    SmokeLatestResponse getLatestSmoke(String deviceId);

    /**
     * 查询设备实时展示状态。
     */
    SmokeRealtimeResponse getRealtimeSmoke(String deviceId);

    /**
     * 查询烟雾历史趋势数据。
     */
    List<SmokeHistoryPointResponse> getHistory(
            String deviceId,
            String range,
            LocalDateTime start,
            LocalDateTime end,
            String source);

    /**
     * 模拟烟雾升高并触发对应状态更新。
     */
    SmokeSimulateResponse simulateSmoke(SmokeSimulateRequest request);

    /**
     * 模拟烟雾恢复正常并解除未处理告警。
     */
    SmokeRestoreResponse restoreSmoke(SmokeRestoreRequest request);
}

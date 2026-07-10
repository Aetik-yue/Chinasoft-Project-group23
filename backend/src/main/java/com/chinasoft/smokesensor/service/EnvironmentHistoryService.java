package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.dto.EnvironmentHistoryResponse;
import java.util.List;

/**
 * 鹦鹉成长报告所需的环境历史：把温度 / 湿度 / 粉尘三张表合并成按时间对齐的时序。
 */
public interface EnvironmentHistoryService {

    /**
     * 查询指定设备在某时间范围内的环境历史。
     *
     * @param deviceId 设备编号，为空则跨设备聚合
     * @param range    时间范围关键字：24h / 7d / 30d（默认 24h）
     * @return 按时间升序排列的环境采样序列
     */
    List<EnvironmentHistoryResponse> getHistory(String deviceId, String range);
}

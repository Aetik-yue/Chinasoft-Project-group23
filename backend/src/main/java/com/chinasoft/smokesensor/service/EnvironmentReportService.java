package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.dto.EnvironmentHistoryResponse;
import java.util.List;

/**
 * 成长报告：读预聚合的小时报表 + 触发聚合任务。
 */
public interface EnvironmentReportService {

    /**
     * 读取某设备在时间范围内的小时报表（已预聚合）。
     *
     * @param deviceId 设备编号
     * @param range    时间范围：24h / 7d / 30d（默认 24h）
     * @return 按时间升序的环境采样序列，缺项为 null
     */
    List<EnvironmentHistoryResponse> getHourlyHistory(String deviceId, String range);

    /**
     * 聚合指定设备在某个小时窗口内的采样，写入 / 更新小时报表。
     * 由定时任务每个整点调用，聚合的是"上一个完整小时"。
     *
     * @param deviceId 设备编号
     * @return 该小时是否有有效采样
     */
    boolean aggregatePreviousHour(String deviceId);

    /**
     * 对所有已知设备聚合上一个完整小时。
     */
    void aggregatePreviousHourForAll();
}

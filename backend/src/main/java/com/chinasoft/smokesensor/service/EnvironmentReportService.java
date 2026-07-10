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
     * 按自然日/周/月读取成长报告数据（针对指定或最近一个已结束的完整周期）。
     *
     * @param deviceId 设备编号
     * @param range    daily / weekly / monthly（默认 daily）
     * @param date     YYYY-MM-DD；为空时 daily=昨天、weekly=上周日、monthly=上月末
     * @return 按时间升序的聚合点序列（daily 每小时一点，weekly/monthly 每天一点）
     */
    List<EnvironmentHistoryResponse> getReport(String deviceId, String range, String date);

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

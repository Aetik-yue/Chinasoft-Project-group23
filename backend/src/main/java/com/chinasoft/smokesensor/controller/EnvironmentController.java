package com.chinasoft.smokesensor.controller;

import com.chinasoft.smokesensor.common.ApiResult;
import com.chinasoft.smokesensor.service.EnvironmentHistoryService;
import com.chinasoft.smokesensor.service.EnvironmentReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 鹦鹉成长报告所需的环境历史数据。
 *
 * 把温度 / 湿度 / 粉尘三张表合并成按时间对齐的时序，供前端绘制成长报告曲线。
 */
@RestController
@RequestMapping("/api/environment")
@RequiredArgsConstructor
public class EnvironmentController {

    private final EnvironmentHistoryService environmentHistoryService;
    private final EnvironmentReportService environmentReportService;

    /**
     * 查询环境历史趋势（原始采样聚合）。
     *
     * @param deviceId 设备编号（对应鹦鹉笼舍绑定的监测设备），为空则返回空序列
     * @param range    时间范围：24h / 7d / 30d，默认 24h
     */
    @GetMapping("/history")
    public ApiResult getHistory(@RequestParam(required = false) String deviceId,
                                @RequestParam(defaultValue = "24h") String range) {
        return ApiResult.ok(environmentHistoryService.getHistory(deviceId, range));
    }

    /**
     * 查询预聚合的小时报表（成长报告主路径）。
     *
     * @param deviceId 设备编号
     * @param range    时间范围：24h / 7d / 30d，默认 24h
     */
    @GetMapping("/hourly")
    public ApiResult getHourly(@RequestParam(required = false) String deviceId,
                               @RequestParam(defaultValue = "24h") String range) {
        return ApiResult.ok(environmentReportService.getHourlyHistory(deviceId, range));
    }
}

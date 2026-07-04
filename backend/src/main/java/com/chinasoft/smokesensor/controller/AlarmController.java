package com.chinasoft.smokesensor.controller;

import com.chinasoft.smokesensor.common.ApiResult;
import com.chinasoft.smokesensor.dto.AlarmHandleRequest;
import com.chinasoft.smokesensor.service.AlarmService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alarm")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;

    /**
     * 查询今日告警统计，用于前端展示今日告警数、昨日告警数和变化率。
     */
    @GetMapping("/stat/today")
    public ApiResult getTodayAlarmStat() {
        return ApiResult.ok(alarmService.getTodayStat());
    }

    /**
     * 查询告警日志列表，支持分页、设备编号、处理状态、风险等级和时间范围筛选。
     */
    @GetMapping("/logs")
    public ApiResult getAlarmLogs(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ApiResult.ok(alarmService.getAlarmLogs(limit, page, pageSize, deviceId, status, level, start, end));
    }

    /**
     * 处理指定告警，由前端提交告警编号、处理人和处理备注。
     */
    @PostMapping("/handle")
    public ApiResult handleAlarm(@Valid @RequestBody AlarmHandleRequest request) {
        return ApiResult.ok(alarmService.handleAlarm(request));
    }
}

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

    @GetMapping("/stat/today")
    public ApiResult getTodayAlarmStat() {
        return ApiResult.ok(alarmService.getTodayStat());
    }

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

    @PostMapping("/handle")
    public ApiResult handleAlarm(@Valid @RequestBody AlarmHandleRequest request) {
        return ApiResult.ok(alarmService.handleAlarm(request));
    }
}

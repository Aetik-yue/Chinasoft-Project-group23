package com.chinasoft.smokesensor.controller;

import com.chinasoft.smokesensor.common.ApiResult;
import com.chinasoft.smokesensor.service.AlarmService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alarm")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;

    @GetMapping("/list")
    public ApiResult getAlarmList(@RequestParam(defaultValue = "50") int limit) {
        return ApiResult.ok(alarmService.getAlarmList(limit));
    }

    @GetMapping("/device/{deviceId}")
    public ApiResult getAlarmsByDeviceId(@PathVariable String deviceId) {
        return ApiResult.ok(alarmService.getAlarmsByDeviceId(deviceId));
    }
}

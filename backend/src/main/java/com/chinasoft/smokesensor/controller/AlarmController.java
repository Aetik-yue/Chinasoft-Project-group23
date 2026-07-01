package com.chinasoft.smokesensor.controller;

import com.chinasoft.smokesensor.dto.AlarmRecordResponse;
import com.chinasoft.smokesensor.service.AlarmService;
import java.util.List;
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
    public List<AlarmRecordResponse> getAlarmList(@RequestParam(defaultValue = "50") int limit) {
        return alarmService.getAlarmList(limit);
    }

    @GetMapping("/device/{deviceId}")
    public List<AlarmRecordResponse> getAlarmsByDeviceId(@PathVariable String deviceId) {
        return alarmService.getAlarmsByDeviceId(deviceId);
    }
}

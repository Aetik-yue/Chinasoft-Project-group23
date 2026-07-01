package com.chinasoft.smokesensor.controller;

import com.chinasoft.smokesensor.common.ApiResult;
import com.chinasoft.smokesensor.dto.SmokeSimulateRequest;
import com.chinasoft.smokesensor.service.SmokeService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/smoke")
@RequiredArgsConstructor
public class SmokeController {

    private final SmokeService smokeService;

    @GetMapping("/latest")
    public ApiResult getLatestSmoke(@RequestParam(required = false) String deviceId) {
        return ApiResult.ok(smokeService.getLatestSmoke(deviceId));
    }

    @GetMapping("/history")
    public ApiResult getSmokeHistory(
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ApiResult.ok(smokeService.getHistory(deviceId, range, start, end));
    }

    @PostMapping("/simulate")
    public ApiResult simulateSmoke(@Valid @RequestBody SmokeSimulateRequest request) {
        return ApiResult.ok(smokeService.simulateSmoke(request));
    }
}

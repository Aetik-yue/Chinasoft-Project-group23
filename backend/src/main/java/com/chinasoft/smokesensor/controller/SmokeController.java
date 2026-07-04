package com.chinasoft.smokesensor.controller;

import com.chinasoft.smokesensor.common.ApiResult;
import com.chinasoft.smokesensor.dto.SmokeRestoreRequest;
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

    /**
     * 查询最新烟雾状态，用于前端当前浓度卡片和风险状态展示。
     */
    @GetMapping("/latest")
    public ApiResult getLatestSmoke(@RequestParam(required = false) String deviceId) {
        return ApiResult.ok(smokeService.getLatestSmoke(deviceId));
    }

    /**
     * 查询实时烟雾状态，返回连接状态、主题状态和当前烟雾数据。
     */
    @GetMapping("/realtime")
    public ApiResult getRealtimeSmoke(@RequestParam(required = false) String deviceId) {
        return ApiResult.ok(smokeService.getRealtimeSmoke(deviceId));
    }

    /**
     * 查询烟雾历史趋势，默认只返回真实传感器数据，避免模拟数据污染趋势图。
     */
    @GetMapping("/history")
    public ApiResult getSmokeHistory(
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "sensor") String source) {
        return ApiResult.ok(smokeService.getHistory(deviceId, range, start, end, source));
    }

    /**
     * 模拟烟雾升高，用于联调和演示，不代表真实硬件数据。
     */
    @PostMapping("/simulate")
    public ApiResult simulateSmoke(@Valid @RequestBody SmokeSimulateRequest request) {
        return ApiResult.ok(smokeService.simulateSmoke(request));
    }

    /**
     * 模拟恢复正常环境，用于演示解除告警和恢复安全状态。
     */
    @PostMapping("/restore")
    public ApiResult restoreSmoke(@RequestBody SmokeRestoreRequest request) {
        return ApiResult.ok(smokeService.restoreSmoke(request));
    }
}

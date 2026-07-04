package com.chinasoft.smokesensor.controller;

import com.chinasoft.smokesensor.common.ApiResult;
import com.chinasoft.smokesensor.service.SystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final SystemService systemService;

    /**
     * 查询系统状态，包括当前时间、设备总数和在线设备数。
     */
    @GetMapping("/status")
    public ApiResult getSystemStatus() {
        return ApiResult.ok(systemService.getSystemStatus());
    }
}

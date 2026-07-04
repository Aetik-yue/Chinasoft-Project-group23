package com.chinasoft.smokesensor.controller;

import com.chinasoft.smokesensor.common.ApiResult;
import com.chinasoft.smokesensor.service.RuntimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/runtime")
@RequiredArgsConstructor
public class RuntimeController {

    private final RuntimeService runtimeService;

    /**
     * 查询运行态连接快照，用于前端页面初始化时判断展示大屏还是未连接状态。
     */
    @GetMapping("/link-snapshot")
    public ApiResult getLinkSnapshot(@RequestParam(required = false) String deviceId) {
        return ApiResult.ok(runtimeService.getLinkSnapshot(deviceId));
    }
}

package com.chinasoft.smokesensor.controller;

import com.chinasoft.smokesensor.common.ApiResult;
import com.chinasoft.smokesensor.service.VisionCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 视觉复核接口控制器，用于对接烟雾或火焰图像识别能力。
 */
@RestController
@RequestMapping("/api/vision")
@RequiredArgsConstructor
public class VisionController {

    private final VisionCheckService visionCheckService;

    /**
     * 根据告警编号查询视觉复核结果，用于告警后查看图像识别结论。
     */
    @GetMapping("/check")
    public ApiResult check(@RequestParam(required = false) String alarmId) {
        return ApiResult.ok(visionCheckService.checkByAlarmId(alarmId));
    }
}

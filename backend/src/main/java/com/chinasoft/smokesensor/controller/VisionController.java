package com.chinasoft.smokesensor.controller;

import com.chinasoft.smokesensor.common.ApiResult;
import com.chinasoft.smokesensor.service.VisionCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 视觉复核接口（P2 加分项），对接 SmartJavaAI 火焰/烟雾识别。
 */
@RestController
@RequestMapping("/api/vision")
@RequiredArgsConstructor
public class VisionController {

    private final VisionCheckService visionCheckService;

    /** 报警后查看摄像头截图与 AI 识别结果，支持人工确认。 */
    @GetMapping("/check")
    public ApiResult check(@RequestParam(required = false) String alarmId) {
        return ApiResult.ok(visionCheckService.checkByAlarmId(alarmId));
    }
}

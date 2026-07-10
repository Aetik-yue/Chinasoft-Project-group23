package com.chinasoft.smokesensor.controller;

import com.chinasoft.smokesensor.client.QwenVisionClient;
import com.chinasoft.smokesensor.common.ApiResult;
import com.chinasoft.smokesensor.service.ParrotBehaviorService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 鹦鹉行为识别接口（P2 加分项）：YOLO 检测鹦鹉 + CLIP 零样本行为分类。
 */
@RestController
@RequestMapping("/api/parrot")
@RequiredArgsConstructor
public class ParrotBehaviorController {

    private final ParrotBehaviorService parrotBehaviorService;
    private final QwenVisionClient qwenVisionClient;

    /** 用配置的截图识别：返回是否检测到鹦鹉、行为标签与置信度，并落库一条记录。 */
    @GetMapping("/behavior")
    public ApiResult behavior(@RequestParam(required = false) String deviceId) {
        return ApiResult.ok(parrotBehaviorService.check(deviceId));
    }

    /** 上传图片识别：multipart 文件上传，直接识别上传的鹦鹉图片。 */
    @PostMapping("/behavior")
    public ApiResult behavior(@RequestParam("file") MultipartFile file,
                              @RequestParam(required = false) String deviceId) {
        return ApiResult.ok(parrotBehaviorService.check(file, deviceId));
    }

    /** 3D 模拟模式：把画面帧（base64 JPEG）发给 Qwen-VL 多模态大模型，识别种类与行为。 */
    @PostMapping("/vision/vlm")
    public ApiResult analyzeByVlm(@RequestBody Map<String, String> body) {
        String image = body.get("image");
        if (image == null || image.isBlank()) {
            return ApiResult.error(4001, "缺少 image 字段");
        }
        QwenVisionClient.VisionResult result = qwenVisionClient.analyze(image);
        return ApiResult.ok(result);
    }

    /** 今日行为统计：按 behavior 分组 count。 */
    @GetMapping("/behavior/today-stats")
    public ApiResult todayStats(@RequestParam(required = false) String deviceId) {
        return ApiResult.ok(parrotBehaviorService.getTodayStats(deviceId));
    }
}

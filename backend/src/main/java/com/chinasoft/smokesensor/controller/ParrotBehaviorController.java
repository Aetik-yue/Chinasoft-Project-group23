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
    private final com.chinasoft.smokesensor.client.LlmClient llmClient;

    /** 用配置的截图识别：返回是否检测到鹦鹉、行为标签与置信度，并落库一条记录。 */
    @GetMapping("/behavior")
    public ApiResult behavior(@RequestParam(required = false) String deviceId,
                              @RequestParam String petId) {
        return ApiResult.ok(parrotBehaviorService.check(deviceId, petId));
    }

    /** 上传图片识别：multipart 文件上传，直接识别上传的鹦鹉图片。 */
    @PostMapping("/behavior")
    public ApiResult behavior(@RequestParam("file") MultipartFile file,
                              @RequestParam(required = false) String deviceId,
                              @RequestParam String petId) {
        return ApiResult.ok(parrotBehaviorService.check(file, deviceId, petId));
    }

    /** 3D 模拟模式：把画面帧（base64 JPEG）发给 Qwen-VL 多模态大模型，识别种类与行为。 */
    @PostMapping("/vision/vlm")
    public ApiResult analyzeByVlm(@RequestBody Map<String, String> body) {
        String image = body.get("image");
        String hint = body.get("hint");
        String deviceId = body.get("deviceId");
        String petId = body.get("petId");
        if (image == null || image.isBlank()) {
            return ApiResult.error(4001, "缺少 image 字段");
        }
        QwenVisionClient.VisionResult result = qwenVisionClient.analyze(image, hint);
        if (result != null && result.behavior() != null) {
            parrotBehaviorService.saveVlmRecord(deviceId, petId, result);
        }
        return ApiResult.ok(result);
    }

    /** 行为统计：按 behavior 分组 count。 */
    @GetMapping("/behavior/today-stats")
    public ApiResult todayStats(@RequestParam String petId,
                                @RequestParam(required = false) String date) {
        return ApiResult.ok(parrotBehaviorService.getTodayStats(petId, date));
    }

    /** 行为周期统计：支持 today、day、week、month 四种时间范围。 */
    @GetMapping("/behavior/stats")
    public ApiResult behaviorStats(@RequestParam String petId,
                                   @RequestParam(defaultValue = "today") String range,
                                   @RequestParam(required = false) String date) {
        return ApiResult.ok(parrotBehaviorService.getBehaviorStats(petId, range, date));
    }

    /** 与鹦鹉进行对话：基于 DeepSeek 或 MaxKB 大模型生成鹦鹉口吻的回复 */
    @PostMapping("/chat")
    public ApiResult chat(@RequestBody Map<String, String> body) {
        String message = body.get("message");
        if (message == null || message.isBlank()) {
            return ApiResult.error(4001, "消息不能为空");
        }
        
        String parrotSystemPrompt = "你是一只可爱粘人的宠物小太阳鹦鹉，你喜欢吃谷物和瓜子，说话活泼生动，喜欢在句尾加“小尾巴”如“啾！”或“咔咔！”。用户是你的主人。请用可爱的口吻简短地回答主人的话，长度控制在50字以内，不要显得像个机器。";
        
        java.util.List<Map<String, Object>> messages = java.util.List.of(
            Map.of("role", "system", "content", parrotSystemPrompt),
            Map.of("role", "user", "content", message)
        );
        
        if (llmClient.isEnabled()) {
            Map<String, Object> reply = llmClient.chat(messages, null);
            if (reply != null && reply.get("content") != null) {
                return ApiResult.ok(reply.get("content"));
            }
        }
        
        String[] fallbacks = {
            "啾！主人你在说什么呀？给点瓜子吃嘛啾~",
            "咔咔！最喜欢主人了！摸摸头啾！",
            "啾啾！你在想我吗？我今天有好好站着哦！",
            "啾！咔咔！要吃苹果加水啾！",
            "啾啾~ 主人真好！我们一起玩荡秋千吧啾！"
        };
        String fallback = fallbacks[(int) (Math.random() * fallbacks.length)];
        return ApiResult.ok(fallback);
    }
}

package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.dto.ParrotBehaviorResponse;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

/** 鹦鹉行为识别服务：截图 → YOLO 检测鹦鹉 → CLIP 行为分类 → 落库。 */
public interface ParrotBehaviorService {

    /** 用 application.yml 配置的截图识别。 */
    ParrotBehaviorResponse check(String deviceId);

    /** 上传图片识别。 */
    ParrotBehaviorResponse check(MultipartFile file, String deviceId);

    /** 实时分析一帧：返回所有鹦鹉框，行为/种类按节流降采样，DB 落库节流。 */
    ParrotBehaviorResponse analyzeRealtime(String imagePath, String deviceId);

    /** 行为统计：按 behavior 分组 count，返回 {date, total, stats:[{behavior, count}]}。 */
    Map<String, Object> getTodayStats(String deviceId, String dateStr);

    /** 按今日、指定日、周或月统计行为识别记录。 */
    Map<String, Object> getBehaviorStats(String deviceId, String range, String dateStr);

    /** 保存 VLM 识别记录到数据库 */
    void saveVlmRecord(String deviceId, com.chinasoft.smokesensor.client.QwenVisionClient.VisionResult result);
}

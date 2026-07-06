package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.dto.ParrotBehaviorResponse;
import org.springframework.web.multipart.MultipartFile;

/** 鹦鹉行为识别服务：截图 → YOLO 检测鹦鹉 → CLIP 行为分类 → 落库。 */
public interface ParrotBehaviorService {

    /** 用 application.yml 配置的截图识别。 */
    ParrotBehaviorResponse check(String deviceId);

    /** 上传图片识别。 */
    ParrotBehaviorResponse check(MultipartFile file, String deviceId);
}

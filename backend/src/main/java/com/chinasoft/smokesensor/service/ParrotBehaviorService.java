package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.dto.ParrotBehaviorResponse;

/** 鹦鹉行为识别服务：截图 → YOLO 检测鹦鹉 → CLIP 行为分类 → 落库。 */
public interface ParrotBehaviorService {

    ParrotBehaviorResponse check(String deviceId);
}

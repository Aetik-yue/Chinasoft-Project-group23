package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.dto.VisionCheckResponse;

/** 视觉复核服务：按告警编号触发摄像头截图分析并落库。 */
public interface VisionCheckService {

    VisionCheckResponse checkByAlarmId(String alarmId);
}

package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.dto.VisionCheckResponse;

/**
 * 视觉复核服务，用于按告警编号查询或触发图像识别结果。
 */
public interface VisionCheckService {

    /**
     * 根据告警编号查询视觉复核结果。
     */
    VisionCheckResponse checkByAlarmId(String alarmId);
}

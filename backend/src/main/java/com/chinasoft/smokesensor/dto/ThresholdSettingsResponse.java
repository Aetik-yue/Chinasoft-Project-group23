package com.chinasoft.smokesensor.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 阈值配置响应 DTO。
 *
 * 返回当前系统使用的风险阈值、告警阈值、离线超时时间和单位。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThresholdSettingsResponse {

    private Integer normalMax;

    private Integer warningThreshold;

    private Integer dangerThreshold;

    /**
     * 当前告警触发阈值；现阶段等同于 warningThreshold。
     */
    private Integer alarmThreshold;

    private Integer heartbeatTimeout;

    private String unit;

    private LocalDateTime updatedAt;
}

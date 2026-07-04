package com.chinasoft.smokesensor.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 实时烟雾状态响应 DTO。
 *
 * 用于 /api/smoke/realtime，字段比 latest 更偏前端实时看板展示。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmokeRealtimeResponse {

    private String deviceId;

    private Boolean connected;

    private Integer smokeValue;

    private String unit;

    private Double temperature;

    private Double humidity;

    private String riskLevel;

    private Integer riskScore;

    private String alarmStatus;

    private String alarmType;

    /**
     * 前端根据该字段切换正常、危险或离线展示样式。
     */
    private String themeType;

    private LocalDateTime updateTime;

    private String message;
}

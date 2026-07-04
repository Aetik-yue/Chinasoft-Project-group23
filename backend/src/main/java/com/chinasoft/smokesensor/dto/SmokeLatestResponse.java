package com.chinasoft.smokesensor.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 最新烟雾状态响应 DTO。
 *
 * 用于 /api/smoke/latest，前端当前浓度卡片和风险状态展示读取该对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmokeLatestResponse {

    private String deviceId;

    private Integer smokeValue;

    /**
     * 设备是否连接；离线时 smokeValue 会返回 null，避免展示旧数据。
     */
    private Boolean connected;

    private String unit;

    private LocalDateTime updateTime;

    private String riskLevel;

    private Integer riskScore;

    private String alarmStatus;

    private String alarmType;

    /**
     * 前端主题类型，例如 normal、danger、offline。
     */
    private String themeType;

    private String message;
}

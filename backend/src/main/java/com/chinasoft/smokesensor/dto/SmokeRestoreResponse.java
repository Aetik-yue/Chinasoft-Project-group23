package com.chinasoft.smokesensor.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模拟恢复正常响应 DTO。
 *
 * 返回恢复后的安全状态，以及本次解除的告警数量。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmokeRestoreResponse {

    private String deviceId;

    private Integer smokeValue;

    private String unit;

    private LocalDateTime updatedAt;

    private String riskLevel;

    private Integer riskScore;

    private String alarmStatus;

    private String alarmType;

    /**
     * 本次恢复操作解除的未处理或处理中告警数量。
     */
    private Integer resolvedAlarmCount;

    private String message;
}

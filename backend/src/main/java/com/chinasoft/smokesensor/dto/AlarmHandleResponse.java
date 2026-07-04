package com.chinasoft.smokesensor.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 告警处理响应 DTO。
 *
 * 返回告警处理后的状态、处理人、处理时间和备注。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmHandleResponse {

    private String alarmId;

    private String deviceId;

    private String status;

    private String handler;

    private LocalDateTime handledAt;

    private LocalDateTime resolvedAt;

    private String remark;

    private String message;
}

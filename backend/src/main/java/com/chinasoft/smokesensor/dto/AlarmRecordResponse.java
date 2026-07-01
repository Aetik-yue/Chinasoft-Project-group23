package com.chinasoft.smokesensor.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmRecordResponse {

    private String alarmId;

    private String deviceId;

    private String alarmType;

    private Integer smokeValue;

    private String riskLevel;

    private String status;

    private String handler;

    private LocalDateTime handledAt;

    private String remark;

    private LocalDateTime triggeredAt;

    private LocalDateTime resolvedAt;

    private Boolean isSimulated;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

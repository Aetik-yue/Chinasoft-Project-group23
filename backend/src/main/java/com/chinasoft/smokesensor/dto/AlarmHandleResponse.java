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

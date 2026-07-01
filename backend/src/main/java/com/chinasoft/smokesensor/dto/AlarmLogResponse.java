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
public class AlarmLogResponse {

    private String alarmId;

    private LocalDateTime alarmTime;

    private String deviceId;

    private String type;

    private Integer value;

    private String level;

    private String status;

    private String handler;

    private String remark;
}

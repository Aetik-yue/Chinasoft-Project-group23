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
public class ThresholdSettingsResponse {

    private Integer normalMax;

    private Integer warningThreshold;

    private Integer dangerThreshold;

    private Integer alarmThreshold;

    private Integer heartbeatTimeout;

    private String unit;

    private LocalDateTime updatedAt;
}

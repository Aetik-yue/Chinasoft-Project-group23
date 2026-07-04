package com.chinasoft.smokesensor.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThresholdSettingsRequest {

    @Min(1)
    private Integer warningThreshold;

    @Min(1)
    private Integer dangerThreshold;

    @Min(1)
    private Integer heartbeatTimeout;

    private String unit;
}

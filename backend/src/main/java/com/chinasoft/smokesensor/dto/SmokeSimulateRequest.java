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
public class SmokeSimulateRequest {

    private String deviceId;

    @Min(0)
    private Integer smokeValue;

    private String scenario;

    private String source;
}

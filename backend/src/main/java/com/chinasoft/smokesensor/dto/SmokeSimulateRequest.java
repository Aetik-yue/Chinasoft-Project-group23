package com.chinasoft.smokesensor.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模拟烟雾升高请求 DTO。
 *
 * 用于 /api/smoke/simulate，仅用于联调和演示，不代表真实硬件数据。
 */
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

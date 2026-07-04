package com.chinasoft.smokesensor.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 阈值配置更新请求 DTO。
 *
 * 用于 /api/settings/threshold，字段为空时业务层保留原配置。
 */
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

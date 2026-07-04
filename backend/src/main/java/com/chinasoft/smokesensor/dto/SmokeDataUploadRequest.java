package com.chinasoft.smokesensor.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 硬件烟雾数据上传请求 DTO。
 *
 * 用于 /api/sensor/upload，后端接收后写入 smoke_data 并更新 smoke_device。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmokeDataUploadRequest {

    @NotBlank
    private String deviceId;

    @NotNull
    @Min(0)
    private Integer smokeValue;

    private String source;
}

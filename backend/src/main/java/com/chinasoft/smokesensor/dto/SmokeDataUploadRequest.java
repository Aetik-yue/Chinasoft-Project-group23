package com.chinasoft.smokesensor.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

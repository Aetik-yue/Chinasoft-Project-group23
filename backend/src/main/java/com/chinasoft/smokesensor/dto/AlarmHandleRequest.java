package com.chinasoft.smokesensor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmHandleRequest {

    @NotBlank
    private String alarmId;

    @NotBlank
    private String handler;

    private String remark;
}

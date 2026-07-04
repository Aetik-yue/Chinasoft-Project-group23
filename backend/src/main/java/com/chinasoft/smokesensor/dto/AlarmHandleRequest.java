package com.chinasoft.smokesensor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 告警处理请求 DTO。
 *
 * 前端提交告警编号、处理人和处理备注，后端将告警更新为 resolved。
 */
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

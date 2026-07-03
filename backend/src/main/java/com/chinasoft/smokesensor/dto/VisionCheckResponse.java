package com.chinasoft.smokesensor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 视觉复核响应，对应 API 文档 4.8.1 GET /vision/check 的 data 结构。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisionCheckResponse {

    private String alarmId;

    /** 摄像头截图 URL（骨架阶段用配置的截图路径占位）。 */
    private String imageUrl;

    /** AI 识别结果：smoke_detected / fire_detected / none */
    private String result;

    /** 置信度 0–1 */
    private Double confidence;

    /** 是否人工确认 */
    private Boolean confirmed;
}

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
public class ParrotBehaviorResponse {

    private String deviceId;

    /** 是否检测到鹦鹉 */
    private Boolean parrotDetected;

    /** 鹦鹉检测置信度 */
    private Double parrotConfidence;

    /** 行为标签（进食/飞翔/…），未检测到鹦鹉或 CLIP 未启用时为 null */
    private String behavior;

    /** 行为置信度 */
    private Double behaviorConfidence;

    private String imageUrl;

    private LocalDateTime checkedAt;
}

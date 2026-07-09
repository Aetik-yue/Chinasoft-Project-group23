package com.chinasoft.smokesensor.dto;

import java.time.LocalDateTime;
import java.util.List;
import com.chinasoft.smokesensor.dto.ParrotBox;
import com.chinasoft.smokesensor.dto.ParrotAbnormalEvent;
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

    /** 种类标签（虎皮/玄凤/…），未检测到鹦鹉或 CLIP 未启用时为 null */
    private String species;

    /** 种类置信度 */
    private Double speciesConfidence;

    private String imageUrl;

    private LocalDateTime checkedAt;

    /** 实时模式下检测到的所有鹦鹉框（定位+计数用），非实时接口为 null */
    private List<ParrotBox> boxes;

    /** 异常行为预警事件列表，无异常时为 null/空 */
    private List<ParrotAbnormalEvent> abnormal;
}

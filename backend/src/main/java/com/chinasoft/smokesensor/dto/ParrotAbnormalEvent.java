package com.chinasoft.smokesensor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 异常行为预警事件（实时模式由 {@code ParrotAbnormalDetector} 产出）。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParrotAbnormalEvent {

    /** 异常类型。 */
    public enum Type {
        /** 连续未检测到鹦鹉：失踪/逃逸 */
        MISSING,
        /** 检测到但长时间几乎不动：可能受伤/昏迷 */
        STATIC,
        /** 持续梳理羽毛：疑似拔羽 */
        PLUCKING,
        /** 鹦鹉数量与期望不符 */
        COUNT
    }

    /** 严重程度。 */
    public enum Severity {
        WARNING,
        DANGER
    }

    private Type type;
    private Severity severity;
    private String message;
    /** 触发时间（epoch 毫秒） */
    private long at;
}

package com.chinasoft.smokesensor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 鹦鹉行为识别记录，对应表 parrot_behavior_record。
 * 每次调用 /api/parrot/behavior 落一条，用于行为历史/趋势展示。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "parrot_behavior_record")
public class ParrotBehaviorRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 64)
    private String deviceId;

    @Column(name = "image_url", nullable = false, length = 512)
    private String imageUrl;

    @Column(name = "parrot_detected", nullable = false)
    private Boolean parrotDetected;

    @Column(name = "parrot_confidence")
    private Double parrotConfidence;

    /** 行为标签：进食/饮水/梳理羽毛/飞翔/攀爬/睡觉 等 */
    @Column(name = "behavior", length = 64)
    private String behavior;

    @Column(name = "behavior_confidence")
    private Double behaviorConfidence;

    /** 检测时间，由数据库 CURRENT_TIMESTAMP 默认生成。 */
    @Column(name = "checked_at", insertable = false, updatable = false)
    private LocalDateTime checkedAt;
}

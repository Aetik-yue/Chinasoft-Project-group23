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
 * AI 视觉复核记录，对应表 vision_check（P2 加分项）。
 * 告警触发后调用摄像头截图 + SmartJavaAI 火焰/烟雾识别，结果落库供前端展示与人工确认。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vision_check")
public class VisionCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alarm_id", nullable = false, unique = true, length = 64)
    private String alarmId;

    @Column(name = "device_id", nullable = false, length = 64)
    private String deviceId;

    @Column(name = "image_url", nullable = false, length = 512)
    private String imageUrl;

    /** AI 识别结果：smoke_detected / fire_detected / none */
    @Column(name = "ai_result", length = 64)
    private String aiResult;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "confirmed", nullable = false)
    private Boolean confirmed;

    @Column(name = "confirmed_by", length = 64)
    private String confirmedBy;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    /** 复核时间，由数据库 CURRENT_TIMESTAMP 默认生成。 */
    @Column(name = "checked_at", insertable = false, updatable = false)
    private LocalDateTime checkedAt;
}

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
 * 视觉复核记录实体，对应 MySQL 表 vision_check。
 *
 * <p>告警触发后可通过摄像头截图和视觉识别模型进行复核，
 * 识别结果写入该表，供前端展示和人工确认。
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

    /**
     * AI 识别结果：smoke_detected / fire_detected / none。
     */
    @Column(name = "ai_result", length = 64)
    private String aiResult;

    /**
     * AI 识别置信度。
     */
    @Column(name = "confidence")
    private Double confidence;

    /**
     * 人工是否已确认视觉复核结果。
     */
    @Column(name = "confirmed", nullable = false)
    private Boolean confirmed;

    @Column(name = "confirmed_by", length = 64)
    private String confirmedBy;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    /**
     * 复核时间，由数据库 CURRENT_TIMESTAMP 默认生成。
     */
    @Column(name = "checked_at", insertable = false, updatable = false)
    private LocalDateTime checkedAt;
}

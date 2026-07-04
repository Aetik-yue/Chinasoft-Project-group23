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
 * 告警记录实体，对应 MySQL 表 alarm_record。
 *
 * 当烟雾值达到告警阈值时，后端会在该表生成记录；
 * 前端告警日志、今日告警统计和人工处理告警都基于该表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "alarm_record")
public class AlarmRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alarm_id", nullable = false, unique = true, length = 64)
    private String alarmId;

    @Column(name = "device_id", nullable = false, length = 64)
    private String deviceId;

    @Column(name = "alarm_type", nullable = false, length = 64)
    private String alarmType;

    /**
     * 触发告警时的烟雾值。
     */
    @Column(name = "smoke_value")
    private Integer smokeValue;

    /**
     * 触发告警时的风险等级。
     */
    @Column(name = "risk_level", length = 32)
    private String riskLevel;

    /**
     * 告警处理状态，例如 unhandled、processing、resolved。
     */
    @Column(name = "status", length = 32)
    private String status;

    @Column(name = "handler", length = 100)
    private String handler;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "triggered_at")
    private LocalDateTime triggeredAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /**
     * 是否由模拟接口产生，用于区分真实硬件告警和演示告警。
     */
    @Column(name = "is_simulated")
    private Boolean isSimulated;

    /**
     * 表中的历史必填字段，新增告警时需要显式写入，不能删除映射。
     */
    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

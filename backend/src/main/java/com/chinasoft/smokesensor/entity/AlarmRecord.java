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

    @Column(name = "smoke_value")
    private Integer smokeValue;

    @Column(name = "risk_level", length = 32)
    private String riskLevel;

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

    @Column(name = "is_simulated")
    private Boolean isSimulated;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

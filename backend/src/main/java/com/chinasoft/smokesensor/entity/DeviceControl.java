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
 * 设备控制状态实体，对应 MySQL 表 device_control。
 *
 * 当前后端在 /api/device/control 中写入该表，
 * 用于保存前端下发的联动控制状态；真实硬件执行可由后续 MQTT 或设备端读取该表实现。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "device_control")
public class DeviceControl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 64)
    private String deviceId;

    @Column(name = "control_type", nullable = false, length = 64)
    private String controlType;

    /**
     * 控制对象显示名称，例如开关、蜂鸣器、报警灯。
     */
    @Column(name = "control_name", length = 128)
    private String controlName;

    /**
     * 控制目标状态：on 或 off。
     */
    @Column(name = "status", nullable = false, length = 32)
    private String status;

    /**
     * 是否参与自动联动，当前默认开启。
     */
    @Column(name = "auto_linkage", nullable = false)
    private Boolean autoLinkage;

    @Column(name = "last_operated_at")
    private LocalDateTime lastOperatedAt;

    @Column(name = "last_operated_by", length = 64)
    private String lastOperatedBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}

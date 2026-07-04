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
 * 设备主表实体，对应 MySQL 表 smoke_device。
 *
 * 该表保存烟感设备的基础信息和最新状态：
 * 设备编号、安装位置、在线状态、最后心跳、当前烟雾值、当前风险等级和当前告警状态。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "smoke_device")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, unique = true, length = 64)
    private String deviceId;

    /**
     * 设备显示名称，前端设备列表和设备详情中展示。
     */
    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "online")
    private Boolean online;

    /**
     * 设备最后一次上报或心跳时间，后端用它判断设备是否离线。
     */
    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;

    /**
     * 设备当前烟雾浓度，来自最近一次硬件上传或模拟数据。
     */
    @Column(name = "current_smoke_value")
    private Integer currentSmokeValue;

    /**
     * 当前风险等级：normal、low、medium、high 或 unknown。
     */
    @Column(name = "current_risk_level", length = 32)
    private String currentRiskLevel;

    /**
     * 当前告警状态：safe、alarm、offline 等。
     */
    @Column(name = "current_alarm_status", length = 32)
    private String currentAlarmStatus;

    @Column(name = "enabled")
    private Boolean enabled;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}

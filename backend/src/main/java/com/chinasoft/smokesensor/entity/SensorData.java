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
 * 烟雾历史数据实体，对应 MySQL 表 smoke_data。
 *
 * 该表保存每一次烟雾数据采集记录，用于历史趋势图、最新数据追溯和告警判断依据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "smoke_data")
public class SensorData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 64)
    private String deviceId;

    @Column(name = "smoke_value", nullable = false)
    private Integer smokeValue;

    /**
     * 本条数据对应的风险等级，由烟雾值和阈值配置计算得到。
     */
    @Column(name = "risk_level", length = 32)
    private String riskLevel;

    /**
     * 数据采集时间或模拟产生时间，历史趋势按该字段排序。
     */
    @Column(name = "record_time")
    private LocalDateTime recordTime;

    /**
     * 数据来源：sensor 表示真实硬件数据，simulate 表示后端模拟数据。
     */
    @Column(name = "source", length = 32)
    private String source;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}

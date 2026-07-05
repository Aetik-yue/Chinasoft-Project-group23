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
 * 湿度历史数据实体，对应 MySQL 表 humidity_data。
 *
 * <p>该表保存每一次湿度采集记录，用于鹦鹉实时监控页的湿度展示和历史趋势分析。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "humidity_data")
public class HumidityData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 64)
    private String deviceId;

    /** 相对湿度值，单位 %RH。 */
    @Column(name = "humidity_value", nullable = false)
    private Float humidityValue;

    /** 数据记录时间（传感器上报时间），历史趋势按该字段排序。 */
    @Column(name = "record_time")
    private LocalDateTime recordTime;

    /** 数据来源：sensor 表示真实硬件数据，simulate 表示模拟数据。 */
    @Column(name = "source", length = 32)
    private String source;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
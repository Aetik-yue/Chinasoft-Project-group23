package com.chinasoft.smokesensor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 环境小时报表：每小时一行预聚合结果。
 *
 * <p>定时任务在每个整点聚合上一小时的温度 / 湿度 / 粉尘采样，写入（或更新）对应行。
 * 成长报告直接读这张表，不再现场聚合原始数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "environment_report_hourly",
        uniqueConstraints = @UniqueConstraint(name = "uk_device_hour",
                columnNames = {"device_id", "hour_time"}))
public class EnvironmentReportHourly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 64)
    private String deviceId;

    /** 该小时的起始时间，如 2026-07-05 13:00:00。 */
    @Column(name = "hour_time", nullable = false)
    private LocalDateTime hourTime;

    @Column(name = "avg_temperature")
    private Float avgTemperature;

    @Column(name = "avg_humidity")
    private Float avgHumidity;

    @Column(name = "avg_dust")
    private Float avgDust;

    /** 该小时参与聚合的原始采样数量（为 0 时等同于无数据）。 */
    @Column(name = "sample_count", nullable = false)
    private Integer sampleCount;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;
}

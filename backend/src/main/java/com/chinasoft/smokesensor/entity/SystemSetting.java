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
 * 系统配置实体，对应 MySQL 表 system_setting。
 *
 * 当前后端主要读取阈值和离线判断配置：
 * warning_threshold、danger_threshold、heartbeat_timeout、unit。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "system_setting")
public class SystemSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", nullable = false, unique = true, length = 128)
    private String settingKey;

    /**
     * 配置值，数据库中统一按字符串保存，业务层负责转换为整数或文本。
     */
    @Column(name = "setting_value", nullable = false, length = 512)
    private String settingValue;

    /**
     * 配置分组，例如 threshold、general。
     */
    @Column(name = "setting_group", length = 64)
    private String settingGroup;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}

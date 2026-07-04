package com.chinasoft.smokesensor.repository;

import com.chinasoft.smokesensor.entity.SystemSetting;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 系统配置表 system_setting 的数据库访问层。
 */
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

    /**
     * 按配置 key 查询单个配置项。
     */
    Optional<SystemSetting> findBySettingKey(String settingKey);

    /**
     * 批量查询多个配置项，阈值配置接口使用。
     */
    List<SystemSetting> findBySettingKeyIn(Collection<String> settingKeys);
}

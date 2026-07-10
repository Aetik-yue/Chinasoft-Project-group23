package com.chinasoft.smokesensor.repository;

import com.chinasoft.smokesensor.entity.EnvironmentReportHourly;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 环境小时报表 repository。
 */
public interface EnvironmentReportHourlyRepository extends JpaRepository<EnvironmentReportHourly, Long> {

    /**
     * 查询某设备在时间范围内的小时报表，按时间升序（供前端绘图）。
     */
    List<EnvironmentReportHourly> findByDeviceIdAndHourTimeBetweenOrderByHourTimeAsc(
            String deviceId, LocalDateTime start, LocalDateTime end);

    /**
     * 按唯一键 (deviceId, hourTime) 精确查一行，供定时聚合时判断"已存在则更新、否则新建"。
     */
    Optional<EnvironmentReportHourly> findByDeviceIdAndHourTime(String deviceId, LocalDateTime hourTime);
}

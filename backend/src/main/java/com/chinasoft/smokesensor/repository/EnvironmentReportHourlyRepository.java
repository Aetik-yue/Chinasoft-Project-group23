package com.chinasoft.smokesensor.repository;

import com.chinasoft.smokesensor.entity.EnvironmentReportHourly;
import java.time.LocalDateTime;
import java.util.List;
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
}

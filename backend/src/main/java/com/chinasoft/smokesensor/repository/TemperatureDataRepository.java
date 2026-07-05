package com.chinasoft.smokesensor.repository;

import com.chinasoft.smokesensor.entity.TemperatureData;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 温度历史数据表 temperature_data 的数据库访问层。
 */
public interface TemperatureDataRepository extends JpaRepository<TemperatureData, Long>,
        JpaSpecificationExecutor<TemperatureData> {

    /**
     * 查询指定设备最新一条温度数据。
     */
    Optional<TemperatureData> findTopByDeviceIdOrderByRecordTimeDesc(String deviceId);

    /**
     * 查询指定设备全部历史温度数据，按采集时间倒序。
     */
    List<TemperatureData> findByDeviceIdOrderByRecordTimeDesc(String deviceId);

    /**
     * 分页查询指定设备历史温度数据，按采集时间倒序。
     */
    Page<TemperatureData> findByDeviceIdOrderByRecordTimeDesc(String deviceId, Pageable pageable);
}
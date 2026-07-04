package com.chinasoft.smokesensor.repository;

import com.chinasoft.smokesensor.entity.SensorData;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 烟雾历史数据表 smoke_data 的数据库访问层。
 */
public interface SensorDataRepository extends JpaRepository<SensorData, Long>, JpaSpecificationExecutor<SensorData> {

    /**
     * 查询指定设备最新一条烟雾数据。
     */
    Optional<SensorData> findTopByDeviceIdOrderByRecordTimeDesc(String deviceId);

    /**
     * 查询全局最新一条烟雾数据，未指定设备时使用。
     */
    Optional<SensorData> findTopByOrderByRecordTimeDesc();

    /**
     * 查询指定设备全部历史烟雾数据，按采集时间倒序。
     */
    List<SensorData> findByDeviceIdOrderByRecordTimeDesc(String deviceId);

    /**
     * 分页查询指定设备历史烟雾数据，按采集时间倒序。
     */
    Page<SensorData> findByDeviceIdOrderByRecordTimeDesc(String deviceId, Pageable pageable);
}

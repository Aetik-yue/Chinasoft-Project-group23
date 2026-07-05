package com.chinasoft.smokesensor.repository;

import com.chinasoft.smokesensor.entity.HumidityData;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 湿度历史数据表 humidity_data 的数据库访问层。
 */
public interface HumidityDataRepository extends JpaRepository<HumidityData, Long>,
        JpaSpecificationExecutor<HumidityData> {

    /**
     * 查询指定设备最新一条湿度数据。
     */
    Optional<HumidityData> findTopByDeviceIdOrderByRecordTimeDesc(String deviceId);

    /**
     * 查询指定设备全部历史湿度数据，按采集时间倒序。
     */
    List<HumidityData> findByDeviceIdOrderByRecordTimeDesc(String deviceId);

    /**
     * 分页查询指定设备历史湿度数据，按采集时间倒序。
     */
    Page<HumidityData> findByDeviceIdOrderByRecordTimeDesc(String deviceId, Pageable pageable);
}
package com.chinasoft.smokesensor.repository;

import com.chinasoft.smokesensor.entity.SensorData;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 烟雾历史数据表 smoke_data 的数据库访问层。
 */
public interface SensorDataRepository extends JpaRepository<SensorData, Long>, JpaSpecificationExecutor<SensorData> {

    /**
     * 按入库时间查询指定设备最新一条真实烟雾数据。
     * source 为空用于兼容早期未填写数据来源的硬件记录。
     */
    @Query(value = """
            SELECT *
            FROM smoke_data
            WHERE device_id = :deviceId
              AND (source = 'sensor' OR source IS NULL)
            ORDER BY created_at DESC, id DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<SensorData> findLatestRealDataByDeviceId(@Param("deviceId") String deviceId);

    /**
     * 按入库时间查询所有设备中最新一条真实烟雾数据。
     */
    @Query(value = """
            SELECT *
            FROM smoke_data
            WHERE source = 'sensor' OR source IS NULL
            ORDER BY created_at DESC, id DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<SensorData> findLatestRealData();

    /**
     * 统计阈值时间之后仍有真实烟雾数据入库的已登记设备数量。
     */
    @Query(value = """
            SELECT COUNT(DISTINCT sd.device_id)
            FROM smoke_data sd
            INNER JOIN smoke_device d ON d.device_id = sd.device_id
            WHERE (sd.source = 'sensor' OR sd.source IS NULL)
              AND sd.created_at >= :thresholdTime
            """, nativeQuery = true)
    long countOnlineDevices(@Param("thresholdTime") LocalDateTime thresholdTime);

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

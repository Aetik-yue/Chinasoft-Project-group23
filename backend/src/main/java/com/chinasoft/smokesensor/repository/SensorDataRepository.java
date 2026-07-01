package com.chinasoft.smokesensor.repository;

import com.chinasoft.smokesensor.entity.SensorData;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SensorDataRepository extends JpaRepository<SensorData, Long>, JpaSpecificationExecutor<SensorData> {

    Optional<SensorData> findTopByDeviceIdOrderByRecordTimeDesc(String deviceId);

    Optional<SensorData> findTopByOrderByRecordTimeDesc();

    List<SensorData> findByDeviceIdOrderByRecordTimeDesc(String deviceId);

    Page<SensorData> findByDeviceIdOrderByRecordTimeDesc(String deviceId, Pageable pageable);
}

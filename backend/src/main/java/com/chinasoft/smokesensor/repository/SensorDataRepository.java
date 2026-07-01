package com.chinasoft.smokesensor.repository;

import com.chinasoft.smokesensor.entity.SensorData;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensorDataRepository extends JpaRepository<SensorData, Long> {

    Optional<SensorData> findTopByDeviceIdOrderByRecordTimeDesc(String deviceId);

    List<SensorData> findByDeviceIdOrderByRecordTimeDesc(String deviceId);

    Page<SensorData> findByDeviceIdOrderByRecordTimeDesc(String deviceId, Pageable pageable);
}

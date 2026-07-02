package com.chinasoft.smokesensor.repository;

import com.chinasoft.smokesensor.entity.Device;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByDeviceId(String deviceId);

    boolean existsByDeviceId(String deviceId);

    long countByOnlineTrue();

    long countByLastHeartbeatGreaterThanEqual(LocalDateTime thresholdTime);

    Optional<Device> findTopByOrderByUpdatedAtDesc();
}

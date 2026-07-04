package com.chinasoft.smokesensor.repository;

import com.chinasoft.smokesensor.entity.DeviceControl;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceControlRepository extends JpaRepository<DeviceControl, Long> {

    Optional<DeviceControl> findByDeviceIdAndControlType(String deviceId, String controlType);
}

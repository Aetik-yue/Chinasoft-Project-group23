package com.chinasoft.smokesensor.repository;

import com.chinasoft.smokesensor.entity.DeviceControl;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 设备控制表 device_control 的数据库访问层。
 */
public interface DeviceControlRepository extends JpaRepository<DeviceControl, Long> {

    /**
     * 查询指定设备的某个控制对象状态，例如 switch、buzzer、alarm_light。
     */
    Optional<DeviceControl> findByDeviceIdAndControlType(String deviceId, String controlType);
}

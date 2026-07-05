package com.chinasoft.smokesensor.repository;

import com.chinasoft.smokesensor.entity.Device;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 设备表 smoke_device 的数据库访问层。
 */
public interface DeviceRepository extends JpaRepository<Device, Long>, JpaSpecificationExecutor<Device> {

    /**
     * 按设备业务编号查询设备。
     */
    Optional<Device> findByDeviceId(String deviceId);

    /**
     * 判断设备业务编号是否已存在，新增设备和上传数据校验时使用。
     */
    boolean existsByDeviceId(String deviceId);

    /**
     * 按 online 字段统计在线设备数，保留给兼容场景使用。
     */
    long countByOnlineTrue();

    /**
     * 查询最近更新的设备，deviceId 为空时作为默认设备使用。
     */
    Optional<Device> findTopByOrderByUpdatedAtDesc();
}

package com.chinasoft.smokesensor.repository;

import com.chinasoft.smokesensor.entity.AlarmRecord;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AlarmRecordRepository extends JpaRepository<AlarmRecord, Long>, JpaSpecificationExecutor<AlarmRecord> {

    // 查询某个设备所有告警记录 并按照触发时间倒序排序
    List<AlarmRecord> findByDeviceIdOrderByTriggeredAtDesc(String deviceId);

    // 查询所有告警记录 并按照触发时间倒序排序     
    Page<AlarmRecord> findAllByOrderByTriggeredAtDesc(Pageable pageable);

    // 查询某个时间段内的告警记录数量
    long countByTriggeredAtGreaterThanEqualAndTriggeredAtLessThan(LocalDateTime start, LocalDateTime end);

    // 查询某个设备处于指定状态集合里的告警记录
    List<AlarmRecord> findByDeviceIdAndStatusIn(String deviceId, Collection<String> statuses);
}

package com.chinasoft.smokesensor.repository;

import com.chinasoft.smokesensor.entity.AlarmRecord;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 告警记录表 alarm_record 的数据库访问层。
 */
public interface AlarmRecordRepository extends JpaRepository<AlarmRecord, Long>, JpaSpecificationExecutor<AlarmRecord> {

    /**
     * 查询指定设备的全部告警记录，按触发时间倒序。
     */
    List<AlarmRecord> findByDeviceIdOrderByTriggeredAtDesc(String deviceId);

    /**
     * 分页查询全部告警记录，按触发时间倒序。
     */
    Page<AlarmRecord> findAllByOrderByTriggeredAtDesc(Pageable pageable);

    /**
     * 统计指定时间段内的告警数量，今日告警统计接口使用。
     */
    long countByTriggeredAtGreaterThanEqualAndTriggeredAtLessThan(LocalDateTime start, LocalDateTime end);

    /**
     * 查询指定设备处于某些状态的告警，恢复正常时批量解除未处理告警。
     */
    List<AlarmRecord> findByDeviceIdAndStatusIn(String deviceId, Collection<String> statuses);

    /**
     * 按业务告警编号查询告警，告警处理和视觉复核通过 alarmId 关联告警。
     */
    Optional<AlarmRecord> findByAlarmId(String alarmId);

    List<AlarmRecord> findByUserIdAndDeviceIdAndAlarmTypeAndStatusIn(
            Long userId, String deviceId, String alarmType, Collection<String> statuses);

    List<AlarmRecord> findByUserIdOrderByTriggeredAtDesc(Long userId);
}

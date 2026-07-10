package com.chinasoft.smokesensor.repository;

import com.chinasoft.smokesensor.entity.ParrotBehaviorRecord;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParrotBehaviorRecordRepository extends JpaRepository<ParrotBehaviorRecord, Long> {

    /** 某设备最近 N 条行为记录（按时间倒序）。 */
    List<ParrotBehaviorRecord> findByDeviceIdOrderByCheckedAtDesc(String deviceId, Pageable pageable);

    /** 某设备在指定时间范围内的行为记录（按时间升序），用于今日统计。 */
    List<ParrotBehaviorRecord> findByDeviceIdAndCheckedAtBetweenOrderByCheckedAtAsc(
            String deviceId, LocalDateTime start, LocalDateTime end);
}

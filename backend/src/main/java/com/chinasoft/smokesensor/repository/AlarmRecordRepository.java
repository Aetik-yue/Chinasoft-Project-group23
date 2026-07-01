package com.chinasoft.smokesensor.repository;

import com.chinasoft.smokesensor.entity.AlarmRecord;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AlarmRecordRepository extends JpaRepository<AlarmRecord, Long>, JpaSpecificationExecutor<AlarmRecord> {

    List<AlarmRecord> findByDeviceIdOrderByTriggeredAtDesc(String deviceId);

    Page<AlarmRecord> findAllByOrderByTriggeredAtDesc(Pageable pageable);

    long countByTriggeredAtGreaterThanEqualAndTriggeredAtLessThan(LocalDateTime start, LocalDateTime end);
}

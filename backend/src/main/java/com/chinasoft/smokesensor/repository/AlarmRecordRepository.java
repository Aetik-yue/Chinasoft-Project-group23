package com.chinasoft.smokesensor.repository;

import com.chinasoft.smokesensor.entity.AlarmRecord;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlarmRecordRepository extends JpaRepository<AlarmRecord, Long> {

    List<AlarmRecord> findByDeviceIdOrderByTriggeredAtDesc(String deviceId);

    Page<AlarmRecord> findAllByOrderByTriggeredAtDesc(Pageable pageable);
}

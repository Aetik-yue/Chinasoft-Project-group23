package com.chinasoft.smokesensor.repository;

import com.chinasoft.smokesensor.entity.VisionCheck;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisionCheckRepository extends JpaRepository<VisionCheck, Long> {

    /** 按告警编号查询复核记录（同一告警幂等，不重复复核）。 */
    Optional<VisionCheck> findByAlarmId(String alarmId);

    boolean existsByAlarmId(String alarmId);
}

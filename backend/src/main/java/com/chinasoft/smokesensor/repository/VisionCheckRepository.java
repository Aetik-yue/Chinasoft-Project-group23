package com.chinasoft.smokesensor.repository;

import com.chinasoft.smokesensor.entity.VisionCheck;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 视觉复核表 vision_check 的数据库访问层。
 */
public interface VisionCheckRepository extends JpaRepository<VisionCheck, Long> {

    /**
     * 按告警编号查询复核记录，同一告警已复核时直接返回已有结果。
     */
    Optional<VisionCheck> findByAlarmId(String alarmId);

    /**
     * 判断某个告警是否已经存在视觉复核记录。
     */
    boolean existsByAlarmId(String alarmId);
}

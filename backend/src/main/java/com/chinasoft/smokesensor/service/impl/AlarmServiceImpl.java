package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.dto.AlarmLogResponse;
import com.chinasoft.smokesensor.dto.AlarmRecordResponse;
import com.chinasoft.smokesensor.dto.AlarmTodayStatResponse;
import com.chinasoft.smokesensor.entity.AlarmRecord;
import com.chinasoft.smokesensor.repository.AlarmRecordRepository;
import com.chinasoft.smokesensor.service.AlarmService;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlarmServiceImpl implements AlarmService {

    private final AlarmRecordRepository alarmRecordRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AlarmRecordResponse> getAlarmList(int limit) {
        int pageSize = Math.max(limit, 1);

        return alarmRecordRepository.findAllByOrderByTriggeredAtDesc(PageRequest.of(0, pageSize))
                .stream()
                .map(this::toAlarmRecordResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlarmRecordResponse> getAlarmsByDeviceId(String deviceId) {
        return alarmRecordRepository.findByDeviceIdOrderByTriggeredAtDesc(deviceId)
                .stream()
                .map(this::toAlarmRecordResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AlarmTodayStatResponse getTodayStat() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();

        long todayCount = alarmRecordRepository.countByTriggeredAtGreaterThanEqualAndTriggeredAtLessThan(
                todayStart, tomorrowStart);
        long yesterdayCount = alarmRecordRepository.countByTriggeredAtGreaterThanEqualAndTriggeredAtLessThan(
                yesterdayStart, todayStart);

        double changeRate = yesterdayCount == 0
                ? (todayCount == 0 ? 0.0 : 100.0)
                : (todayCount - yesterdayCount) * 100.0 / yesterdayCount;

        return AlarmTodayStatResponse.builder()
                .todayCount(todayCount)
                .yesterdayCount(yesterdayCount)
                .changeRate(changeRate)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlarmLogResponse> getAlarmLogs(
            Integer limit,
            Integer page,
            Integer pageSize,
            String deviceId,
            String status,
            String level,
            LocalDateTime start,
            LocalDateTime end) {
        int resolvedPage = page == null || page <= 1 ? 0 : page - 1;
        int resolvedPageSize = resolvePageSize(limit, pageSize);

        Specification<AlarmRecord> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (deviceId != null && !deviceId.isBlank()) {
                predicates.add(cb.equal(root.get("deviceId"), deviceId));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (level != null && !level.isBlank()) {
                predicates.add(cb.equal(root.get("riskLevel"), level));
            }
            if (start != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("triggeredAt"), start));
            }
            if (end != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<LocalDateTime>get("triggeredAt"), end));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return alarmRecordRepository.findAll(
                        specification,
                        PageRequest.of(resolvedPage, resolvedPageSize, Sort.by(Sort.Direction.DESC, "triggeredAt")))
                .stream()
                .map(this::toAlarmLogResponse)
                .toList();
    }

    private AlarmRecordResponse toAlarmRecordResponse(AlarmRecord alarmRecord) {
        return AlarmRecordResponse.builder()
                .alarmId(alarmRecord.getAlarmId())
                .deviceId(alarmRecord.getDeviceId())
                .alarmType(alarmRecord.getAlarmType())
                .smokeValue(alarmRecord.getSmokeValue())
                .riskLevel(alarmRecord.getRiskLevel())
                .status(alarmRecord.getStatus())
                .handler(alarmRecord.getHandler())
                .handledAt(alarmRecord.getHandledAt())
                .remark(alarmRecord.getRemark())
                .triggeredAt(alarmRecord.getTriggeredAt())
                .resolvedAt(alarmRecord.getResolvedAt())
                .isSimulated(alarmRecord.getIsSimulated())
                .createdAt(alarmRecord.getCreatedAt())
                .updatedAt(alarmRecord.getUpdatedAt())
                .build();
    }

    private AlarmLogResponse toAlarmLogResponse(AlarmRecord alarmRecord) {
        return AlarmLogResponse.builder()
                .alarmId(alarmRecord.getAlarmId())
                .alarmTime(alarmRecord.getTriggeredAt())
                .deviceId(alarmRecord.getDeviceId())
                .type(alarmRecord.getAlarmType())
                .value(alarmRecord.getSmokeValue())
                .level(alarmRecord.getRiskLevel())
                .status(alarmRecord.getStatus())
                .handler(alarmRecord.getHandler())
                .remark(alarmRecord.getRemark())
                .build();
    }

    private int resolvePageSize(Integer limit, Integer pageSize) {
        Integer value = pageSize != null ? pageSize : limit;
        if (value == null) {
            return 50;
        }
        return Math.max(value, 1);
    }
}

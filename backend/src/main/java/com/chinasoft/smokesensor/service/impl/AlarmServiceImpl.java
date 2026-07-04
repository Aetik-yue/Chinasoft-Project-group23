package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.common.BusinessException;
import com.chinasoft.smokesensor.dto.AlarmHandleRequest;
import com.chinasoft.smokesensor.dto.AlarmHandleResponse;
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

/**
 * 告警查询和告警处理业务实现。
 *
 * <p>该类主要操作 alarm_record，提供今日统计、日志筛选、设备告警列表，
 * 以及人工处理告警能力。
 */
@Service
@RequiredArgsConstructor
public class AlarmServiceImpl implements AlarmService {

    private static final String STATUS_RESOLVED = "resolved";
    private static final String HANDLE_MESSAGE = "告警已处理";

    private final AlarmRecordRepository alarmRecordRepository;

    /**
     * 查询告警列表。
     *
     * <p>按 triggeredAt 倒序读取最近 limit 条，用于兼容早期告警查询接口。
     */
    @Override
    @Transactional(readOnly = true)
    public List<AlarmRecordResponse> getAlarmList(int limit) {
        int pageSize = Math.max(limit, 1);

        return alarmRecordRepository.findAllByOrderByTriggeredAtDesc(PageRequest.of(0, pageSize))
                .stream()
                .map(this::toAlarmRecordResponse)
                .toList();
    }

    /**
     * 查询指定设备的告警记录。
     *
     * <p>按 triggeredAt 倒序返回，用于查看单个设备历史告警。
     */
    @Override
    @Transactional(readOnly = true)
    public List<AlarmRecordResponse> getAlarmsByDeviceId(String deviceId) {
        return alarmRecordRepository.findByDeviceIdOrderByTriggeredAtDesc(deviceId)
                .stream()
                .map(this::toAlarmRecordResponse)
                .toList();
    }

    /**
     * 查询今日告警统计。
     *
     * <p>统计今天和昨天的 alarm_record 数量，并计算变化率。
     */
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

    /**
     * 查询告警日志。
     *
     * <p>支持分页、设备编号、处理状态、风险等级、起止时间筛选，
     * 查询结果按 triggeredAt 倒序返回给前端日志表格。
     */
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

    /**
     * 人工处理告警。
     *
     * <p>处理流程：
     * 1. 根据 alarmId 查询 alarm_record；
     * 2. 将状态更新为 resolved；
     * 3. 写入 handler、handledAt、resolvedAt、remark 和 updatedAt。
     */
    @Override
    @Transactional
    public AlarmHandleResponse handleAlarm(AlarmHandleRequest request) {
        LocalDateTime handledTime = LocalDateTime.now();
        AlarmRecord alarmRecord = alarmRecordRepository.findByAlarmId(request.getAlarmId())
                .orElseThrow(() -> BusinessException.notFound("告警不存在: " + request.getAlarmId()));

        alarmRecord.setStatus(STATUS_RESOLVED);
        alarmRecord.setHandler(request.getHandler());
        alarmRecord.setHandledAt(handledTime);
        alarmRecord.setResolvedAt(handledTime);
        alarmRecord.setRemark(request.getRemark());
        alarmRecord.setUpdatedAt(handledTime);
        alarmRecordRepository.save(alarmRecord);

        return AlarmHandleResponse.builder()
                .alarmId(alarmRecord.getAlarmId())
                .deviceId(alarmRecord.getDeviceId())
                .status(alarmRecord.getStatus())
                .handler(alarmRecord.getHandler())
                .handledAt(alarmRecord.getHandledAt())
                .resolvedAt(alarmRecord.getResolvedAt())
                .remark(alarmRecord.getRemark())
                .message(HANDLE_MESSAGE)
                .build();
    }

    /**
     * 将告警实体转换为通用告警记录响应。
     */
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

    /**
     * 将告警实体转换为前端告警日志表格响应。
     */
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

    /**
     * 兼容 limit 和 pageSize 两种分页参数，统一解析每页条数。
     */
    private int resolvePageSize(Integer limit, Integer pageSize) {
        Integer value = pageSize != null ? pageSize : limit;
        if (value == null) {
            return 50;
        }
        return Math.max(value, 1);
    }
}

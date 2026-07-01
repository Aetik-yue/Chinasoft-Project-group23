package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.dto.AlarmRecordResponse;
import com.chinasoft.smokesensor.entity.AlarmRecord;
import com.chinasoft.smokesensor.repository.AlarmRecordRepository;
import com.chinasoft.smokesensor.service.AlarmService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
}

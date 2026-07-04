package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.dto.AlarmLogResponse;
import com.chinasoft.smokesensor.dto.AlarmHandleRequest;
import com.chinasoft.smokesensor.dto.AlarmHandleResponse;
import com.chinasoft.smokesensor.dto.AlarmRecordResponse;
import com.chinasoft.smokesensor.dto.AlarmTodayStatResponse;
import java.time.LocalDateTime;
import java.util.List;

public interface AlarmService {

    List<AlarmRecordResponse> getAlarmList(int limit);

    List<AlarmRecordResponse> getAlarmsByDeviceId(String deviceId);

    AlarmTodayStatResponse getTodayStat();

    List<AlarmLogResponse> getAlarmLogs(
            Integer limit,
            Integer page,
            Integer pageSize,
            String deviceId,
            String status,
            String level,
            LocalDateTime start,
            LocalDateTime end);

    AlarmHandleResponse handleAlarm(AlarmHandleRequest request);
}

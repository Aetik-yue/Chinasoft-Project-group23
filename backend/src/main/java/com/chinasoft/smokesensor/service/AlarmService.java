package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.dto.AlarmLogResponse;
import com.chinasoft.smokesensor.dto.AlarmHandleRequest;
import com.chinasoft.smokesensor.dto.AlarmHandleResponse;
import com.chinasoft.smokesensor.dto.AlarmRecordResponse;
import com.chinasoft.smokesensor.dto.AlarmTodayStatResponse;
import java.time.LocalDateTime;
import java.util.List;

public interface AlarmService {

    /**
     * 查询全部告警记录，按告警时间倒序限制返回数量。
     */
    List<AlarmRecordResponse> getAlarmList(int limit);

    /**
     * 查询指定设备的告警记录。
     */
    List<AlarmRecordResponse> getAlarmsByDeviceId(String deviceId);

    /**
     * 查询今日告警统计数据。
     */
    AlarmTodayStatResponse getTodayStat();

    /**
     * 查询告警日志，支持分页、设备、状态、等级和时间范围筛选。
     */
    List<AlarmLogResponse> getAlarmLogs(
            Integer limit,
            Integer page,
            Integer pageSize,
            String deviceId,
            String status,
            String level,
            LocalDateTime start,
            LocalDateTime end);

    /**
     * 处理告警，将指定告警更新为已处理状态并记录处理信息。
     */
    AlarmHandleResponse handleAlarm(AlarmHandleRequest request);
}

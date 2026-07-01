package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.dto.AlarmRecordResponse;
import java.util.List;

public interface AlarmService {

    List<AlarmRecordResponse> getAlarmList(int limit);

    List<AlarmRecordResponse> getAlarmsByDeviceId(String deviceId);
}

package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.dto.SmokeHistoryPointResponse;
import com.chinasoft.smokesensor.dto.SmokeLatestResponse;
import java.time.LocalDateTime;
import java.util.List;

public interface SmokeService {

    SmokeLatestResponse getLatestSmoke(String deviceId);

    List<SmokeHistoryPointResponse> getHistory(String deviceId, String range, LocalDateTime start, LocalDateTime end);
}

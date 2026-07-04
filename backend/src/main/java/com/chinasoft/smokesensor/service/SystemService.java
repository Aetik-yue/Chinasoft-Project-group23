package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.dto.SystemStatusResponse;

public interface SystemService {

    /**
     * 查询系统整体运行状态。
     */
    SystemStatusResponse getSystemStatus();
}

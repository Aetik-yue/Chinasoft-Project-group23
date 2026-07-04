package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.dto.RuntimeLinkSnapshotResponse;

public interface RuntimeService {

    /**
     * 查询设备连接运行态快照。
     */
    RuntimeLinkSnapshotResponse getLinkSnapshot(String deviceId);
}

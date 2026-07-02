package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.dto.RuntimeLinkSnapshotResponse;

public interface RuntimeService {

    RuntimeLinkSnapshotResponse getLinkSnapshot(String deviceId);
}

package com.chinasoft.smokesensor.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuntimeLinkSnapshotResponse {

    private String linkState;

    private Boolean hardwareOnline;

    private Boolean mqttOnline;

    private LocalDateTime lastSeenAt;

    private String offlineReason;

    private String displayMode;
}

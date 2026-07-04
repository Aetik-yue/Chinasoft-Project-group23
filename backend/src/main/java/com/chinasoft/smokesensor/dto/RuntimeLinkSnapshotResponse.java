package com.chinasoft.smokesensor.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 运行态连接快照响应 DTO。
 *
 * 用于前端页面初始化时判断硬件连接状态和展示模式。
 */
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

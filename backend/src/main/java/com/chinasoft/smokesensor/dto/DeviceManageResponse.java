package com.chinasoft.smokesensor.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备管理响应 DTO。
 *
 * 用于设备列表、新增、编辑接口返回设备基础信息和当前状态。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceManageResponse {

    private String deviceId;

    private String name;

    private String location;

    private Boolean online;

    private LocalDateTime lastHeartbeat;

    private Integer currentSmokeValue;

    private String currentRiskLevel;

    private String currentAlarmStatus;

    private Boolean enabled;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

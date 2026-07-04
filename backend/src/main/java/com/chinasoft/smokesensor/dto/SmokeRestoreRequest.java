package com.chinasoft.smokesensor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模拟恢复正常请求 DTO。
 *
 * 用于 /api/smoke/restore，将指定设备恢复到安全烟雾值。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmokeRestoreRequest {

    private String deviceId;

    private String scenario;
}

package com.chinasoft.smokesensor.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户偏好响应对象，字段直接对齐前端设置页的 systemPrefs 与开关状态。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferencesResponse {

    private Long userId;

    private String language;

    private String theme;

    private String fontFamily;

    private Integer fontSize;

    private String fontColor;

    private Boolean notificationEnabled;

    private Boolean permissionEnabled;

    private String avatarParrotId;

    private LocalDateTime updatedAt;
}

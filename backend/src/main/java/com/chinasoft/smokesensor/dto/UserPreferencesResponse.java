package com.chinasoft.smokesensor.dto;

import java.time.LocalDateTime;
import java.util.Map;
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

    /** 当前用户各宠物选择的成长相册头像。 */
    private Map<String, String> petAvatarMediaMap;

    /** 当前用户的环境告警范围，单位依次为 ℃、%RH、ppm。 */
    private Double temperatureLower;
    private Double temperatureUpper;
    private Double humidityLower;
    private Double humidityUpper;
    private Double dustLower;
    private Double dustUpper;

    private LocalDateTime updatedAt;
}

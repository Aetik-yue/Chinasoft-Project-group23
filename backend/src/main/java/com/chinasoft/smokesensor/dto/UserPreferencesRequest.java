package com.chinasoft.smokesensor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

/**
 * 用户偏好保存请求。
 *
 * <p>所有字段都允许为空：前端可只提交发生变化的字段，未提交字段保留数据库原值；
 * 若数据库尚无对应记录，则读取响应时使用业务默认值兜底。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferencesRequest {

    private String language;

    private String theme;

    private String fontFamily;

    private Integer fontSize;

    private String fontColor;

    private Boolean notificationEnabled;

    private Boolean permissionEnabled;

    private String avatarParrotId;

    /** 宠物 ID 到成长相册媒体 ID 的映射；仅保存照片引用，不复制图片内容。 */
    private Map<String, String> petAvatarMediaMap;

    /** 用户个人环境告警范围；三个指标均使用下界/上界成对提交。 */
    private Double temperatureLower;
    private Double temperatureUpper;
    private Double humidityLower;
    private Double humidityUpper;
    private Double dustLower;
    private Double dustUpper;
}

package com.chinasoft.smokesensor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 当前登录用户修改账号资料时提交的字段。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 64, message = "用户名长度需为 3-64 位")
    private String username;

    @Pattern(regexp = "^\\s*$|^\\s*\\d{11}\\s*$", message = "手机号必须为 11 位数字")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过 100 位")
    private String email;

    @Size(max = 255, message = "位置信息长度不能超过 255 位")
    private String location;

    /** 头像 base64 data URI，可选；为空表示不修改。 */
    private String avatarImage;
}

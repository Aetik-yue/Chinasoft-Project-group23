package com.chinasoft.smokesensor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 注册请求体。
 *
 * 账号对应 sys_user.username，注册成功后默认授予 viewer 角色。
 * 手机号选填，填写后可用于短信验证码登录。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "账号不能为空")
    @Size(min = 3, max = 64, message = "账号长度需为 3-64 位")
    private String account;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需为 6-64 位")
    private String password;

    private String phone;

    /** Service 层按用户名注册，语义上 account 即用户名。 */
    public String getUsername() {
        return account;
    }
}

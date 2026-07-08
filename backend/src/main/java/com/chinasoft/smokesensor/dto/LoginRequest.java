package com.chinasoft.smokesensor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录请求体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    // 前端 / 文档约定字段名为 account（对应用户名）
    @NotBlank(message = "账号不能为空")
    private String account;

    @NotBlank(message = "密码不能为空")
    private String password;

    // Spring 需要通过 getUsername 语义读取账号（保持 Service 层调用一致）
    public String getUsername() {
        return account;
    }
}

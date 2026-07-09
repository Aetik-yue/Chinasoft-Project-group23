package com.chinasoft.smokesensor.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录成功响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;

    private Long userId;

    private String userRole;

    private String username;

    private String realName;

    private String phone;

    private String email;

    private String location;

    private LocalDateTime expiresAt;
}

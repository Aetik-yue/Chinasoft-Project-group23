package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.dto.LoginRequest;
import com.chinasoft.smokesensor.dto.LoginResponse;
import com.chinasoft.smokesensor.dto.RegisterRequest;

public interface AuthService {

    /**
     * 校验账号密码并返回登录凭证。
     */
    LoginResponse login(LoginRequest request);

    /**
     * 发送短信验证码，返回给前端的提示信息（含有效期）。
     */
    SmsCodeResult sendSmsCode(String phone);

    /**
     * 手机验证码登录，校验成功后返回登录凭证。
     */
    LoginResponse smsLogin(String phone, String code);

    /**
     * 注册新账号，成功后返回登录凭证（可直接进入系统）。
     */
    LoginResponse register(RegisterRequest request);

    /**
     * 解析 token 中的用户 ID，返回当前用户的完整资料（手机/邮箱/角色等）。
     */
    LoginResponse me(String token);

    /**
     * 从 token 中解析当前用户 ID；token 为空或无效时返回 null（不抛异常）。
     *
     * <p>供全局拦截器在未登录场景下安全调用，避免用异常控制流程。
     */
    Long resolveUserIdFromToken(String token);

    /**
     * 短信验证码发送结果。
     */
    record SmsCodeResult(int expiresIn) {}
}

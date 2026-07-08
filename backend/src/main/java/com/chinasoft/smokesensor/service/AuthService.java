package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.dto.LoginRequest;
import com.chinasoft.smokesensor.dto.LoginResponse;

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
     * 短信验证码发送结果。
     */
    record SmsCodeResult(int expiresIn) {}
}

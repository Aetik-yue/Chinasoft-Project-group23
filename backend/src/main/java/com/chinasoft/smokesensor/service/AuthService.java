package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.dto.LoginRequest;
import com.chinasoft.smokesensor.dto.LoginResponse;

public interface AuthService {

    /**
     * 校验账号密码并返回登录凭证。
     */
    LoginResponse login(LoginRequest request);
}

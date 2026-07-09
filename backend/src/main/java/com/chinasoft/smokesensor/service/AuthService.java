package com.chinasoft.smokesensor.service;

import com.chinasoft.smokesensor.dto.ChangePasswordRequest;
import com.chinasoft.smokesensor.dto.LoginRequest;
import com.chinasoft.smokesensor.dto.LoginResponse;
import com.chinasoft.smokesensor.dto.RegisterRequest;
import com.chinasoft.smokesensor.dto.UserProfileUpdateRequest;

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
     * 更新当前用户的账号资料，用户名变化时同时改变后续登录账号。
     */
    LoginResponse updateProfile(Long userId, UserProfileUpdateRequest request);

    /**
     * 从 token 中解析当前用户 ID；token 为空或无效时返回 null（不抛异常）。
     *
     * <p>供全局拦截器在未登录场景下安全调用，避免用异常控制流程。
     */
    Long resolveUserIdFromToken(String token);

    /**
     * 注销当前用户账号：级联删除用户偏好、宠物档案及其关联记录，最后删除用户本身。
     */
    void deleteAccount(Long userId);

    /**
     * 修改当前用户密码：校验通过后写入新密码。
     */
    void changePassword(Long userId, ChangePasswordRequest request);

    /**
     * 短信验证码发送结果。
     */
    record SmsCodeResult(int expiresIn) {}
}

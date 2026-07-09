package com.chinasoft.smokesensor.controller;

import com.chinasoft.smokesensor.common.ApiResult;
import com.chinasoft.smokesensor.dto.ChangePasswordRequest;
import com.chinasoft.smokesensor.dto.LoginRequest;
import com.chinasoft.smokesensor.dto.RegisterRequest;
import com.chinasoft.smokesensor.dto.UserProfileUpdateRequest;
import com.chinasoft.smokesensor.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 账号密码登录：校验 sys_user 中的账号密码，返回前端保存的登录 token。
     */
    @PostMapping("/login")
    public ApiResult login(@Valid @RequestBody LoginRequest request) {
        return ApiResult.ok(authService.login(request));
    }

    /**
     * 发送短信验证码：生成 6 位验证码并缓存，供后续 sms-login 校验。
     */
    @PostMapping("/sms-code")
    public ApiResult sendSmsCode(@RequestBody @Valid SmsRequest request) {
        return ApiResult.ok(authService.sendSmsCode(request.phone()));
    }

    /**
     * 手机验证码登录：校验短信验证码，校验通过后返回登录 token。
     */
    @PostMapping("/sms-login")
    public ApiResult smsLogin(@RequestBody @Valid SmsLoginRequest request) {
        return ApiResult.ok(authService.smsLogin(request.phone(), request.code()));
    }

    /**
     * 注册：创建新账号，成功后直接返回登录凭证。
     */
    @PostMapping("/register")
    public ApiResult register(@RequestBody @Valid RegisterRequest request) {
        return ApiResult.ok(authService.register(request));
    }

    /**
     * 获取当前登录用户的资料：从请求头解析 token，返回手机/邮箱/角色等完整信息。
     */
    @GetMapping("/me")
    public ApiResult me(jakarta.servlet.http.HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new com.chinasoft.smokesensor.common.BusinessException(2001, "未登录或登录已过期",
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
        return ApiResult.ok(authService.me(header.substring(7)));
    }

    /** 更新当前登录用户的用户名、手机号、邮箱和位置信息。 */
    @PutMapping("/me")
    public ApiResult updateMe(@Valid @RequestBody UserProfileUpdateRequest updateRequest,
                              jakarta.servlet.http.HttpServletRequest httpRequest) {
        String header = httpRequest.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new com.chinasoft.smokesensor.common.BusinessException(2001, "未登录或登录已过期",
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
        String token = header.substring(7);
        Long userId = authService.resolveUserIdFromToken(token);
        if (userId == null) {
            throw new com.chinasoft.smokesensor.common.BusinessException(2001, "登录凭证无效或已过期",
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
        var response = authService.updateProfile(userId, updateRequest);
        response.setToken(token);
        return ApiResult.ok(response);
    }

    /**
     * 修改当前登录用户的密码：校验通过后写入新密码。
     */
    @PostMapping("/change-password")
    public ApiResult changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                    jakarta.servlet.http.HttpServletRequest httpRequest) {
        String header = httpRequest.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new com.chinasoft.smokesensor.common.BusinessException(2001, "未登录或登录已过期",
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
        Long userId = authService.resolveUserIdFromToken(header.substring(7));
        if (userId == null) {
            throw new com.chinasoft.smokesensor.common.BusinessException(2001, "登录凭证无效或已过期",
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
        authService.changePassword(userId, request);
        return ApiResult.ok("密码修改成功");
    }

    /**
     * 注销当前登录账号：从请求头解析 token，级联删除用户数据。
     */
    @DeleteMapping("/account")
    public ApiResult deleteAccount(jakarta.servlet.http.HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new com.chinasoft.smokesensor.common.BusinessException(2001, "未登录或登录已过期",
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
        Long userId = authService.resolveUserIdFromToken(header.substring(7));
        if (userId == null) {
            throw new com.chinasoft.smokesensor.common.BusinessException(2001, "登录凭证无效或已过期",
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
        authService.deleteAccount(userId);
        return ApiResult.ok("账号已注销");
    }

    /** 短信验证码请求：只需要手机号。 */
    public record SmsRequest(@NotBlank(message = "手机号不能为空") String phone) {}

    /** 短信登录请求：手机号 + 验证码。 */
    public record SmsLoginRequest(@NotBlank(message = "手机号不能为空") String phone,
                                  @NotBlank(message = "验证码不能为空") String code) {}
}

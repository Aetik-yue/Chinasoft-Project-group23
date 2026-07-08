package com.chinasoft.smokesensor.controller;

import com.chinasoft.smokesensor.common.ApiResult;
import com.chinasoft.smokesensor.dto.LoginRequest;
import com.chinasoft.smokesensor.dto.RegisterRequest;
import com.chinasoft.smokesensor.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

    /** 短信验证码请求：只需要手机号。 */
    public record SmsRequest(@NotBlank(message = "手机号不能为空") String phone) {}

    /** 短信登录请求：手机号 + 验证码。 */
    public record SmsLoginRequest(@NotBlank(message = "手机号不能为空") String phone,
                                  @NotBlank(message = "验证码不能为空") String code) {}
}

package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.common.BusinessException;
import com.chinasoft.smokesensor.dto.LoginRequest;
import com.chinasoft.smokesensor.dto.LoginResponse;
import com.chinasoft.smokesensor.dto.RegisterRequest;
import com.chinasoft.smokesensor.entity.SysUser;
import com.chinasoft.smokesensor.repository.SysUserRepository;
import com.chinasoft.smokesensor.service.AuthService;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 登录业务实现。
 *
 * 当前阶段只负责真实账号登录和返回 token，不启用全局接口拦截，避免影响已有接口测试。
 * 短信验证码使用进程内缓存，仅适合演示环境；生产环境应替换为 Redis + 真实短信网关。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int TOKEN_EXPIRE_HOURS = 8;
    private static final int STATUS_ENABLED = 1;
    private static final int SMS_CODE_EXPIRE_MINUTES = 5;
    private static final int SMS_CODE_LENGTH = 6;

    private final SysUserRepository sysUserRepository;

    // 短信验证码缓存：phone -> code。演示用，重启即失效。
    private final ConcurrentHashMap<String, String> smsCodeCache = new ConcurrentHashMap<>();

    /**
     * 登录流程：查用户 -> 校验状态 -> 校验密码 -> 更新最后登录时间 -> 返回简单 token。
     */
    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername().trim();
        String password = request.getPassword();

        SysUser user = sysUserRepository.findByUsername(username)
                .orElseThrow(() -> BusinessException.unauthorized("账号或密码错误"));

        if (user.getStatus() == null || user.getStatus() != STATUS_ENABLED) {
            throw BusinessException.unauthorized("账号已被禁用");
        }

        // 当前演示数据库中的密码为明文，正式环境应改为 BCrypt 加密校验。
        if (!password.equals(user.getPassword())) {
            throw BusinessException.unauthorized("账号或密码错误");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(TOKEN_EXPIRE_HOURS);
        user.setLastLoginTime(now);
        sysUserRepository.save(user);

        return buildLoginResponse(user, expiresAt);
    }

    @Override
    public SmsCodeResult sendSmsCode(String phone) {
        String code = generateSmsCode();
        smsCodeCache.put(phone, code);
        // 演示环境：把验证码打到日志，方便开发调试；生产环境应通过短信网关下发并移除日志。
        log.info("[sms-code] phone={} code={} (valid {} minutes)", phone, code, SMS_CODE_EXPIRE_MINUTES);
        return new SmsCodeResult(SMS_CODE_EXPIRE_MINUTES * 60);
    }

    @Override
    public LoginResponse smsLogin(String phone, String code) {
        String cached = smsCodeCache.get(phone);
        if (cached == null || !cached.equals(code)) {
            throw BusinessException.unauthorized("验证码错误或已过期");
        }
        // 验证通过后立即移除，防止重复使用。
        smsCodeCache.remove(phone);

        Optional<SysUser> existing = sysUserRepository.findAll().stream()
                .filter(u -> phone.equals(u.getPhone()))
                .findFirst();

        // 演示策略：手机号已注册则直接登录；未注册则拒绝（避免自动批量注册）。
        SysUser user = existing.orElseThrow(() ->
                BusinessException.unauthorized("该手机号未注册，请使用账号密码登录或先注册"));

        if (user.getStatus() == null || user.getStatus() != STATUS_ENABLED) {
            throw BusinessException.unauthorized("账号已被禁用");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(TOKEN_EXPIRE_HOURS);
        user.setLastLoginTime(now);
        sysUserRepository.save(user);

        return buildLoginResponse(user, expiresAt);
    }

    /**
     * 注册流程：校验账号唯一性 -> 创建用户（默认 viewer 角色）-> 返回登录凭证。
     */
    @Override
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        String password = request.getPassword();
        String phone = request.getPhone() == null ? null : request.getPhone().trim();

        if (sysUserRepository.findByUsername(username).isPresent()) {
            throw new BusinessException(1201, "该账号已被注册", HttpStatus.BAD_REQUEST);
        }

        SysUser user = SysUser.builder()
                .username(username)
                .password(password)
                .realName(username)
                .role("viewer")
                .phone(phone.isEmpty() ? null : phone)
                .status(STATUS_ENABLED)
                .build();
        sysUserRepository.save(user);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(TOKEN_EXPIRE_HOURS);
        user.setLastLoginTime(now);
        sysUserRepository.save(user);

        return buildLoginResponse(user, expiresAt);
    }

    /**
     * token 格式：smoke-token-{userId}-{expiresAt}-{uuid}，按 "-" 拆分后第 3 段即用户 ID。
     */
    @Override
    public LoginResponse me(String token) {
        Long userId = resolveUserIdFromToken(token);
        if (userId == null) {
            throw BusinessException.unauthorized("登录凭证无效或已过期");
        }
        SysUser user = sysUserRepository.findById(userId)
                .orElseThrow(() -> BusinessException.unauthorized("登录凭证无效或已过期"));

        return LoginResponse.builder()
                .token(token)
                .userRole(user.getRole())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .build();
    }

    @Override
    public Long resolveUserIdFromToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String[] parts = token.split("-");
        // smoke-token-{id}-{expiresAt}-{uuid}
        if (parts.length < 4 || !"smoke".equals(parts[0]) || !"token".equals(parts[1])) {
            return null;
        }
        try {
            return Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LoginResponse buildLoginResponse(SysUser user, LocalDateTime expiresAt) {
        return LoginResponse.builder()
                .token(buildToken(user, expiresAt))
                .userRole(user.getRole())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .expiresAt(expiresAt)
                .build();
    }

    private String generateSmsCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < SMS_CODE_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * 简单登录凭证，仅用于当前演示阶段；后续接入 JWT 后可替换此方法。
     */
    private String buildToken(SysUser user, LocalDateTime expiresAt) {
        return "smoke-token-" + user.getId() + "-" + expiresAt + "-" + UUID.randomUUID();
    }
}

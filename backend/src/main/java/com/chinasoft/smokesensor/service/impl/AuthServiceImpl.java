package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.common.BusinessException;
import com.chinasoft.smokesensor.dto.LoginRequest;
import com.chinasoft.smokesensor.dto.LoginResponse;
import com.chinasoft.smokesensor.entity.SysUser;
import com.chinasoft.smokesensor.repository.SysUserRepository;
import com.chinasoft.smokesensor.service.AuthService;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 登录业务实现。
 *
 * 当前阶段只负责真实账号登录和返回 token，不启用全局接口拦截，避免影响已有接口测试。
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int TOKEN_EXPIRE_HOURS = 8;
    private static final int STATUS_ENABLED = 1;

    private final SysUserRepository sysUserRepository;

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

        return LoginResponse.builder()
                .token(buildToken(user, expiresAt))
                .userRole(user.getRole())
                .username(user.getUsername())
                .realName(user.getRealName())
                .expiresAt(expiresAt)
                .build();
    }

    /**
     * 简单登录凭证，仅用于当前演示阶段；后续接入 JWT 后可替换此方法。
     */
    private String buildToken(SysUser user, LocalDateTime expiresAt) {
        return "smoke-token-" + user.getId() + "-" + expiresAt + "-" + UUID.randomUUID();
    }
}

package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.common.BusinessException;
import com.chinasoft.smokesensor.dto.ChangePasswordRequest;
import com.chinasoft.smokesensor.dto.LoginRequest;
import com.chinasoft.smokesensor.dto.LoginResponse;
import com.chinasoft.smokesensor.dto.RegisterRequest;
import com.chinasoft.smokesensor.dto.UserProfileUpdateRequest;
import com.chinasoft.smokesensor.entity.PetProfile;
import com.chinasoft.smokesensor.entity.SysUser;
import com.chinasoft.smokesensor.repository.PetLedgerRecordRepository;
import com.chinasoft.smokesensor.repository.PetMediaRecordRepository;
import com.chinasoft.smokesensor.repository.PetMedicalRecordRepository;
import com.chinasoft.smokesensor.repository.PetProfileRepository;
import com.chinasoft.smokesensor.repository.PetWeightRecordRepository;
import com.chinasoft.smokesensor.repository.SysUserRepository;
import com.chinasoft.smokesensor.repository.UserPreferenceRepository;
import com.chinasoft.smokesensor.service.AuthService;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
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
    private final UserPreferenceRepository userPreferenceRepository;
    private final PetProfileRepository petProfileRepository;
    private final PetWeightRecordRepository petWeightRecordRepository;
    private final PetMedicalRecordRepository petMedicalRecordRepository;
    private final PetLedgerRecordRepository petLedgerRecordRepository;
    private final PetMediaRecordRepository petMediaRecordRepository;

    // 短信验证码缓存：phone -> code。演示用，重启即失效。
    private final ConcurrentHashMap<String, String> smsCodeCache = new ConcurrentHashMap<>();

    /**
     * 登录流程：查用户 -> 校验状态 -> 校验密码 -> 更新最后登录时间 -> 返回简单 token。
     *
     * <p>账号字段支持两种身份：用户名或已绑定手机号。先按用户名查，查不到再按手机号查，
     * 这样绑定手机号后就能直接用手机号 + 密码登录。
     */
    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String account = request.getUsername().trim();
        String password = request.getPassword();

        SysUser user = sysUserRepository.findByUsername(account)
                .or(() -> sysUserRepository.findByPhone(account))
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
                .phone(phone == null || phone.isEmpty() ? null : phone)
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

        return buildUserResponse(user, token, null);
    }

    /**
     * 资料更新与当前 token 绑定的用户 ID 对齐；token 本身不包含用户名，
     * 因此修改登录账号后当前会话仍然有效。
     */
    @Override
    @Transactional
    public LoginResponse updateProfile(Long userId, UserProfileUpdateRequest request) {
        SysUser user = sysUserRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));

        String username = requiredProfileText(request.getUsername(), "用户名不能为空");
        if (username.length() < 3 || username.length() > 64) {
            throw new IllegalArgumentException("用户名长度需为 3-64 位");
        }
        sysUserRepository.findByUsername(username)
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new BusinessException(1201, "该用户名已被使用", HttpStatus.BAD_REQUEST);
                });

        user.setUsername(username);
        user.setPhone(trimToNull(request.getPhone()));
        user.setEmail(trimToNull(request.getEmail()));
        user.setLocation(trimToNull(request.getLocation()));
        return buildUserResponse(sysUserRepository.save(user), null, null);
    }

    @Override
    public Long resolveUserIdFromToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String[] parts = token.split("-");
        // smoke-token-{id}-{expiresAt}-{uuid}
        // 注意：expiresAt 是 LocalDateTime 格式（如 2027-07-11T12:00:00），本身含连字符，
        // 因此 split 后有多段。最后一段是 uuid，中间段拼接起来即为 expiresAt。
        if (parts.length < 6 || !"smoke".equals(parts[0]) || !"token".equals(parts[1])) {
            return null;
        }
        // 从 parts[3] 到 parts[length-2] 拼接回日期字符串（排除最后一段 uuid）
        String expiresAtStr = String.join("-", Arrays.copyOfRange(parts, 3, parts.length - 1));
        // 校验过期时间
        try {
            LocalDateTime expiresAt = LocalDateTime.parse(expiresAtStr);
            if (LocalDateTime.now().isAfter(expiresAt)) {
                return null; // token 已过期
            }
        } catch (Exception e) {
            return null; // 无法解析过期时间，视为无效
        }
        try {
            return Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 注销账号：先删用户偏好，再删宠物档案及关联记录，最后删用户。同一事务内完成。
     */
    @Override
    @Transactional
    public void deleteAccount(Long userId) {
        SysUser user = sysUserRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));

        userPreferenceRepository.deleteByUserId(userId);

        List<PetProfile> profiles = petProfileRepository.findByUserId(userId);
        for (PetProfile profile : profiles) {
            String petId = profile.getPetId();
            petWeightRecordRepository.deleteByPetId(petId);
            petMedicalRecordRepository.deleteByPetId(petId);
            petLedgerRecordRepository.deleteByPetId(petId);
            petMediaRecordRepository.deleteByPetId(petId);
        }
        // 子资源清理后再删档案，避免子资源悬空。
        petProfileRepository.deleteAll(profiles);

        sysUserRepository.delete(user);
    }

    /**
     * 修改密码：先校验用户存在，再核对当前密码，最后写入新密码。
     */
    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        SysUser user = sysUserRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));

        if (!request.getOldPassword().equals(user.getPassword())) {
            throw BusinessException.unauthorized("当前密码错误");
        }

        user.setPassword(request.getNewPassword());
        sysUserRepository.save(user);
    }

    private LoginResponse buildLoginResponse(SysUser user, LocalDateTime expiresAt) {
        return buildUserResponse(user, buildToken(user, expiresAt), expiresAt);
    }

    private LoginResponse buildUserResponse(SysUser user, String token, LocalDateTime expiresAt) {
        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .userRole(user.getRole())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .location(user.getLocation())
                .expiresAt(expiresAt)
                .build();
    }

    private String requiredProfileText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
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

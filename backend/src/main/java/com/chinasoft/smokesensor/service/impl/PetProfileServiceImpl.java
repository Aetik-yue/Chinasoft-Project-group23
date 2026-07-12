package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.common.BusinessException;
import com.chinasoft.smokesensor.common.UserContext;
import com.chinasoft.smokesensor.dto.PetProfileCreateRequest;
import com.chinasoft.smokesensor.dto.PetProfileResponse;
import com.chinasoft.smokesensor.dto.PetProfileUpdateRequest;
import com.chinasoft.smokesensor.entity.PetProfile;
import com.chinasoft.smokesensor.entity.PetWeightRecord;
import com.chinasoft.smokesensor.repository.PetLedgerRecordRepository;
import com.chinasoft.smokesensor.repository.ParrotBehaviorRecordRepository;
import com.chinasoft.smokesensor.repository.PetMediaRecordRepository;
import com.chinasoft.smokesensor.repository.PetMedicalRecordRepository;
import com.chinasoft.smokesensor.repository.PetProfileRepository;
import com.chinasoft.smokesensor.repository.PetWeightRecordRepository;
import com.chinasoft.smokesensor.service.PetProfileService;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 鹦鹉档案业务。
 *
 * <p>所有查询和写入都以 {@link UserContext#requireUserId()} 取当前登录用户 ID 做隔离：
 * 列表只返回当前用户的档案；按 petId 查询时同时校验归属，不属于当前用户则返回 404。
 * 创建档案和首条体重记录必须在同一事务内完成。
 */
@Service
@RequiredArgsConstructor
public class PetProfileServiceImpl implements PetProfileService {
    private static final Set<String> SEX_VALUES = Set.of("male", "female", "unknown");

    private final PetProfileRepository profileRepository;
    private final PetWeightRecordRepository weightRepository;
    private final PetMedicalRecordRepository medicalRepository;
    private final PetLedgerRecordRepository ledgerRepository;
    private final PetMediaRecordRepository photoRepository;
    private final ParrotBehaviorRecordRepository behaviorRecordRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PetProfileResponse> listProfiles() {
        Long userId = UserContext.requireUserId();
        return profileRepository.findByUserIdAndEnabledTrueOrderByUpdatedAtDesc(userId).stream()
                .map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PetProfileResponse getProfile(String petId) {
        return toResponse(findOwnedProfile(petId));
    }

    @Override
    @Transactional
    public PetProfileResponse createProfile(PetProfileCreateRequest request) {
        Long userId = UserContext.requireUserId();
        validateBirthday(request.getBirthday());
        String sex = normalizeSex(request.getSex());
        String petId = generateBusinessId("PET");
        PetProfile profile = PetProfile.builder()
                .petId(petId)
                .userId(userId)
                .deviceId(trimToNull(request.getDeviceId()))
                .name(required(request.getName(), "name 不能为空"))
                .species(required(request.getSpecies(), "species 不能为空"))
                .birthday(request.getBirthday())
                .sex(sex)
                .weightGrams(request.getInitialWeightGrams())
                .featherColor(trimToNull(request.getFeatherColor()))
                .sterilized(Boolean.TRUE.equals(request.getSterilized()))
                .avatarUrl(trimToNull(request.getAvatarUrl()))
                .currentStatus(trimToNull(request.getCurrentStatus()))
                .enabled(true)
                .remark(trimToNull(request.getRemark()))
                .build();
        PetProfile saved = profileRepository.save(profile);

        if (request.getInitialWeightGrams() != null) {
            weightRepository.save(PetWeightRecord.builder()
                    .petId(petId)
                    .weightGrams(request.getInitialWeightGrams())
                    .measuredAt(LocalDateTime.now())
                    .source("manual")
                    .remark("创建档案时录入的初始体重")
                    .build());
        }
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PetProfileResponse updateProfile(String petId, PetProfileUpdateRequest request) {
        PetProfile profile = findOwnedProfile(petId);
        if (request.getName() != null) profile.setName(required(request.getName(), "name 不能为空"));
        if (request.getSpecies() != null) profile.setSpecies(required(request.getSpecies(), "species 不能为空"));
        if (request.getBirthday() != null) {
            validateBirthday(request.getBirthday());
            profile.setBirthday(request.getBirthday());
        }
        if (request.getSex() != null) profile.setSex(normalizeSex(request.getSex()));
        if (request.getDeviceId() != null) profile.setDeviceId(trimToNull(request.getDeviceId()));
        if (request.getFeatherColor() != null) profile.setFeatherColor(trimToNull(request.getFeatherColor()));
        if (request.getSterilized() != null) profile.setSterilized(request.getSterilized());
        if (request.getAvatarUrl() != null) profile.setAvatarUrl(trimToNull(request.getAvatarUrl()));
        if (request.getCurrentStatus() != null) profile.setCurrentStatus(trimToNull(request.getCurrentStatus()));
        if (request.getRemark() != null) profile.setRemark(trimToNull(request.getRemark()));
        if (request.getEnabled() != null) profile.setEnabled(request.getEnabled());
        return toResponse(profileRepository.save(profile));
    }

    /**
     * 删除宠物档案：先清理体重/病历/记账/照片等关联记录，再删除档案本身。
     */
    @Override
    @Transactional
    public void deleteProfile(String petId) {
        PetProfile profile = findOwnedProfile(petId);
        String targetPetId = profile.getPetId();
        weightRepository.deleteByPetId(targetPetId);
        medicalRepository.deleteByPetId(targetPetId);
        ledgerRepository.deleteByPetId(targetPetId);
        photoRepository.deleteByPetId(targetPetId);
        behaviorRecordRepository.deleteByPetId(targetPetId);
        profileRepository.delete(profile);
    }

    /**
     * 按 petId 查询当前用户名下的档案；不存在或不属于当前用户均抛 404，
     * 避免通过错误信息泄露其它用户的档案存在性。
     */
    private PetProfile findOwnedProfile(String petId) {
        String normalized = required(petId, "petId 不能为空");
        Long userId = UserContext.requireUserId();
        return profileRepository.findByPetIdAndUserId(normalized, userId)
                .orElseThrow(() -> BusinessException.notFound("鹦鹉档案不存在: " + normalized));
    }

    private String normalizeSex(String value) {
        String normalized = value == null || value.isBlank() ? "unknown" : value.trim().toLowerCase(Locale.ROOT);
        if (!SEX_VALUES.contains(normalized)) {
            throw new IllegalArgumentException("sex 只能是 male、female 或 unknown");
        }
        return normalized;
    }

    private void validateBirthday(LocalDate birthday) {
        if (birthday != null && birthday.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("birthday 不能晚于今天");
        }
    }

    private String generateBusinessId(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value.trim();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private PetProfileResponse toResponse(PetProfile profile) {
        return PetProfileResponse.builder()
                .petId(profile.getPetId()).userId(profile.getUserId())
                .deviceId(profile.getDeviceId()).name(profile.getName()).species(profile.getSpecies())
                .birthday(profile.getBirthday()).sex(profile.getSex()).weightGrams(profile.getWeightGrams())
                .featherColor(profile.getFeatherColor()).sterilized(profile.getSterilized())
                .avatarUrl(profile.getAvatarUrl()).currentStatus(profile.getCurrentStatus())
                .enabled(profile.getEnabled()).remark(profile.getRemark())
                .createdAt(profile.getCreatedAt()).updatedAt(profile.getUpdatedAt()).build();
    }
}

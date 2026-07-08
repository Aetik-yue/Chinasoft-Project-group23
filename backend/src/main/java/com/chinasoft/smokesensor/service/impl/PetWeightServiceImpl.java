package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.common.BusinessException;
import com.chinasoft.smokesensor.common.UserContext;
import com.chinasoft.smokesensor.dto.PetWeightRequest;
import com.chinasoft.smokesensor.dto.PetWeightResponse;
import com.chinasoft.smokesensor.entity.PetProfile;
import com.chinasoft.smokesensor.entity.PetWeightRecord;
import com.chinasoft.smokesensor.repository.PetProfileRepository;
import com.chinasoft.smokesensor.repository.PetWeightRecordRepository;
import com.chinasoft.smokesensor.service.PetWeightService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 体重记录业务；每次写入后同步档案中的最新体重。 */
@Service
@RequiredArgsConstructor
public class PetWeightServiceImpl implements PetWeightService {
    private final PetProfileRepository profileRepository;
    private final PetWeightRecordRepository weightRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PetWeightResponse> listWeights(String petId) {
        requireProfile(petId);
        return weightRepository.findByPetIdOrderByMeasuredAtDesc(petId.trim()).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public PetWeightResponse createWeight(String petId, PetWeightRequest request) {
        PetProfile profile = requireProfile(petId);
        validate(request);
        PetWeightRecord saved = weightRepository.save(PetWeightRecord.builder()
                .petId(profile.getPetId()).weightGrams(request.getWeightGrams())
                .measuredAt(request.getMeasuredAt()).source(defaultText(request.getSource(), "manual"))
                .remark(trimToNull(request.getRemark())).build());
        syncCurrentWeight(profile);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PetWeightResponse updateWeight(String petId, Long id, PetWeightRequest request) {
        PetProfile profile = requireProfile(petId);
        if (id == null) throw new IllegalArgumentException("体重记录 id 不能为空");
        validate(request);
        PetWeightRecord record = weightRepository.findByIdAndPetId(id, profile.getPetId())
                .orElseThrow(() -> BusinessException.notFound("体重记录不存在或不属于该鹦鹉: " + id));
        record.setWeightGrams(request.getWeightGrams());
        record.setMeasuredAt(request.getMeasuredAt());
        record.setSource(defaultText(request.getSource(), "manual"));
        record.setRemark(trimToNull(request.getRemark()));
        PetWeightRecord saved = weightRepository.save(record);
        syncCurrentWeight(profile);
        return toResponse(saved);
    }

    private void syncCurrentWeight(PetProfile profile) {
        BigDecimal latestWeight = weightRepository.findTopByPetIdOrderByMeasuredAtDesc(profile.getPetId())
                .map(PetWeightRecord::getWeightGrams).orElse(null);
        profile.setWeightGrams(latestWeight);
        profileRepository.save(profile);
    }

    private PetProfile requireProfile(String petId) {
        if (petId == null || petId.isBlank()) throw new IllegalArgumentException("petId 不能为空");
        String normalized = petId.trim();
        return profileRepository.findByPetIdAndUserId(normalized, UserContext.requireUserId())
                .orElseThrow(() -> BusinessException.notFound("鹦鹉档案不存在: " + normalized));
    }

    private void validate(PetWeightRequest request) {
        if (request == null || request.getWeightGrams() == null
                || request.getWeightGrams().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("weightGrams 必须大于 0");
        }
        if (request.getMeasuredAt() == null || request.getMeasuredAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("measuredAt 不能为空且不能晚于当前时间");
        }
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private PetWeightResponse toResponse(PetWeightRecord record) {
        return PetWeightResponse.builder().id(record.getId()).petId(record.getPetId())
                .weightGrams(record.getWeightGrams()).measuredAt(record.getMeasuredAt())
                .source(record.getSource()).remark(record.getRemark()).createdAt(record.getCreatedAt()).build();
    }
}

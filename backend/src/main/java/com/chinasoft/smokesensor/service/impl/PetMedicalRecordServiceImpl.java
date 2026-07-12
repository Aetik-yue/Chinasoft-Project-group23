package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.common.BusinessException;
import com.chinasoft.smokesensor.common.UserContext;
import com.chinasoft.smokesensor.dto.PetMedicalRecordRequest;
import com.chinasoft.smokesensor.dto.PetMedicalRecordResponse;
import com.chinasoft.smokesensor.entity.PetMedicalRecord;
import com.chinasoft.smokesensor.repository.PetMedicalRecordRepository;
import com.chinasoft.smokesensor.repository.PetProfileRepository;
import com.chinasoft.smokesensor.service.PetMedicalRecordService;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chinasoft.smokesensor.service.qq.OneBotPushService;

/** 病历新增、修改和按鹦鹉查询业务。 */
@Service
@RequiredArgsConstructor
public class PetMedicalRecordServiceImpl implements PetMedicalRecordService {
    private static final Set<String> TYPES = Set.of("symptom", "diagnosis", "medication", "recheck", "other");
    private final PetProfileRepository profileRepository;
    private final PetMedicalRecordRepository recordRepository;
    private final OneBotPushService oneBotPushService;

    @Override
    @Transactional(readOnly = true)
    public List<PetMedicalRecordResponse> listRecords(String petId) {
        String normalized = requireProfile(petId);
        return recordRepository.findByPetIdOrderByRecordDateDescCreatedAtDesc(normalized).stream()
                .map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public PetMedicalRecordResponse createRecord(String petId, PetMedicalRecordRequest request) {
        String normalized = requireProfile(petId);
        validate(request);
        PetMedicalRecord record = PetMedicalRecord.builder()
                .recordId(businessId("MED")).petId(normalized).recordDate(request.getRecordDate())
                .recordType(normalizeType(request.getRecordType())).title(trimToNull(request.getTitle()))
                .content(request.getContent().trim()).hospitalName(trimToNull(request.getHospitalName()))
                .hospitalPhone(trimToNull(request.getHospitalPhone())).attachments(copyAttachments(request.getAttachments()))
                .build();
        PetMedicalRecordResponse response = toResponse(recordRepository.save(record));
        checkHealthScoreAndAlert(normalized, record.getContent());
        return response;
    }

    @Override
    @Transactional
    public PetMedicalRecordResponse updateRecord(String petId, String recordId, PetMedicalRecordRequest request) {
        String normalized = requireProfile(petId);
        validate(request);
        PetMedicalRecord record = recordRepository.findByRecordIdAndPetId(required(recordId, "recordId 不能为空"), normalized)
                .orElseThrow(() -> BusinessException.notFound("病历不存在或不属于该鹦鹉: " + recordId));
        record.setRecordDate(request.getRecordDate());
        record.setRecordType(normalizeType(request.getRecordType()));
        record.setTitle(trimToNull(request.getTitle()));
        record.setContent(request.getContent().trim());
        record.setHospitalName(trimToNull(request.getHospitalName()));
        record.setHospitalPhone(trimToNull(request.getHospitalPhone()));
        record.setAttachments(copyAttachments(request.getAttachments()));
        PetMedicalRecordResponse response = toResponse(recordRepository.save(record));
        checkHealthScoreAndAlert(normalized, record.getContent());
        return response;
    }

    @Override
    @Transactional
    public String deleteRecord(String petId, String recordId) {
        String normalized = requireProfile(petId);
        PetMedicalRecord record = recordRepository.findByRecordIdAndPetId(required(recordId, "recordId 不能为空"), normalized)
                .orElseThrow(() -> BusinessException.notFound("病历不存在或不属于该鹦鹉: " + recordId));
        recordRepository.delete(record);
        return "病历已删除: " + recordId;
    }

    private String requireProfile(String petId) {
        String normalized = required(petId, "petId 不能为空");
        if (!profileRepository.existsByPetIdAndUserId(normalized, UserContext.requireUserId())) {
            throw BusinessException.notFound("鹦鹉档案不存在: " + normalized);
        }
        return normalized;
    }

    private void validate(PetMedicalRecordRequest request) {
        if (request == null || request.getRecordDate() == null || request.getRecordDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("recordDate 不能为空且不能晚于今天");
        }
        required(request.getContent(), "content 不能为空");
    }

    private String normalizeType(String value) {
        String normalized = value == null || value.isBlank() ? "symptom" : value.trim().toLowerCase(Locale.ROOT);
        if (!TYPES.contains(normalized)) throw new IllegalArgumentException("recordType 不受支持: " + normalized);
        return normalized;
    }

    private List<String> copyAttachments(List<String> values) {
        if (values == null) return null;
        return values.stream().map(value -> required(value, "附件 URL 不能为空")).toList();
    }

    private String businessId(String prefix) { return prefix + "-" + UUID.randomUUID(); }
    private String required(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value.trim();
    }
    private String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private PetMedicalRecordResponse toResponse(PetMedicalRecord record) {
        return PetMedicalRecordResponse.builder().recordId(record.getRecordId()).petId(record.getPetId())
                .recordDate(record.getRecordDate()).recordType(record.getRecordType()).title(record.getTitle())
                .content(record.getContent()).hospitalName(record.getHospitalName()).hospitalPhone(record.getHospitalPhone())
                .attachments(record.getAttachments()).createdAt(record.getCreatedAt()).updatedAt(record.getUpdatedAt()).build();
    }

    private void checkHealthScoreAndAlert(String petId, String latestContent) {
        try {
            LocalDate cutoff = LocalDate.now().minusDays(90);
            List<PetMedicalRecord> records = recordRepository.findByPetIdOrderByRecordDateDescCreatedAtDesc(petId);
            List<PetMedicalRecord> recent90 = records.stream()
                    .filter(r -> r.getRecordDate() != null && !r.getRecordDate().isBefore(cutoff))
                    .toList();

            long symptomCount = recent90.stream().filter(r -> "symptom".equals(r.getRecordType())).count();
            long diagnosisCount = recent90.stream().filter(r -> "diagnosis".equals(r.getRecordType())).count();
            long medicationCount = recent90.stream().filter(r -> "medication".equals(r.getRecordType())).count();
            long recheckCount = recent90.stream().filter(r -> "recheck".equals(r.getRecordType())).count();
            long otherCount = recent90.stream().filter(r -> "other".equals(r.getRecordType())).count();

            int total = 100;
            total += Math.max(-8 * symptomCount, -40);
            total += Math.max(-6 * diagnosisCount, -30);
            total += Math.max(-3 * medicationCount, -15);
            total += Math.min(3 * recheckCount, 12);
            total += Math.max(-1 * otherCount, -5);
            total = Math.max(0, Math.min(100, total));

            int finalTotal = total;
            if (total < 60) {
                profileRepository.findByPetId(petId).ifPresent(pet -> {
                    oneBotPushService.pushHealthScoreAlert(pet.getUserId(), pet.getName(), finalTotal, latestContent);
                });
            }
        } catch (Exception e) {
            // 捕获异常，防止外部推送失败影响业务写入
        }
    }
}

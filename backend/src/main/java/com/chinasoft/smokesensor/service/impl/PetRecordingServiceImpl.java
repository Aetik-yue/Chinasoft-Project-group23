package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.common.BusinessException;
import com.chinasoft.smokesensor.common.UserContext;
import com.chinasoft.smokesensor.dto.PetPhotoCreateRequest;
import com.chinasoft.smokesensor.dto.PetPhotoDeleteResponse;
import com.chinasoft.smokesensor.dto.PetPhotoResponse;
import com.chinasoft.smokesensor.entity.PetMediaRecord;
import com.chinasoft.smokesensor.repository.PetMediaRecordRepository;
import com.chinasoft.smokesensor.repository.PetProfileRepository;
import com.chinasoft.smokesensor.service.PetRecordingService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 宠物录音管理业务实现。
 */
@Service
@RequiredArgsConstructor
public class PetRecordingServiceImpl implements PetRecordingService {
    private static final String RECORDING_TYPE = "recording";
    private static final int MAX_RECORDINGS_PER_PET = 30;
    
    private final PetProfileRepository profileRepository;
    private final PetMediaRecordRepository mediaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PetPhotoResponse> listRecordings(String petId) {
        String normalized = requireProfile(petId);
        return mediaRepository.findByPetIdAndMediaTypeInOrderByCapturedAtDesc(normalized, Set.of(RECORDING_TYPE)).stream()
                .filter(media -> normalized.equals(media.getPetId()))
                .map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public PetPhotoResponse createRecording(String petId, PetPhotoCreateRequest request) {
        String normalized = requireProfile(petId);
        validate(request);
        
        PetMediaRecord media = PetMediaRecord.builder()
                .mediaId("MEDIA-" + UUID.randomUUID())
                .petId(normalized)
                .mediaType(RECORDING_TYPE)
                .title(trimToNull(request.getTitle()))
                .fileUrl(request.getFileUrl() == null || request.getFileUrl().isBlank() ? "base64" : request.getFileUrl().trim())
                .imageData(trimToNull(request.getImageBase64()))
                .thumbnailUrl(trimToNull(request.getThumbnailUrl()))
                .durationSeconds(request.getDurationSeconds())
                .tags(trimToNull(request.getTags()))
                .capturedAt(resolveCapturedAt(request.getCapturedAt()))
                .build();
                
        PetMediaRecord saved = mediaRepository.save(media);
        enforceRecordingLimit(normalized);
        return toResponse(saved);
    }

    private void enforceRecordingLimit(String petId) {
        long count = mediaRepository.countByPetIdAndMediaType(petId, RECORDING_TYPE);
        if (count <= MAX_RECORDINGS_PER_PET) return;
        int overflow = (int) (count - MAX_RECORDINGS_PER_PET);
        List<PetMediaRecord> oldest = mediaRepository
                .findByPetIdAndMediaTypeOrderByCapturedAtAsc(petId, RECORDING_TYPE)
                .stream().limit(overflow).toList();
        mediaRepository.deleteAll(oldest);
    }

    @Override
    @Transactional
    public PetPhotoDeleteResponse deleteRecording(String petId, String mediaId) {
        String normalized = requireProfile(petId);
        String normalizedMediaId = required(mediaId, "mediaId 不能为空");
        PetMediaRecord media = mediaRepository.findByMediaIdAndPetId(normalizedMediaId, normalized)
                .orElseThrow(() -> BusinessException.notFound("录音不存在或不属于该鹦鹉: " + normalizedMediaId));
        if (!RECORDING_TYPE.equals(media.getMediaType())) {
            throw BusinessException.notFound("录音不存在或不属于该鹦鹉: " + normalizedMediaId);
        }
        mediaRepository.delete(media);
        return PetPhotoDeleteResponse.builder().mediaId(media.getMediaId()).petId(normalized)
                .deletedAt(LocalDateTime.now()).build();
    }

    private String requireProfile(String petId) {
        String normalized = required(petId, "petId 不能为空");
        if (!profileRepository.existsByPetIdAndUserId(normalized, UserContext.requireUserId())) {
            throw BusinessException.notFound("鹦鹉档案不存在: " + normalized);
        }
        return normalized;
    }

    private void validate(PetPhotoCreateRequest request) {
        if (request == null) throw new IllegalArgumentException("请求体不能为空");
        boolean hasBase64 = request.getImageBase64() != null && !request.getImageBase64().isBlank();
        boolean hasUrl = request.getFileUrl() != null && !request.getFileUrl().isBlank();
        if (!hasBase64 && !hasUrl) throw new IllegalArgumentException("音频数据不能为空");
    }

    private LocalDateTime resolveCapturedAt(LocalDateTime capturedAt) {
        LocalDateTime now = LocalDateTime.now();
        if (capturedAt != null && capturedAt.isAfter(now)) {
            throw new IllegalArgumentException("capturedAt 不能晚于当前时间");
        }
        return capturedAt == null ? now : capturedAt;
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value.trim();
    }

    private String trimToNull(String value) { 
        return value == null || value.isBlank() ? null : value.trim(); 
    }

    private PetPhotoResponse toResponse(PetMediaRecord media) {
        return PetPhotoResponse.builder()
                .mediaId(media.getMediaId())
                .petId(media.getPetId())
                .mediaType(media.getMediaType())
                .title(media.getTitle())
                .fileUrl(media.getFileUrl())
                .imageBase64(media.getImageData())
                .thumbnailUrl(media.getThumbnailUrl())
                .durationSeconds(media.getDurationSeconds())
                .tags(media.getTags())
                .capturedAt(media.getCapturedAt())
                .createdAt(media.getCreatedAt())
                .updatedAt(media.getUpdatedAt())
                .build();
    }
}

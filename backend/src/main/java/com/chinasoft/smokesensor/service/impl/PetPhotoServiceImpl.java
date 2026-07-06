package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.common.BusinessException;
import com.chinasoft.smokesensor.dto.PetPhotoCreateRequest;
import com.chinasoft.smokesensor.dto.PetPhotoDeleteResponse;
import com.chinasoft.smokesensor.dto.PetPhotoResponse;
import com.chinasoft.smokesensor.entity.PetMediaRecord;
import com.chinasoft.smokesensor.repository.PetMediaRecordRepository;
import com.chinasoft.smokesensor.repository.PetProfileRepository;
import com.chinasoft.smokesensor.service.PetPhotoService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 相片元数据业务；不接收二进制文件，只保存 URL。 */
@Service
@RequiredArgsConstructor
public class PetPhotoServiceImpl implements PetPhotoService {
    private static final Set<String> PHOTO_TYPES = Set.of("photo", "screenshot");
    private final PetProfileRepository profileRepository;
    private final PetMediaRecordRepository mediaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PetPhotoResponse> listPhotos(String petId) {
        String normalized = requireProfile(petId);
        return mediaRepository.findByPetIdAndMediaTypeInOrderByCapturedAtDesc(normalized, PHOTO_TYPES).stream()
                .map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public PetPhotoResponse createPhoto(String petId, PetPhotoCreateRequest request) {
        String normalized = requireProfile(petId);
        validate(request);
        String mediaType = request.getMediaType() == null || request.getMediaType().isBlank()
                ? "photo" : request.getMediaType().trim().toLowerCase(Locale.ROOT);
        if (!PHOTO_TYPES.contains(mediaType)) throw new IllegalArgumentException("mediaType 只能是 photo 或 screenshot");
        PetMediaRecord media = PetMediaRecord.builder().mediaId("MEDIA-" + UUID.randomUUID())
                .petId(normalized).cageId(trimToNull(request.getCageId())).mediaType(mediaType)
                .title(trimToNull(request.getTitle())).fileUrl(request.getFileUrl().trim())
                .thumbnailUrl(trimToNull(request.getThumbnailUrl())).tags(trimToNull(request.getTags()))
                .capturedAt(request.getCapturedAt()).build();
        return toResponse(mediaRepository.save(media));
    }

    @Override
    @Transactional
    public PetPhotoDeleteResponse deletePhoto(String petId, String mediaId) {
        String normalized = requireProfile(petId);
        String normalizedMediaId = required(mediaId, "mediaId 不能为空");
        PetMediaRecord media = mediaRepository.findByMediaIdAndPetId(normalizedMediaId, normalized)
                .orElseThrow(() -> BusinessException.notFound("相片不存在或不属于该鹦鹉: " + normalizedMediaId));
        if (!PHOTO_TYPES.contains(media.getMediaType())) {
            throw BusinessException.notFound("相片不存在或不属于该鹦鹉: " + normalizedMediaId);
        }
        mediaRepository.delete(media);
        return PetPhotoDeleteResponse.builder().mediaId(media.getMediaId()).petId(normalized)
                .deletedAt(LocalDateTime.now()).build();
    }

    private String requireProfile(String petId) {
        String normalized = required(petId, "petId 不能为空");
        if (!profileRepository.existsByPetId(normalized)) throw BusinessException.notFound("鹦鹉档案不存在: " + normalized);
        return normalized;
    }
    private void validate(PetPhotoCreateRequest request) {
        if (request == null) throw new IllegalArgumentException("请求体不能为空");
        required(request.getFileUrl(), "fileUrl 不能为空");
        if (request.getCapturedAt() == null || request.getCapturedAt().isAfter(LocalDateTime.now()))
            throw new IllegalArgumentException("capturedAt 不能为空且不能晚于当前时间");
    }
    private String required(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value.trim();
    }
    private String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private PetPhotoResponse toResponse(PetMediaRecord media) {
        return PetPhotoResponse.builder().mediaId(media.getMediaId()).petId(media.getPetId())
                .cageId(media.getCageId()).mediaType(media.getMediaType()).title(media.getTitle())
                .fileUrl(media.getFileUrl()).thumbnailUrl(media.getThumbnailUrl()).tags(media.getTags())
                .capturedAt(media.getCapturedAt()).createdAt(media.getCreatedAt()).updatedAt(media.getUpdatedAt()).build();
    }
}

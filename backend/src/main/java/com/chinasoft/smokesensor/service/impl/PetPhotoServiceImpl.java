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

/** 相片元数据业务；支持 fileUrl 或 imageBase64 两种入库方式，截图走 base64 存 LONGTEXT。 */
@Service
@RequiredArgsConstructor
public class PetPhotoServiceImpl implements PetPhotoService {
    private static final Set<String> PHOTO_TYPES = Set.of("photo", "screenshot");
    /** 单只鹦鹉的截图保留上限：超过后自动清理最旧的（行车记录仪模式）。 */
    private static final int MAX_SCREENSHOTS_PER_PET = 30;
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
                .title(trimToNull(request.getTitle())).fileUrl(trimToNull(request.getFileUrl()))
                .imageData(trimToNull(request.getImageBase64()))
                .thumbnailUrl(trimToNull(request.getThumbnailUrl())).tags(trimToNull(request.getTags()))
                .capturedAt(request.getCapturedAt()).build();
        PetMediaRecord saved = mediaRepository.save(media);
        // 上限管理：screenshot 超过上限时，自动清理最旧的（行车记录仪模式）
        if ("screenshot".equals(mediaType)) {
            enforceScreenshotLimit(normalized);
        }
        return toResponse(saved);
    }

    /** 单只鹦鹉的截图超过上限时，按拍摄时间删除最旧的若干条。 */
    private void enforceScreenshotLimit(String petId) {
        long count = mediaRepository.countByPetIdAndMediaType(petId, "screenshot");
        if (count <= MAX_SCREENSHOTS_PER_PET) return;
        int overflow = (int) (count - MAX_SCREENSHOTS_PER_PET);
        List<PetMediaRecord> oldest = mediaRepository
                .findByPetIdAndMediaTypeOrderByCapturedAtAsc(petId, "screenshot")
                .stream().limit(overflow).toList();
        mediaRepository.deleteAll(oldest);
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
        boolean hasUrl = request.getFileUrl() != null && !request.getFileUrl().isBlank();
        boolean hasBase64 = request.getImageBase64() != null && !request.getImageBase64().isBlank();
        if (!hasUrl && !hasBase64) throw new IllegalArgumentException("fileUrl 与 imageBase64 至少填一个");
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
                .fileUrl(media.getFileUrl()).imageBase64(media.getImageData())
                .thumbnailUrl(media.getThumbnailUrl()).tags(media.getTags())
                .capturedAt(media.getCapturedAt()).createdAt(media.getCreatedAt()).updatedAt(media.getUpdatedAt()).build();
    }
}

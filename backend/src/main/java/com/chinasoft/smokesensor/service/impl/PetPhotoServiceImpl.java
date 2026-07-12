package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.common.BusinessException;
import com.chinasoft.smokesensor.common.UserContext;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chinasoft.smokesensor.service.qq.OneBotPushService;

/** 相片元数据业务；支持 fileUrl 或 imageBase64 两种入库方式，截图走 base64 存 LONGTEXT。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PetPhotoServiceImpl implements PetPhotoService {
    private static final Set<String> PHOTO_TYPES = Set.of("photo", "screenshot");
    /** 单只鹦鹉的截图保留上限：超过后自动清理最旧的（行车记录仪模式）。 */
    private static final int MAX_SCREENSHOTS_PER_PET = 30;
    private final PetProfileRepository profileRepository;
    private final PetMediaRecordRepository mediaRepository;
    private final OneBotPushService oneBotPushService;

    @Override
    @Transactional(readOnly = true)
    public List<PetPhotoResponse> listPhotos(String petId) {
        String normalized = requireProfile(petId);
        return mediaRepository.findByPetIdAndMediaTypeInOrderByCapturedAtDescIdDesc(normalized, PHOTO_TYPES).stream()
                // 即使仓储实现被替换或返回异常数据，也绝不能把另一只宠物的媒体序列化给当前档案。
                .filter(media -> normalized.equals(media.getPetId()))
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
                .petId(normalized).mediaType(mediaType)
                .title(trimToNull(request.getTitle())).fileUrl(trimToNull(request.getFileUrl()))
                .imageData(trimToNull(request.getImageBase64()))
                .thumbnailUrl(trimToNull(request.getThumbnailUrl())).tags(trimToNull(request.getTags()))
                .durationSeconds(request.getDurationSeconds())
                .capturedAt(resolveCapturedAt(request.getCapturedAt())).build();
        PetMediaRecord saved = mediaRepository.save(media);
        // 上限管理：screenshot 超过上限时，自动清理最旧的（行车记录仪模式）
        if ("screenshot".equals(mediaType)) {
            enforceScreenshotLimit(normalized);
            long now = System.currentTimeMillis();
            log.info("收到前端上传的截图快照. lastScreenshotQq={}, timeDiff={}ms", 
                OneBotPushService.lastScreenshotQq, 
                (now - OneBotPushService.lastScreenshotTime));
            if (OneBotPushService.lastScreenshotQq != null && (now - OneBotPushService.lastScreenshotTime < 60000)) {
                try {
                    oneBotPushService.sendScreenshotToQq(OneBotPushService.lastScreenshotQq, saved.getImageData());
                } catch (Exception e) {
                    log.error("触发 QQ 截图返图异常: {}", e.getMessage(), e);
                }
                OneBotPushService.lastScreenshotQq = null;
            }
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
        if (!profileRepository.existsByPetIdAndUserId(normalized, UserContext.requireUserId())) throw BusinessException.notFound("鹦鹉档案不存在: " + normalized);
        return normalized;
    }
    private void validate(PetPhotoCreateRequest request) {
        if (request == null) throw new IllegalArgumentException("请求体不能为空");
        boolean hasUrl = request.getFileUrl() != null && !request.getFileUrl().isBlank();
        boolean hasBase64 = request.getImageBase64() != null && !request.getImageBase64().isBlank();
        if (!hasUrl && !hasBase64) throw new IllegalArgumentException("fileUrl 与 imageBase64 至少填一个");
    }

    /** 未传拍摄时间的实时截图由服务端写入当前时间，避免客户端时钟/时区误差。 */
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
    private String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private PetPhotoResponse toResponse(PetMediaRecord media) {
        return PetPhotoResponse.builder().mediaId(media.getMediaId()).petId(media.getPetId())
                .mediaType(media.getMediaType()).title(media.getTitle())
                .fileUrl(media.getFileUrl()).imageBase64(media.getImageData())
                .thumbnailUrl(media.getThumbnailUrl()).tags(media.getTags())
                .durationSeconds(media.getDurationSeconds())
                .capturedAt(media.getCapturedAt()).createdAt(media.getCreatedAt()).updatedAt(media.getUpdatedAt()).build();
    }
}

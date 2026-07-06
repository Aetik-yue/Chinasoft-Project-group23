package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.common.BusinessException;
import com.chinasoft.smokesensor.config.ParrotProperties;
import com.chinasoft.smokesensor.dto.ParrotBehaviorResponse;
import com.chinasoft.smokesensor.entity.ParrotBehaviorRecord;
import com.chinasoft.smokesensor.repository.ParrotBehaviorRecordRepository;
import com.chinasoft.smokesensor.service.ParrotBehaviorService;
import com.chinasoft.smokesensor.service.parrot.ClipBehaviorProvider;
import com.chinasoft.smokesensor.service.parrot.ParrotDetectionProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 鹦鹉行为识别实现：截图 → YOLO 检测鹦鹉 → CLIP 行为分类 → 落库。
 *
 * <p>分级可用：YOLO 未配置 → 5001；YOLO 已配置、CLIP 未配置 → 返回鹦鹉检测结果，behavior 留空；
 * 两者都配置 → 完整结果。模型推理较重，放在数据库事务之外执行，save 由仓库层事务兜底。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParrotBehaviorServiceImpl implements ParrotBehaviorService {

    private final ParrotProperties parrotProperties;
    private final ParrotDetectionProvider parrotDetectionProvider;
    private final ClipBehaviorProvider clipBehaviorProvider;
    private final ParrotBehaviorRecordRepository parrotBehaviorRecordRepository;

    @Override
    public ParrotBehaviorResponse check(String deviceId) {
        String snapshotPath = resolveSnapshotPath();
        return analyze(resolveDeviceId(deviceId), snapshotPath, snapshotPath);
    }

    @Override
    public ParrotBehaviorResponse check(MultipartFile file, String deviceId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        Path temp = null;
        try {
            temp = Files.createTempFile("parrot-upload-", getSuffix(file.getOriginalFilename()));
            try (var in = file.getInputStream()) {
                Files.copy(in, temp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            String imageUrl = file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()
                    ? "upload" : file.getOriginalFilename();
            return analyze(resolveDeviceId(deviceId), temp.toString(), imageUrl);
        } catch (IOException e) {
            throw new BusinessException(5000, "保存上传图片失败: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // 忽略临时文件删除失败
                }
            }
        }
    }

    /** 共用分析逻辑：YOLO 检测 → CLIP 分类 → 落库 → 返回。imageUrl 为记录里存的图片来源标识。 */
    private ParrotBehaviorResponse analyze(String deviceId, String imagePath, String imageUrl) {
        try {
            ParrotDetectionProvider.DetectionOutcome detection = parrotDetectionProvider.detect(imagePath);

            String behavior = null;
            Double behaviorConfidence = null;
            if (detection.detected()) {
                try {
                    ClipBehaviorProvider.Classification classification =
                            clipBehaviorProvider.classify(detection.crop());
                    behavior = classification.behavior();
                    behaviorConfidence = classification.confidence();
                } catch (BusinessException e) {
                    // CLIP 未配置时，仍返回鹦鹉检测结果，行为字段留空
                    log.warn("CLIP 行为识别跳过: {}", e.getMessage());
                }
            }

            ParrotBehaviorRecord record = ParrotBehaviorRecord.builder()
                    .deviceId(deviceId)
                    .imageUrl(imageUrl)
                    .parrotDetected(detection.detected())
                    .parrotConfidence(detection.detected() ? detection.confidence() : null)
                    .behavior(behavior)
                    .behaviorConfidence(behaviorConfidence)
                    .checkedAt(java.time.LocalDateTime.now())
                    .build();
            parrotBehaviorRecordRepository.save(record);
            log.info("鹦鹉行为识别 deviceId={} detected={} behavior={}",
                    deviceId, detection.detected(), behavior);
            return toResponse(record);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("鹦鹉行为识别失败", e);
            throw new BusinessException(5000, "鹦鹉行为识别失败: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String resolveSnapshotPath() {
        String path = parrotProperties.getSnapshotPath();
        if (path == null || path.isBlank()) {
            throw new BusinessException(5001,
                    "鹦鹉截图未配置：请在 application.yml 设置 parrot.snapshot-path",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (!Files.exists(Path.of(path))) {
            throw new BusinessException(5001, "鹦鹉截图文件不存在: " + path,
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        return path;
    }

    private String resolveDeviceId(String deviceId) {
        if (deviceId != null && !deviceId.isBlank()) {
            return deviceId;
        }
        return "default";
    }

    private String getSuffix(String filename) {
        if (filename == null) {
            return ".img";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0) {
            return ".img";
        }
        return filename.substring(dot).toLowerCase();
    }

    private ParrotBehaviorResponse toResponse(ParrotBehaviorRecord r) {
        return ParrotBehaviorResponse.builder()
                .deviceId(r.getDeviceId())
                .parrotDetected(r.getParrotDetected())
                .parrotConfidence(r.getParrotConfidence())
                .behavior(r.getBehavior())
                .behaviorConfidence(r.getBehaviorConfidence())
                .imageUrl(r.getImageUrl())
                .checkedAt(r.getCheckedAt())
                .build();
    }
}

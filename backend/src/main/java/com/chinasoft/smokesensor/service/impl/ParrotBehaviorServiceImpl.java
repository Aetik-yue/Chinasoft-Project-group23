package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.common.BusinessException;
import com.chinasoft.smokesensor.config.ParrotProperties;
import com.chinasoft.smokesensor.dto.ParrotBehaviorResponse;
import com.chinasoft.smokesensor.entity.ParrotBehaviorRecord;
import com.chinasoft.smokesensor.repository.ParrotBehaviorRecordRepository;
import com.chinasoft.smokesensor.service.ParrotBehaviorService;
import com.chinasoft.smokesensor.service.parrot.ClipBehaviorProvider;
import com.chinasoft.smokesensor.service.parrot.ParrotDetectionProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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
        try {
            ParrotDetectionProvider.DetectionOutcome detection = parrotDetectionProvider.detect(snapshotPath);

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
                    .deviceId(resolveDeviceId(deviceId))
                    .imageUrl(snapshotPath)
                    .parrotDetected(detection.detected())
                    .parrotConfidence(detection.detected() ? detection.confidence() : null)
                    .behavior(behavior)
                    .behaviorConfidence(behaviorConfidence)
                    .checkedAt(java.time.LocalDateTime.now())
                    .build();
            parrotBehaviorRecordRepository.save(record);
            log.info("鹦鹉行为识别 deviceId={} detected={} behavior={}",
                    record.getDeviceId(), detection.detected(), behavior);
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

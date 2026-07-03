package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.common.BusinessException;
import com.chinasoft.smokesensor.config.VisionProperties;
import com.chinasoft.smokesensor.dto.VisionCheckResponse;
import com.chinasoft.smokesensor.entity.AlarmRecord;
import com.chinasoft.smokesensor.entity.VisionCheck;
import com.chinasoft.smokesensor.repository.AlarmRecordRepository;
import com.chinasoft.smokesensor.repository.VisionCheckRepository;
import com.chinasoft.smokesensor.service.VisionCheckService;
import com.chinasoft.smokesensor.service.vision.VisionDetectionProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * 视觉复核实现：查告警 → 取截图 → SmartJavaAI 推理 → 落库。
 *
 * <p>同一告警幂等（已复核则直接返回既有结果，避免重复跑模型）；
 * 模型推理较重，放在数据库事务之外执行，save 由仓库层自带事务兜底。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisionCheckServiceImpl implements VisionCheckService {

    private final AlarmRecordRepository alarmRecordRepository;
    private final VisionCheckRepository visionCheckRepository;
    private final VisionDetectionProvider visionDetectionProvider;
    private final VisionProperties visionProperties;

    @Override
    public VisionCheckResponse checkByAlarmId(String alarmId) {
        if (alarmId == null || alarmId.isBlank()) {
            throw new IllegalArgumentException("alarmId 不能为空");
        }
        // 幂等：同一告警若已复核，直接返回既有结果
        VisionCheck existing = visionCheckRepository.findByAlarmId(alarmId).orElse(null);
        if (existing != null) {
            return toResponse(existing);
        }
        AlarmRecord alarm = alarmRecordRepository.findByAlarmId(alarmId)
                .orElseThrow(() -> BusinessException.notFound("告警不存在: " + alarmId));
        String snapshotPath = resolveSnapshotPath();
        // 模型推理较重，放在数据库事务之外执行
        VisionDetectionProvider.DetectionResult detection = visionDetectionProvider.detect(snapshotPath);

        VisionCheck record = VisionCheck.builder()
                .alarmId(alarmId)
                .deviceId(alarm.getDeviceId())
                .imageUrl(snapshotPath)
                .aiResult(detection.result())
                .confidence(detection.confidence())
                .confirmed(false)
                .build();
        visionCheckRepository.save(record);
        log.info("视觉复核完成 alarmId={} result={} confidence={}",
                alarmId, detection.result(), detection.confidence());
        return toResponse(record);
    }

    private String resolveSnapshotPath() {
        String path = visionProperties.getSnapshotPath();
        if (path == null || path.isBlank()) {
            throw new BusinessException(5001,
                    "视觉复核截图未配置：请在 application.yml 设置 smartjavaai.vision.snapshot-path"
                            + "（摄像头截图文件路径，骨架阶段用本地图片占位）",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (!Files.exists(Path.of(path))) {
            throw new BusinessException(5001, "视觉复核截图文件不存在: " + path,
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        return path;
    }

    private VisionCheckResponse toResponse(VisionCheck record) {
        return VisionCheckResponse.builder()
                .alarmId(record.getAlarmId())
                .imageUrl(record.getImageUrl())
                .result(record.getAiResult())
                .confidence(record.getConfidence())
                .confirmed(Boolean.TRUE.equals(record.getConfirmed()))
                .build();
    }
}

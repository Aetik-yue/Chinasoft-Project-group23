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
 * 视觉复核业务实现。
 *
 * <p>当前流程是：根据 alarmId 查询告警、读取配置中的截图路径、调用视觉检测组件分析图片，
 * 最后把复核结果写入 vision_check 表。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisionCheckServiceImpl implements VisionCheckService {

    private final AlarmRecordRepository alarmRecordRepository;
    private final VisionCheckRepository visionCheckRepository;
    private final VisionDetectionProvider visionDetectionProvider;
    private final VisionProperties visionProperties;

    /**
     * 根据告警编号执行或读取视觉复核。
     *
     * <p>同一个 alarmId 如果已经存在复核记录，会直接返回已有结果，避免重复调用模型和重复写入数据库。
     */
    @Override
    public VisionCheckResponse checkByAlarmId(String alarmId) {
        if (alarmId == null || alarmId.isBlank()) {
            throw new IllegalArgumentException("alarmId 不能为空");
        }
        VisionCheck existing = visionCheckRepository.findByAlarmId(alarmId).orElse(null);
        if (existing != null) {
            return toResponse(existing);
        }
        AlarmRecord alarm = alarmRecordRepository.findByAlarmId(alarmId)
                .orElseThrow(() -> BusinessException.notFound("告警不存在: " + alarmId));
        String snapshotPath = resolveSnapshotPath();
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

    /**
     * 解析视觉复核截图路径，并校验配置和文件是否存在。
     */
    private String resolveSnapshotPath() {
        String path = visionProperties.getSnapshotPath();
        if (path == null || path.isBlank()) {
            throw new BusinessException(5001,
                    "视觉复核截图未配置：请在 application.yml 设置 smartjavaai.vision.snapshot-path"
                            + "（摄像头截图文件路径，骨架阶段可用本地图片占位）",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (!Files.exists(Path.of(path))) {
            throw new BusinessException(5001, "视觉复核截图文件不存在: " + path,
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        return path;
    }

    /**
     * 将视觉复核记录转换为接口响应对象。
     */
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

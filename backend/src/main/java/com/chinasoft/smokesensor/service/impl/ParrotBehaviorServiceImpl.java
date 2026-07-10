package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.common.BusinessException;
import com.chinasoft.smokesensor.config.ParrotProperties;
import com.chinasoft.smokesensor.dto.ParrotBehaviorResponse;
import com.chinasoft.smokesensor.entity.ParrotBehaviorRecord;
import com.chinasoft.smokesensor.repository.ParrotBehaviorRecordRepository;
import com.chinasoft.smokesensor.service.ParrotBehaviorService;
import com.chinasoft.smokesensor.dto.ParrotBox;
import com.chinasoft.smokesensor.service.parrot.ClipBehaviorProvider;
import com.chinasoft.smokesensor.service.parrot.ParrotDetectionProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 鹦鹉识别实现：截图 → YOLO 检测鹦鹉 → CLIP 行为+种类分类 → 落库。
 *
 * <p>分级可用：YOLO 未配置 → 5001；YOLO 已配置、CLIP 未配置 → 返回鹦鹉检测结果，behavior/species 留空；
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

    /** 实时分析节流缓存：按 deviceId 维护上次 CLIP 与落库时间。非 final，避免被 @RequiredArgsConstructor 当作注入 bean。 */
    private Map<String, RealtimeCache> realtimeCache = new ConcurrentHashMap<>();

    /** CLIP 行为/种类降采样间隔（毫秒），避免每帧重推理。 */
    private static final long CLIP_INTERVAL_MS = 2500L;

    /** DB 落库节流间隔（毫秒），避免实时流刷表。 */
    private static final long SAVE_INTERVAL_MS = 5000L;

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

    /** 共用分析逻辑：YOLO 检测 → CLIP 行为+种类分类 → 落库 → 返回。imageUrl 为记录里存的图片来源标识。 */
    private ParrotBehaviorResponse analyze(String deviceId, String imagePath, String imageUrl) {
        try {
            ParrotDetectionProvider.DetectionOutcome detection = parrotDetectionProvider.detect(imagePath);

            String behavior = null;
            Double behaviorConfidence = null;
            String species = null;
            Double speciesConfidence = null;
            if (detection.detected()) {
                try {
                    ClipBehaviorProvider.Classification behaviorCls =
                            clipBehaviorProvider.classifyBehavior(detection.crop());
                    behavior = behaviorCls.label();
                    behaviorConfidence = behaviorCls.confidence();
                } catch (BusinessException e) {
                    // CLIP 未配置时，仍返回鹦鹉检测结果，行为字段留空
                    log.warn("CLIP 行为识别跳过: {}", e.getMessage());
                }
                try {
                    ClipBehaviorProvider.Classification speciesCls =
                            clipBehaviorProvider.classifySpecies(detection.crop());
                    species = speciesCls.label();
                    speciesConfidence = speciesCls.confidence();
                } catch (BusinessException e) {
                    log.warn("CLIP 种类识别跳过: {}", e.getMessage());
                }
            }

            ParrotBehaviorRecord record = ParrotBehaviorRecord.builder()
                    .deviceId(deviceId)
                    .imageUrl(imageUrl)
                    .parrotDetected(detection.detected())
                    .parrotConfidence(detection.detected() ? detection.confidence() : null)
                    .behavior(behavior)
                    .behaviorConfidence(behaviorConfidence)
                    .species(species)
                    .speciesConfidence(speciesConfidence)
                    .checkedAt(java.time.LocalDateTime.now())
                    .build();
            parrotBehaviorRecordRepository.save(record);
            log.info("鹦鹉识别 deviceId={} detected={} behavior={} species={}",
                    deviceId, detection.detected(), behavior, species);
            return toResponse(record);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("鹦鹉识别失败", e);
            throw new BusinessException(5000, "鹦鹉识别失败: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 实时分析一帧：YOLO 出所有鹦鹉框（定位+计数），行为/种类按节流降采样调 CLIP，
     * DB 落库同样节流。异常字段（abnormal）由调用方（WebSocket Handler）填充。
     */
    @Override
    public ParrotBehaviorResponse analyzeRealtime(String imagePath, String deviceId) {
        String did = resolveDeviceId(deviceId);
        List<ParrotBox> boxes;
        try {
            boxes = parrotDetectionProvider.detectBoxes(imagePath);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("实时鹦鹉检测失败", e);
            throw new BusinessException(5000, "实时鹦鹉检测失败: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        boolean detected = boxes != null && !boxes.isEmpty();
        double mainConf = detected
                ? boxes.stream().mapToDouble(ParrotBox::getConfidence).max().orElse(0.0)
                : 0.0;
        String behavior = null;
        String species = null;
        Double bc = null;
        Double sc = null;
        if (detected) {
            RealtimeCache c = realtimeCache.computeIfAbsent(did, k -> new RealtimeCache());
            long now = System.currentTimeMillis();
            if (now - c.lastClipAt > CLIP_INTERVAL_MS) {
                try {
                    ParrotDetectionProvider.DetectionOutcome best = parrotDetectionProvider.detect(imagePath);
                    if (best.detected() && best.crop() != null) {
                        ClipBehaviorProvider.Classification bCls = clipBehaviorProvider.classifyBehavior(best.crop());
                        behavior = bCls.label();
                        bc = bCls.confidence();
                        ClipBehaviorProvider.Classification sCls = clipBehaviorProvider.classifySpecies(best.crop());
                        species = sCls.label();
                        sc = sCls.confidence();
                        c.behavior = behavior;
                        c.behaviorConfidence = bc;
                        c.species = species;
                        c.speciesConfidence = sc;
                        c.lastClipAt = now;
                    }
                } catch (BusinessException ex) {
                    log.warn("实时 CLIP 跳过: {}", ex.getMessage());
                }
            } else {
                behavior = c.behavior;
                bc = c.behaviorConfidence;
                species = c.species;
                sc = c.speciesConfidence;
            }
            if (now - c.lastSaveAt > SAVE_INTERVAL_MS) {
                saveRecord(did, imagePath, true, mainConf, behavior, bc, species, sc);
                c.lastSaveAt = now;
            }
        }
        return ParrotBehaviorResponse.builder()
                .deviceId(did)
                .parrotDetected(detected)
                .parrotConfidence(detected ? mainConf : null)
                .behavior(behavior)
                .behaviorConfidence(bc)
                .species(species)
                .speciesConfidence(sc)
                .boxes(boxes)
                .imageUrl(imagePath)
                .checkedAt(java.time.LocalDateTime.now())
                .build();
    }

    /** 落库一条鹦鹉识别记录（实时与单次共用）。 */
    private void saveRecord(String deviceId, String imageUrl, boolean detected, Double conf,
                            String behavior, Double bc, String species, Double sc) {
        parrotBehaviorRecordRepository.save(ParrotBehaviorRecord.builder()
                .deviceId(deviceId)
                .imageUrl(imageUrl)
                .parrotDetected(detected)
                .parrotConfidence(conf)
                .behavior(behavior)
                .behaviorConfidence(bc)
                .species(species)
                .speciesConfidence(sc)
                .build());
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

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getTodayStats(String deviceId) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(23, 59, 59);
        List<ParrotBehaviorRecord> records = parrotBehaviorRecordRepository
                .findByDeviceIdAndCheckedAtBetweenOrderByCheckedAtAsc(
                        deviceId, start, end);

        Map<String, Long> counts = new LinkedHashMap<>();
        for (ParrotBehaviorRecord r : records) {
            String b = (r.getBehavior() != null && !r.getBehavior().isBlank())
                    ? r.getBehavior() : "未识别";
            counts.merge(b, 1L, Long::sum);
        }

        List<Map<String, Object>> stats = new ArrayList<>();
        for (Map.Entry<String, Long> e : counts.entrySet()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("behavior", e.getKey());
            m.put("count", e.getValue());
            stats.add(m);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", today.toString());
        result.put("total", (long) records.size());
        result.put("stats", stats);
        return result;
    }

    private ParrotBehaviorResponse toResponse(ParrotBehaviorRecord r) {
        return ParrotBehaviorResponse.builder()
                .deviceId(r.getDeviceId())
                .parrotDetected(r.getParrotDetected())
                .parrotConfidence(r.getParrotConfidence())
                .behavior(r.getBehavior())
                .behaviorConfidence(r.getBehaviorConfidence())
                .species(r.getSpecies())
                .speciesConfidence(r.getSpeciesConfidence())
                .imageUrl(r.getImageUrl())
                .checkedAt(r.getCheckedAt())
                .build();
    }

    /** 实时分析每设备缓存：上次 CLIP 与落库时间 + 最近一次行为/种类结果。 */
    private static final class RealtimeCache {
        long lastClipAt = -1L;
        long lastSaveAt = -1L;
        String behavior;
        Double behaviorConfidence;
        String species;
        Double speciesConfidence;
    }
}

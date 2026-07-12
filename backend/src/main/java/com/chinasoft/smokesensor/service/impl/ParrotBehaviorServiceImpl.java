package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.common.BusinessException;
import com.chinasoft.smokesensor.common.UserContext;
import com.chinasoft.smokesensor.config.ParrotProperties;
import com.chinasoft.smokesensor.dto.ParrotBehaviorResponse;
import com.chinasoft.smokesensor.entity.ParrotBehaviorRecord;
import com.chinasoft.smokesensor.entity.PetProfile;
import com.chinasoft.smokesensor.repository.ParrotBehaviorRecordRepository;
import com.chinasoft.smokesensor.repository.PetProfileRepository;
import com.chinasoft.smokesensor.service.ParrotBehaviorService;
import com.chinasoft.smokesensor.dto.ParrotBox;
import com.chinasoft.smokesensor.client.QwenVisionClient;
import com.chinasoft.smokesensor.service.parrot.ClipBehaviorProvider;
import com.chinasoft.smokesensor.service.parrot.ParrotDetectionProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    /** 连续同类识别记录的最大间隔，间隔不超过该值时合并为同一次行为。 */
    private static final long BEHAVIOR_EVENT_GAP_SECONDS = 30L;

    private final ParrotProperties parrotProperties;
    private final ParrotDetectionProvider parrotDetectionProvider;
    private final ClipBehaviorProvider clipBehaviorProvider;
    private final ParrotBehaviorRecordRepository parrotBehaviorRecordRepository;
    private final PetProfileRepository petProfileRepository;
    private final QwenVisionClient qwenVisionClient;

    /** 实时分析节流缓存：按 deviceId 维护上次 CLIP 与落库时间。非 final，避免被 @RequiredArgsConstructor 当作注入 bean。 */
    private Map<String, RealtimeCache> realtimeCache = new ConcurrentHashMap<>();

    /** CLIP 行为/种类降采样间隔（毫秒），避免每帧重推理。 */
    private static final long CLIP_INTERVAL_MS = 2500L;

    /** DB 落库节流间隔（毫秒），避免实时流刷表。 */
    private static final long SAVE_INTERVAL_MS = 5000L;

    @Override
    public ParrotBehaviorResponse check(String deviceId, String petId) {
        PetProfile pet = requireOwnedPet(petId);
        String snapshotPath = resolveSnapshotPath();
        return analyze(resolvePetDeviceId(deviceId, pet), pet.getPetId(), snapshotPath, snapshotPath);
    }

    @Override
    public ParrotBehaviorResponse check(MultipartFile file, String deviceId, String petId) {
        PetProfile pet = requireOwnedPet(petId);
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
            return analyze(resolvePetDeviceId(deviceId, pet), pet.getPetId(), temp.toString(), imageUrl);
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
    private ParrotBehaviorResponse analyze(String deviceId, String petId, String imagePath, String imageUrl) {
        // 1. 优先使用通义千问视觉大模型识别 (如果 API 已配置且开启)
        if (qwenVisionClient != null && qwenVisionClient.isEnabled()) {
            try {
                byte[] bytes = Files.readAllBytes(Path.of(imagePath));
                String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
                QwenVisionClient.VisionResult visionResult = qwenVisionClient.analyzeUploadedImage(base64);
                
                ParrotBehaviorRecord record = ParrotBehaviorRecord.builder()
                        .deviceId(deviceId)
                        .petId(petId)
                        .imageUrl(imageUrl)
                        .parrotDetected(true)
                        .parrotConfidence(visionResult.confidence())
                        .behavior(visionResult.behavior())
                        .behaviorConfidence(visionResult.confidence())
                        .species(visionResult.species())
                        .speciesConfidence(visionResult.confidence())
                        .checkedAt(java.time.LocalDateTime.now())
                        .build();
                
                parrotBehaviorRecordRepository.save(record);
                log.info("通义千问 拍照识鹦鹉识别成功: deviceId={} species={} behavior={}",
                        deviceId, visionResult.species(), visionResult.behavior());
                return toResponse(record);
            } catch (Exception e) {
                log.warn("通义千问 识别异常，降级到本地 YOLO 和 CLIP 识别: {}", e.getMessage());
            }
        }

        // 2. 降级为 YOLO + CLIP 识别
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
                    .petId(petId)
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
    public ParrotBehaviorResponse analyzeRealtime(String imagePath, String deviceId, String petId) {
        String did = resolveDeviceId(deviceId);
        String resolvedPetId = requireText(petId, "petId 不能为空");
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
            RealtimeCache c = realtimeCache.computeIfAbsent(resolvedPetId, k -> new RealtimeCache());
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
                saveRecord(did, resolvedPetId, imagePath, true, mainConf, behavior, bc, species, sc);
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
    private void saveRecord(String deviceId, String petId, String imageUrl, boolean detected, Double conf,
                            String behavior, Double bc, String species, Double sc) {
        parrotBehaviorRecordRepository.save(ParrotBehaviorRecord.builder()
                .deviceId(deviceId)
                .petId(petId)
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
    public Map<String, Object> getTodayStats(String petId, String dateStr) {
        PetProfile pet = requireOwnedPet(petId);
        LocalDate date = parseReferenceDate(dateStr);
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        List<ParrotBehaviorRecord> records = parrotBehaviorRecordRepository
                .findByPetIdAndCheckedAtBetweenOrderByCheckedAtAsc(
                        pet.getPetId(), start, end);

        Map<String, Object> result = summarizeRecords(records);
        result.put("date", date.toString());
        return result;
    }

    /**
     * 按时间范围统计行为识别记录。
     * today 为今天截至当前时刻，day 为指定自然日，week 为周一至周日，month 为自然月。
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getBehaviorStats(String petId, String range, String dateStr) {
        String normalizedRange = range == null || range.isBlank()
                ? "today" : range.trim().toLowerCase();
        LocalDate referenceDate = parseReferenceDate(dateStr);
        LocalDateTime start;
        LocalDateTime end;

        switch (normalizedRange) {
            case "today" -> {
                referenceDate = LocalDate.now();
                start = referenceDate.atStartOfDay();
                end = LocalDateTime.now();
            }
            case "day" -> {
                start = referenceDate.atStartOfDay();
                end = referenceDate.atTime(LocalTime.MAX);
            }
            case "week" -> {
                LocalDate monday = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate sunday = referenceDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
                start = monday.atStartOfDay();
                end = sunday.atTime(LocalTime.MAX);
            }
            case "month" -> {
                LocalDate firstDay = referenceDate.withDayOfMonth(1);
                LocalDate lastDay = referenceDate.with(TemporalAdjusters.lastDayOfMonth());
                start = firstDay.atStartOfDay();
                end = lastDay.atTime(LocalTime.MAX);
            }
            default -> throw new IllegalArgumentException(
                    "range 仅支持 today、day、week、month");
        }

        LocalDateTime now = LocalDateTime.now();
        if (start.isAfter(now)) {
            throw new IllegalArgumentException("date 不能晚于今天");
        }
        // 当前日报、周报或月报尚未结束时，只统计到本次请求的当前时刻。
        if (end.isAfter(now)) {
            end = now;
        }

        PetProfile pet = requireOwnedPet(petId);
        List<ParrotBehaviorRecord> records = parrotBehaviorRecordRepository
                .findByPetIdAndCheckedAtBetweenOrderByCheckedAtAsc(
                        pet.getPetId(), start, end);

        Map<String, Object> result = summarizeRecords(records);
        result.put("range", normalizedRange);
        result.put("referenceDate", referenceDate.toString());
        result.put("startTime", start);
        result.put("endTime", end);
        return result;
    }

    /** 将高频识别记录合并为连续行为事件，同时保留原始记录数。 */
    private Map<String, Object> summarizeRecords(List<ParrotBehaviorRecord> records) {
        Map<String, Long> recordCounts = new LinkedHashMap<>();
        Map<String, Long> eventCounts = new LinkedHashMap<>();
        String previousBehavior = null;
        LocalDateTime previousCheckedAt = null;

        for (ParrotBehaviorRecord record : records) {
            String behavior = record.getBehavior() != null && !record.getBehavior().isBlank()
                    ? record.getBehavior() : "未识别";
            LocalDateTime checkedAt = record.getCheckedAt();
            recordCounts.merge(behavior, 1L, Long::sum);

            boolean startsNewEvent = previousBehavior == null
                    || !behavior.equals(previousBehavior)
                    || previousCheckedAt == null
                    || checkedAt == null;
            if (!startsNewEvent) {
                long gapSeconds = Duration.between(previousCheckedAt, checkedAt).getSeconds();
                startsNewEvent = gapSeconds < 0 || gapSeconds > BEHAVIOR_EVENT_GAP_SECONDS;
            }
            if (startsNewEvent) {
                eventCounts.merge(behavior, 1L, Long::sum);
            }

            previousBehavior = behavior;
            previousCheckedAt = checkedAt;
        }

        List<Map<String, Object>> stats = new ArrayList<>();
        for (Map.Entry<String, Long> entry : recordCounts.entrySet()) {
            long eventCount = eventCounts.getOrDefault(entry.getKey(), 0L);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("behavior", entry.getKey());
            item.put("count", eventCount);
            item.put("eventCount", eventCount);
            item.put("recordCount", entry.getValue());
            stats.add(item);
        }

        long totalEvents = eventCounts.values().stream().mapToLong(Long::longValue).sum();
        Map<String, Object> result = new LinkedHashMap<>();
        // total 保留原有的记录总数语义，避免影响已经使用该字段的前端。
        result.put("total", (long) records.size());
        result.put("totalRecords", (long) records.size());
        result.put("totalEvents", totalEvents);
        result.put("stats", stats);
        return result;
    }

    /** 解析接口日期参数，未传时使用今天，格式错误时返回明确的参数异常。 */
    private LocalDate parseReferenceDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("date 必须使用 yyyy-MM-dd 格式");
        }
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

    @Override
    @Transactional
    public void saveVlmRecord(String deviceId, String petId, QwenVisionClient.VisionResult result) {
        PetProfile pet = requireOwnedPet(petId);
        String devId = resolvePetDeviceId(deviceId, pet);
        ParrotBehaviorRecord record = ParrotBehaviorRecord.builder()
                .deviceId(devId)
                .petId(pet.getPetId())
                .imageUrl("vlm_simulation")
                .parrotDetected(true)
                .parrotConfidence(result.confidence())
                .behavior(result.behavior())
                .behaviorConfidence(result.confidence())
                .species(result.species())
                .speciesConfidence(result.confidence())
                .build();
        parrotBehaviorRecordRepository.save(record);
        log.info("VLM 模拟识别记录已保存: deviceId={}, behavior={}, species={}", devId, result.behavior(), result.species());
    }

    private PetProfile requireOwnedPet(String petId) {
        String normalized = requireText(petId, "petId 不能为空");
        Long userId = UserContext.requireUserId();
        return petProfileRepository.findByPetIdAndUserId(normalized, userId)
                .orElseThrow(() -> BusinessException.notFound("鹦鹉档案不存在: " + normalized));
    }

    private String resolvePetDeviceId(String requestedDeviceId, PetProfile pet) {
        String profileDeviceId = pet.getDeviceId();
        if (profileDeviceId != null && !profileDeviceId.isBlank()) {
            if (requestedDeviceId != null && !requestedDeviceId.isBlank()
                    && !profileDeviceId.equals(requestedDeviceId.trim())) {
                throw new IllegalArgumentException("deviceId 与鹦鹉绑定设备不一致");
            }
            return profileDeviceId;
        }
        return resolveDeviceId(requestedDeviceId);
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
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

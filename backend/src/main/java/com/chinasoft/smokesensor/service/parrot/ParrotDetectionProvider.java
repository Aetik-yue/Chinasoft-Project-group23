package com.chinasoft.smokesensor.service.parrot;

import ai.djl.modality.cv.Image;
import cn.smartjavaai.common.cv.SmartImageFactory;
import cn.smartjavaai.common.entity.DetectionInfo;
import cn.smartjavaai.common.entity.DetectionResponse;
import cn.smartjavaai.common.enums.DeviceEnum;
import cn.smartjavaai.objectdetection.config.DetectorModelConfig;
import cn.smartjavaai.objectdetection.enums.DetectorModelEnum;
import cn.smartjavaai.objectdetection.model.DetectorModel;
import cn.smartjavaai.objectdetection.model.ObjectDetectionModelFactory;
import com.chinasoft.smokesensor.common.BusinessException;
import com.chinasoft.smokesensor.config.ParrotProperties;
import com.chinasoft.smokesensor.dto.ParrotBox;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 鹦鹉检测封装：用 COCO 官方 YOLO 模型检测 bird 类，定位鹦鹉并裁剪出区域供 CLIP 行为分类。
 *
 * <p>模型懒加载，未配置时抛业务异常（5001），应用可正常启动。
 * 与火焰/烟雾的 VisionDetectionProvider 互不干扰（不同模型、不同配置）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParrotDetectionProvider {

    /** 检测结果：是否检测到鹦鹉、置信度、裁剪图（检测到时非空）。 */
    public record DetectionOutcome(boolean detected, double confidence, Image crop) {
    }

    private final ParrotProperties props;

    private volatile DetectorModel detectorModel;
    private volatile boolean engineInitialized = false;

    /**
     * 检测图片中的鹦鹉（bird 类），返回置信度最高那只的裁剪图。
     */
    public DetectionOutcome detect(String imagePath) {
        ensureConfigured();
        try {
            DetectorModel model = getOrLoadModel();
            Image image = SmartImageFactory.getInstance().fromFile(imagePath);
            DetectionResponse response = model.detect(image);
            return pickBestBird(response, image);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("鹦鹉检测失败", e);
            throw new BusinessException(5000, "鹦鹉检测失败: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 检测图片中的所有鹦鹉框（实时画框用），不做裁剪、不调 CLIP。
     *
     * @return 所有 bird 类框的坐标与置信度，未检测到时返回空列表
     */
    public List<ParrotBox> detectBoxes(String imagePath) {
        ensureConfigured();
        try {
            DetectorModel model = getOrLoadModel();
            Image image = SmartImageFactory.getInstance().fromFile(imagePath);
            DetectionResponse response = model.detect(image);
            return allBirdBoxes(response);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("鹦鹉检测失败", e);
            throw new BusinessException(5000, "鹦鹉检测失败: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void ensureConfigured() {
        if (!props.getDetection().isEnabled()) {
            throw new BusinessException(5001,
                    "鹦鹉检测未启用：请在 application.yml 设置 parrot.detection.enabled=true",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (props.getDetection().getModelPath() == null || props.getDetection().getModelPath().isBlank()) {
            throw new BusinessException(5001,
                    "鹦鹉检测模型未配置：请在 application.yml 设置 parrot.detection.model-path"
                            + "（COCO YOLO ONNX，含 bird 类）",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private DetectionOutcome pickBestBird(DetectionResponse response, Image image) {
        if (response == null || response.getDetectionInfoList() == null
                || response.getDetectionInfoList().isEmpty()) {
            return new DetectionOutcome(false, 0.0, null);
        }
        Set<String> birdClasses = props.getDetection().getBirdClasses().stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        DetectionInfo best = null;
        double bestScore = 0.0;
        for (DetectionInfo info : response.getDetectionInfoList()) {
            if (info.getObjectDetInfo() == null) {
                continue;
            }
            String className = info.getObjectDetInfo().getClassName();
            if (className == null || !birdClasses.contains(className.toLowerCase())) {
                continue;
            }
            double score = info.getScore();
            if (score > bestScore) {
                bestScore = score;
                best = info;
            }
        }
        if (best == null) {
            return new DetectionOutcome(false, 0.0, null);
        }
        Image crop = cropByRectangle(image, best);
        return new DetectionOutcome(true, bestScore, crop);
    }

    private List<ParrotBox> allBirdBoxes(DetectionResponse response) {
        List<ParrotBox> boxes = new ArrayList<>();
        if (response == null || response.getDetectionInfoList() == null) {
            return boxes;
        }
        Set<String> birdClasses = props.getDetection().getBirdClasses().stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        for (DetectionInfo info : response.getDetectionInfoList()) {
            if (info.getObjectDetInfo() == null) {
                continue;
            }
            String className = info.getObjectDetInfo().getClassName();
            if (className == null || !birdClasses.contains(className.toLowerCase())) {
                continue;
            }
            var rect = info.getDetectionRectangle();
            if (rect == null) {
                continue;
            }
            boxes.add(ParrotBox.builder()
                    .x((int) rect.getX())
                    .y((int) rect.getY())
                    .width((int) rect.getWidth())
                    .height((int) rect.getHeight())
                    .confidence(info.getScore())
                    .label(className)
                    .build());
        }
        return boxes;
    }

    private Image cropByRectangle(Image image, DetectionInfo info) {
        var rect = info.getDetectionRectangle();
        if (rect == null) {
            return image;
        }
        return image.getSubImage(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
    }

    private DetectorModel getOrLoadModel() {
        DetectorModel local = detectorModel;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (detectorModel == null) {
                initEngineOnce();
                var cfg = props.getDetection();
                DetectorModelConfig config = new DetectorModelConfig();
                config.setModelEnum(DetectorModelEnum.valueOf(cfg.getModelEnum()));
                config.setModelPath(cfg.getModelPath());
                config.setThreshold(cfg.getThreshold());
                config.setTopK(50);
                config.setDevice(DeviceEnum.CPU);
                log.info("加载鹦鹉检测模型: {} ({})", cfg.getModelPath(), cfg.getModelEnum());
                detectorModel = ObjectDetectionModelFactory.getInstance().getModel(config);
            }
            return detectorModel;
        }
    }

    private void initEngineOnce() {
        if (!engineInitialized) {
            SmartImageFactory.setEngine(SmartImageFactory.Engine.OPENCV);
            engineInitialized = true;
        }
    }
}

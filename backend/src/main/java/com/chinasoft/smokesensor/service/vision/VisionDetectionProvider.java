package com.chinasoft.smokesensor.service.vision;

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
import com.chinasoft.smokesensor.config.VisionProperties;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * SmartJavaAI 视觉复核封装：调用 YOLO 目标检测模型对摄像头截图做火焰/烟雾识别。
 *
 * <p>模型较重（含 ONNX Runtime / OpenCV 原生库），按需懒加载并缓存单例；
 * 未配置模型时抛业务异常（5001），保证应用可正常启动、其它接口不受影响。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VisionDetectionProvider {

    /** 检测结果：识别结论（fire_detected/smoke_detected/none）与最大置信度。 */
    public record DetectionResult(String result, double confidence) {
    }

    private final VisionProperties props;

    private volatile DetectorModel detectorModel;
    private volatile boolean engineInitialized = false;

    /**
     * 对指定图片执行火焰/烟雾检测。
     *
     * @param imagePath 图片文件路径
     * @return 检测结果
     */
    public DetectionResult detect(String imagePath) {
        if (!props.isEnabled()) {
            throw new BusinessException(5001,
                    "视觉复核未启用：请在 application.yml 设置 smartjavaai.vision.enabled=true",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (props.getModelPath() == null || props.getModelPath().isBlank()) {
            throw new BusinessException(5001,
                    "视觉复核模型未配置：请在 application.yml 设置 smartjavaai.vision.model-path"
                            + "（火焰/烟雾 YOLO .onnx 模型，默认 COCO 模型无 flame/smoke 类别）",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        try {
            DetectorModel model = getOrLoadModel();
            Image image = SmartImageFactory.getInstance().fromFile(imagePath);
            DetectionResponse response = model.detect(image);
            return mapResult(response);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("SmartJavaAI 视觉复核检测失败: image={}", imagePath, e);
            throw new BusinessException(5000, "视觉复核检测失败: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** 懒加载并缓存目标检测模型（线程安全）。 */
    private DetectorModel getOrLoadModel() {
        DetectorModel local = detectorModel;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (detectorModel == null) {
                initEngineOnce();
                DetectorModelConfig config = new DetectorModelConfig();
                config.setModelEnum(DetectorModelEnum.valueOf(props.getModelEnum()));
                config.setModelPath(props.getModelPath());
                config.setThreshold(props.getThreshold());
                config.setTopK(100);
                config.setDevice(DeviceEnum.CPU);
                if (props.getWidth() != null) {
                    config.putCustomParam("width", props.getWidth());
                }
                if (props.getHeight() != null) {
                    config.putCustomParam("height", props.getHeight());
                }
                config.putCustomParam("nmsThreshold", 0.5f);
                log.info("加载 SmartJavaAI 视觉复核模型: {} ({})", props.getModelPath(), props.getModelEnum());
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

    /** 把检测到的目标类别映射为 fire_detected/smoke_detected/none，明火优先级高于烟雾。 */
    private DetectionResult mapResult(DetectionResponse response) {
        if (response == null || response.getDetectionInfoList() == null
                || response.getDetectionInfoList().isEmpty()) {
            return new DetectionResult("none", 0.0);
        }
        Set<String> fireClasses = parseClasses(props.getFireClasses());
        Set<String> smokeClasses = parseClasses(props.getSmokeClasses());
        double bestFire = 0.0;
        double bestSmoke = 0.0;
        for (DetectionInfo info : response.getDetectionInfoList()) {
            if (info.getObjectDetInfo() == null) {
                continue;
            }
            String className = info.getObjectDetInfo().getClassName();
            if (className == null) {
                continue;
            }
            double score = info.getScore();
            if (fireClasses.contains(className)) {
                bestFire = Math.max(bestFire, score);
            } else if (smokeClasses.contains(className)) {
                bestSmoke = Math.max(bestSmoke, score);
            }
        }
        if (bestFire > 0) {
            return new DetectionResult("fire_detected", bestFire);
        }
        if (bestSmoke > 0) {
            return new DetectionResult("smoke_detected", bestSmoke);
        }
        return new DetectionResult("none", 0.0);
    }

    private Set<String> parseClasses(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }
}

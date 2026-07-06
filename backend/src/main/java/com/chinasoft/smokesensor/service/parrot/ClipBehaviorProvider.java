package com.chinasoft.smokesensor.service.parrot;

import ai.djl.modality.cv.Image;
import cn.smartjavaai.clip.config.ClipModelConfig;
import cn.smartjavaai.clip.enums.ClipModelEnum;
import cn.smartjavaai.clip.model.ClipModel;
import cn.smartjavaai.clip.model.ClipModelFactory;
import cn.smartjavaai.common.cv.SmartImageFactory;
import cn.smartjavaai.common.entity.R;
import cn.smartjavaai.common.enums.DeviceEnum;
import com.chinasoft.smokesensor.common.BusinessException;
import com.chinasoft.smokesensor.config.ParrotProperties;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * CLIP 零样本分类封装（行为 + 种类复用同一模型）。
 *
 * <p>用 OpenAI CLIP 把图片与一组文本描述比对，取最相似项，softmax 归一化得置信度。
 * 行为分类（进食/睡觉/…）与种类分类（虎皮/玄凤/…）共用已加载的 CLIP 模型，零训练。
 *
 * <p>CLIP 是单帧零样本模型：对视觉差异大的类别效果好，对相似类别（如虎皮 vs 牡丹）可能混淆。
 * OpenAI CLIP 英文训练，prompt 用英文，配平行中文 labels。模型懒加载，未配置时抛 5001。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClipBehaviorProvider {

    /** 分类结果：标签 + 置信度。 */
    public record Classification(String label, double confidence) {
    }

    private final ParrotProperties props;

    private volatile ClipModel clipModel;
    private volatile boolean engineInitialized = false;

    /** 行为零样本分类。 */
    public Classification classifyBehavior(Image crop) {
        return classify(crop, props.getBehavior().getPrompts(), props.getBehavior().getLabels());
    }

    /** 种类零样本分类。 */
    public Classification classifySpecies(Image crop) {
        return classify(crop, props.getSpecies().getPrompts(), props.getSpecies().getLabels());
    }

    /**
     * 通用零样本分类：图片 vs 一组文本 prompt，取最相似项。CLIP 未启用或未配置时抛 5001。
     */
    public Classification classify(Image crop, List<String> prompts, List<String> labels) {
        if (!props.getClip().isEnabled()) {
            throw new BusinessException(5001,
                    "CLIP 未启用：请在 application.yml 设置 parrot.clip.enabled=true",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (props.getClip().getModelPath() == null || props.getClip().getModelPath().isBlank()) {
            throw new BusinessException(5001,
                    "CLIP 模型未配置：请在 application.yml 设置 parrot.clip.model-path"
                            + "（clip.pt 或 jar://META-INF/models/clip/openai.zip）",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (prompts == null || prompts.isEmpty()) {
            throw new BusinessException(5001, "未配置 prompt",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        try {
            ClipModel model = getOrLoadModel();
            double[] scores = new double[prompts.size()];
            int bestIdx = 0;
            double bestScore = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < prompts.size(); i++) {
                R<Float> sim = model.compareTextAndImage(crop, prompts.get(i));
                double s = (sim != null && sim.isSuccess() && sim.getData() != null)
                        ? sim.getData()
                        : Double.NEGATIVE_INFINITY;
                scores[i] = s;
                if (s > bestScore) {
                    bestScore = s;
                    bestIdx = i;
                }
            }
            double confidence = softmaxConfidence(scores, bestIdx);
            String label = (labels != null && bestIdx < labels.size())
                    ? labels.get(bestIdx)
                    : prompts.get(bestIdx);
            return new Classification(label, confidence);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("CLIP 分类失败", e);
            throw new BusinessException(5000, "CLIP 分类失败: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private double softmaxConfidence(double[] scores, int target) {
        double max = Double.NEGATIVE_INFINITY;
        for (double s : scores) {
            if (s > max) {
                max = s;
            }
        }
        double sum = 0.0;
        for (double s : scores) {
            sum += Math.exp(s - max);
        }
        if (sum <= 0) {
            return 0.0;
        }
        return Math.exp(scores[target] - max) / sum;
    }

    private ClipModel getOrLoadModel() {
        ClipModel local = clipModel;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (clipModel == null) {
                initEngineOnce();
                ClipModelConfig config = new ClipModelConfig();
                config.setModelEnum(ClipModelEnum.OPENAI);
                config.setModelPath(props.getClip().getModelPath());
                config.setDevice(DeviceEnum.CPU);
                log.info("加载 CLIP 模型: {}", props.getClip().getModelPath());
                clipModel = ClipModelFactory.getInstance().getModel(config);
            }
            return clipModel;
        }
    }

    private void initEngineOnce() {
        if (!engineInitialized) {
            SmartImageFactory.setEngine(SmartImageFactory.Engine.OPENCV);
            engineInitialized = true;
        }
    }
}

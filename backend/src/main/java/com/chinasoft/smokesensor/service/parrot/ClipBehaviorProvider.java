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
 * CLIP 零样本行为分类封装：用 OpenAI CLIP 把鹦鹉裁剪图与一组行为描述文本比对，
 * 取最相似的行为，softmax 归一化得置信度。
 *
 * <p>CLIP 是单帧模型，本质是"看图猜行为"：对视觉差异大的行为（进食 vs 飞翔 vs 睡觉）效果好，
 * 对相似行为（梳理 vs 啄羽）可能混淆。OpenAI CLIP 英文训练，prompt 用英文，配平行中文 labels。
 *
 * <p>模型懒加载，未配置时抛业务异常（5001），应用可正常启动。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClipBehaviorProvider {

    /** 行为分类结果：行为标签 + 置信度。 */
    public record Classification(String behavior, double confidence) {
    }

    private final ParrotProperties props;

    private volatile ClipModel clipModel;
    private volatile boolean engineInitialized = false;

    /**
     * 对鹦鹉裁剪图做零样本行为分类。
     *
     * @param crop 鹦鹉裁剪图
     * @return 行为标签 + 置信度；CLIP 未启用时抛 5001
     */
    public Classification classify(Image crop) {
        if (!props.getClip().isEnabled()) {
            throw new BusinessException(5001,
                    "CLIP 行为识别未启用：请在 application.yml 设置 parrot.clip.enabled=true",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (props.getClip().getModelPath() == null || props.getClip().getModelPath().isBlank()) {
            throw new BusinessException(5001,
                    "CLIP 模型未配置：请在 application.yml 设置 parrot.clip.model-path"
                            + "（clip.pt 或 jar://META-INF/models/clip/openai.zip）",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        List<String> prompts = props.getBehavior().getPrompts();
        List<String> labels = props.getBehavior().getLabels();
        if (prompts == null || prompts.isEmpty()) {
            throw new BusinessException(5001, "未配置行为 prompt（parrot.behavior.prompts）",
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
            String behavior = (labels != null && bestIdx < labels.size())
                    ? labels.get(bestIdx)
                    : prompts.get(bestIdx);
            return new Classification(behavior, confidence);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("CLIP 行为分类失败", e);
            throw new BusinessException(5000, "CLIP 行为分类失败: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** 把原始相似度分数经 softmax 归一化，取目标下标的概率作为置信度。 */
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

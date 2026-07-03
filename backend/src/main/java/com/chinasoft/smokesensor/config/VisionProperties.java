package com.chinasoft.smokesensor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SmartJavaAI 视觉复核配置（前缀 smartjavaai.vision）。
 * 默认 enabled=false，未提供模型时 /api/vision/check 返回 5001，应用仍可正常启动。
 */
@Data
@Component
@ConfigurationProperties(prefix = "smartjavaai.vision")
public class VisionProperties {

    /** 是否启用视觉复核（需提供模型文件后开启）。 */
    private boolean enabled = false;

    /** 火焰/烟雾自定义 YOLO 模型文件路径（.onnx），需自备。 */
    private String modelPath;

    /** 模型枚举，默认 YOLOV12_CUSTOM_ONNX（自定义模型），详见 SmartJavaAI 文档。 */
    private String modelEnum = "YOLOV12_CUSTOM_ONNX";

    /** 置信度阈值。 */
    private float threshold = 0.5f;

    /** 模型训练图片宽度。 */
    private Integer width = 640;

    /** 模型训练图片高度。 */
    private Integer height = 640;

    /** 摄像头截图文件路径（骨架阶段无真实摄像头，用本地图片占位）。 */
    private String snapshotPath;

    /** 视为「明火」的类别名（逗号分隔），取决于自定义模型的 synset。 */
    private String fireClasses = "fire,flame";

    /** 视为「烟雾」的类别名（逗号分隔）。 */
    private String smokeClasses = "smoke";
}

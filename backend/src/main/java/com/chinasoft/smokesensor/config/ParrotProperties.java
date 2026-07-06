package com.chinasoft.smokesensor.config;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 鹦鹉检测 + CLIP 行为识别配置（前缀 parrot）。
 * 默认 enabled=false，未配置模型时 /api/parrot/behavior 返回 5001，应用仍可正常启动。
 */
@Data
@Component
@ConfigurationProperties(prefix = "parrot")
public class ParrotProperties {

    /** 摄像头截图文件路径（骨架阶段无真实摄像头，用本地图片占位）。 */
    private String snapshotPath;

    private Detection detection = new Detection();
    private Clip clip = new Clip();
    private Behavior behavior = new Behavior();

    @Data
    public static class Detection {
        private boolean enabled = false;
        /** COCO YOLO 官方 ONNX 模型路径（含 bird 类）。 */
        private String modelPath;
        /** 模型枚举：YOLOV8_OFFICIAL_ONNX / YOLOV11_OFFICIAL_ONNX / YOLOV12_OFFICIAL_ONNX。 */
        private String modelEnum = "YOLOV8_OFFICIAL_ONNX";
        private float threshold = 0.4f;
        /** 视为鹦鹉的类别名（COCO 里是 bird）。 */
        private List<String> birdClasses = List.of("bird");
    }

    @Data
    public static class Clip {
        private boolean enabled = false;
        /** CLIP 模型路径：clip.pt，或 jar://META-INF/models/clip/openai.zip（先试 jar:// 自带）。 */
        private String modelPath;
    }

    @Data
    public static class Behavior {
        /** CLIP 行为描述 prompt（英文，OpenAI CLIP 英文训练），与 labels 一一对应。 */
        private List<String> prompts = List.of(
                "a photo of a parrot eating food",
                "a photo of a parrot drinking water",
                "a photo of a parrot preening its feathers",
                "a photo of a parrot flying",
                "a photo of a parrot climbing",
                "a photo of a parrot sleeping"
        );
        /** 中文行为标签，与 prompts 一一对应。 */
        private List<String> labels = List.of(
                "进食", "饮水", "梳理羽毛", "飞翔", "攀爬", "睡觉"
        );
    }
}

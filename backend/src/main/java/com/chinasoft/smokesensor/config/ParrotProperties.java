package com.chinasoft.smokesensor.config;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 鹦鹉检测 + CLIP 零样本分类配置（前缀 parrot）。
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
    private Species species = new Species();
    private Abnormal abnormal = new Abnormal();

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
        /** 行为描述 prompt（英文），与 labels 一一对应。 */
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

    @Data
    public static class Species {
        /** 种类描述 prompt（英文），与 labels 一一对应。 */
        private List<String> prompts = List.of(
                "a photo of a budgerigar parrot",
                "a photo of a cockatiel",
                "a photo of a lovebird",
                "a photo of a green-cheeked conure",
                "a photo of a sun conure",
                "a photo of an african grey parrot",
                "a photo of a macaw",
                "a photo of a cockatoo",
                "a photo of an amazon parrot",
                "a photo of an eclectus parrot"
        );
        /** 中文种类标签，与 prompts 一一对应。 */
        private List<String> labels = List.of(
                "虎皮鹦鹉", "玄凤鹦鹉", "牡丹鹦鹉", "绿颊锥尾鹦鹉", "太阳锥尾鹦鹉",
                "非洲灰鹦鹉", "金刚鹦鹉", "葵花鹦鹉", "亚马逊鹦鹉", "折衷鹦鹉"
        );
    }

    @Data
    public static class Abnormal {
        /** 连续无检测超过该秒数 → 失踪/逃逸。 */
        private int missingSeconds = 30;
        /** 检测到但框质心位移低于阈值持续超过该秒数 → 可能受伤/昏迷。 */
        private int staticSeconds = 60;
        /** 期望鹦鹉数，与实际检测数不符 → 数量异常。 */
        private int expectedCount = 1;
        /** 行为持续为"梳理羽毛"超过该秒数 → 疑似拔羽。 */
        private int pluckingSeconds = 20;
        /** 框质心位移小于该像素值视为"不动"。 */
        private double staticMoveThreshold = 15.0;
    }
}

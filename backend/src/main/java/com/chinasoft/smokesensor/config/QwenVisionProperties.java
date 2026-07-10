package com.chinasoft.smokesensor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 通义千问视觉模型配置，前缀 qwen.vision。
 *
 * <p>3D 模拟模式通过 Qwen-VL 零样本识别鹦鹉种类与行为，默认 enabled=false。
 * 获取 API Key：阿里云百炼 → DashScope → 创建 Key → 设环境变量 QWEN_API_KEY。
 */
@Data
@Component
@ConfigurationProperties(prefix = "qwen.vision")
public class QwenVisionProperties {

    /** 是否启用 Qwen-VL 视觉复核 */
    private boolean enabled = false;

    /** DashScope OpenAI 兼容 API 地址 */
    private String baseUrl;

    /** DashScope API Key，通过环境变量注入 */
    private String apiKey;

    /** 模型名，默认 qwen-vl-max */
    private String model = "qwen-vl-max";
}

package com.chinasoft.smokesensor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MaxKB 智能问答配置，前缀 qq.maxkb。
 *
 * <p>MaxKB 为外部独立部署的智能体服务，提供警情 / 养护知识 RAG 问答。
 * 默认 enabled=false，未配置时 agent 的自然语言兜底问答返回"未启用"提示，不影响其他功能。
 */
@Data
@Component
@ConfigurationProperties(prefix = "qq.maxkb")
public class MaxKBProperties {

    /** 是否启用 MaxKB 智能问答。 */
    private boolean enabled = false;

    /** MaxKB 服务地址，如 http://47.108.58.107:8080。 */
    private String baseUrl;

    /** MaxKB 应用 ID。 */
    private String appId;

    /** MaxKB API Key。 */
    private String apiKey;
}

package com.chinasoft.smokesensor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 大模型（DeepSeek，OpenAI 兼容）配置，前缀 qq.llm。
 *
 * <p>用于 function calling agent：大模型理解用户自然语言意图，自动调用后端查询 / 控制工具，
 * 替代关键词规则识别，实现更灵活的自然语言交互。
 *
 * <p>默认 enabled=false，未配置 api-key 时 agent 静默退化为规则模式，应用正常启动。
 * 启用后优先于规则与 MaxKB（三层降级：LLM -> MaxKB -> 规则）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "qq.llm")
public class LlmProperties {

    /** 是否启用 LLM agent。 */
    private boolean enabled = false;

    /** DeepSeek API 地址（OpenAI 兼容）。 */
    private String baseUrl = "https://api.deepseek.com";

    /** DeepSeek API Key（必填，未填则视为未启用）。 */
    private String apiKey;

    /** 模型名，deepseek-chat 支持function calling。 */
    private String model = "deepseek-chat";

    /** function calling 最大轮数（防止无限循环）。 */
    private int maxRounds = 5;

    /** Agent 系统提示词，定义角色、能力与规则。 */
    private String systemPrompt = """
            你是「智慧宠物烟感安全系统」的 QQ 助手，负责帮用户查询宠物笼舍环境数据、控制联动设备、管理鹦鹉宠物档案和解答养护问题。

            能力：
            - 查询实时烟雾浓度/温度/湿度、设备在线状态、今日告警统计、最近告警记录
            - 控制蜂鸣器/报警灯/总开关（控制会触发二次确认，需用户回复"确认"才执行）
            - 管理鹦鹉（查询列表、查看详情、新建档案、记体重、记病历、记账本）
            - 综合近期环境、体重、病历和行为数据生成定制化的宠物成长日报/健康体检报告
            - 推荐用户所在城市或指定城市的异宠/鸟类专科宠物医院
            - 关联/绑定系统账号（用户名和密码），实现多用户数据隔离与个性化管理

            规则：
            - 涉及具体数据（浓度、告警等）时必须调用对应工具查询，不要编造数字
            - 控制设备时调用 control_device 工具，系统会要求用户二次确认，你只需转达确认提示
            - 关联账号时调用 bind_account 工具（传入 username 和 password）
            - 回复简洁，用中文，可适当使用 emoji
            - 用户回复"确认"/"取消"由系统拦截执行待确认操作，你不要自行调用工具处理
            - 如果用户询问如何绑定或关联系统账号，请友好地引导其使用如下方式：
              “您可以发送『绑定账号 [用户名] [密码]』或者直接告诉我：『帮我绑定系统账号，用户名是xxx，密码是xxx』来关联您的账号。”""";
}

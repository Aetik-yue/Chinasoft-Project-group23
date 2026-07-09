package com.chinasoft.smokesensor.service.qq;

import com.chinasoft.smokesensor.client.LlmClient;
import com.chinasoft.smokesensor.config.LlmProperties;
import com.chinasoft.smokesensor.service.AlarmService;
import com.chinasoft.smokesensor.service.DeviceService;
import com.chinasoft.smokesensor.service.SmokeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * LLM function calling agent 核心：工具 schema 定义、工具执行、多轮编排。
 *
 * <p>{@link #runAgent} 接收用户消息，构造 system + user 消息发给 DeepSeek，若模型返回
 * {@code tool_calls} 则执行对应工具（查询 / 控制），把结果作为 tool 消息回填，再次调用模型，
 * 直到模型返回最终文本回复（无 tool_calls）或达到最大轮数。
 *
 * <p>工具集（均操作默认设备 SMK-001）：
 * <ul>
 *   <li>{@code get_realtime_status} - 实时浓度温湿度</li>
 *   <li>{@code get_device_status} - 设备在线状态</li>
 *   <li>{@code get_alarm_stat} - 今日告警统计</li>
 *   <li>{@code get_recent_alarms} - 最近告警列表</li>
 *   <li>{@code control_device} - 控制设备（target + action，触发二次确认）</li>
 * </ul>
 *
 * <p>控制工具不直接执行：调用 {@link OneBotControlService#requestControl} 暂存 pendingOp 并返回
 * 确认提示，由模型转达给用户；用户回复"确认"时由 {@code OneBotMessageRouter} 规则拦截执行，
 * 实现智能与安全兼顾。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentToolService {

    private static final String DEFAULT_DEVICE_ID = "SMK-001";
    private static final int RECENT_ALARM_LIMIT = 5;

    private final LlmClient llmClient;
    private final LlmProperties llmProperties;
    private final SmokeService smokeService;
    private final DeviceService deviceService;
    private final AlarmService alarmService;
    private final OneBotControlService controlService;

    /** JSON 序列化 / 参数解析用 ObjectMapper（自建，避免与 Redis ObjectMapper 歧义）。 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 运行 agent：多轮 function calling，返回最终回复文本。
     *
     * @param userId      用户 QQ 号（用于控制 pendingOp 绑定）
     * @param userMessage 用户消息原文
     * @return 最终回复，或 null（LLM 未启用）
     */
    public String runAgent(long userId, String userMessage) {
        if (!llmClient.isEnabled()) {
            return null;
        }
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(message("system", llmProperties.getSystemPrompt()));
        messages.add(message("user", userMessage));

        List<Map<String, Object>> tools = getToolSchemas();
        for (int round = 0; round < llmProperties.getMaxRounds(); round++) {
            Map<String, Object> assistant = llmClient.chat(messages, tools);
            if (assistant == null) {
                return "🤔 模型无响应，请稍后重试。";
            }
            // 把 assistant 消息原样回填到对话（含 tool_calls 时模型需要看到自己的调用）
            messages.add(assistant);

            Object toolCalls = assistant.get("tool_calls");
            if (!(toolCalls instanceof List<?> calls) || calls.isEmpty()) {
                // 无 tool_calls，返回 content 作为最终回复
                Object content = assistant.get("content");
                return content == null ? "" : content.toString();
            }
            // 执行每个 tool_call，结果以 tool 角色消息回填
            for (Object call : calls) {
                if (!(call instanceof Map<?, ?> tc)) {
                    continue;
                }
                String toolCallId = String.valueOf(tc.get("id"));
                Object functionObj = tc.get("function");
                if (!(functionObj instanceof Map<?, ?> func)) {
                    continue;
                }
                String name = String.valueOf(func.get("name"));
                String arguments = String.valueOf(func.get("arguments"));
                log.info("agent 工具调用: userId={}, tool={}, args={}", userId, name, arguments);
                String result = executeTool(userId, name, arguments);
                messages.add(toolMessage(toolCallId, name, result));
            }
        }
        return "🤔 处理轮数超限，请简化问题。";
    }

    // ========================================================================
    // 工具 schema 定义（OpenAI function 格式）
    // ========================================================================

    private List<Map<String, Object>> getToolSchemas() {
        return List.of(
                functionTool("get_realtime_status",
                        "查询当前烟雾浓度、温度、湿度、风险等级、告警状态和更新时间", emptyParams()),
                functionTool("get_device_status",
                        "查询设备在线状态和最后心跳时间", emptyParams()),
                functionTool("get_alarm_stat",
                        "查询今日告警次数和较昨日的变化率", emptyParams()),
                functionTool("get_recent_alarms",
                        "查询最近5条告警记录（含等级、浓度、时间、状态）", emptyParams()),
                functionTool("control_device",
                        "控制联动设备，会触发二次确认（蜂鸣器/报警灯/总开关）", controlParams())
        );
    }

    private Map<String, Object> functionTool(String name, String desc, Map<String, Object> params) {
        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", name);
        function.put("description", desc);
        function.put("parameters", params);
        return Map.of("type", "function", "function", function);
    }

    private Map<String, Object> emptyParams() {
        return Map.of("type", "object", "properties", Map.of());
    }

    private Map<String, Object> controlParams() {
        Map<String, Object> target = Map.of(
                "type", "string",
                "enum", List.of("buzzer", "alarm_light", "switch"),
                "description", "控制对象：buzzer蜂鸣器 / alarm_light报警灯 / switch总开关");
        Map<String, Object> action = Map.of(
                "type", "string",
                "enum", List.of("on", "off"),
                "description", "动作：on开启 / off关闭");
        return Map.of(
                "type", "object",
                "properties", Map.of("target", target, "action", action),
                "required", List.of("target", "action"));
    }

    // ========================================================================
    // 工具执行
    // ========================================================================

    private String executeTool(long userId, String name, String arguments) {
        try {
            return switch (name) {
                case "get_realtime_status" -> toJson(smokeService.getRealtimeSmoke(DEFAULT_DEVICE_ID));
                case "get_device_status" -> toJson(deviceService.getDeviceStatus(DEFAULT_DEVICE_ID));
                case "get_alarm_stat" -> toJson(alarmService.getTodayStat());
                case "get_recent_alarms" -> toJson(alarmService.getAlarmList(RECENT_ALARM_LIMIT));
                case "control_device" -> controlDevice(userId, arguments);
                default -> "未知工具：" + name;
            };
        } catch (Exception e) {
            log.warn("工具执行失败: tool={}, reason={}", name, e.getMessage());
            return "工具执行失败：" + e.getMessage();
        }
    }

    /** 解析 control_device 参数，调用 OneBotControlService 发起二次确认。 */
    private String controlDevice(long userId, String arguments) {
        try {
            JsonNode node = objectMapper.readTree(arguments);
            String target = node.has("target") ? node.get("target").asText() : null;
            String action = node.has("action") ? node.get("action").asText() : null;
            return controlService.requestControl(userId, target, action);
        } catch (Exception e) {
            return "参数解析失败：" + e.getMessage();
        }
    }

    // ========================================================================
    // 消息构造辅助
    // ========================================================================

    private Map<String, Object> message(String role, String content) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }

    private Map<String, Object> toolMessage(String toolCallId, String name, String content) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("role", "tool");
        m.put("tool_call_id", toolCallId);
        m.put("name", name);
        m.put("content", content);
        return m;
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return String.valueOf(o);
        }
    }
}

package com.chinasoft.smokesensor.service.qq;

import com.chinasoft.smokesensor.client.LlmClient;
import com.chinasoft.smokesensor.config.LlmProperties;
import com.chinasoft.smokesensor.service.AlarmService;
import com.chinasoft.smokesensor.service.DeviceOnlineStatusService;
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
 * {@code tool_calls} 则执行对应工具，把结果作为 tool 消息回填，再次调用模型，
 * 直到模型返回最终文本回复（无 tool_calls）或达到最大轮数。
 *
 * <p>工具集（不再写死设备，模型可先查设备列表再操作指定设备）：
 * <ul>
 *   <li>{@code list_devices} - 查询数据库中所有设备列表</li>
 *   <li>{@code get_realtime_status} - 实时浓度温湿度（可选 device_id）</li>
 *   <li>{@code get_device_status} - 设备在线状态（可选 device_id）</li>
 *   <li>{@code get_alarm_stat} - 今日告警统计（全局）</li>
 *   <li>{@code get_recent_alarms} - 最近告警记录（可选 device_id 过滤）</li>
 *   <li>{@code control_device} - 控制设备（可选 device_id + target + action，触发二次确认）</li>
 * </ul>
 *
 * <p>设备解析：模型未传 device_id 时，自动取数据库中最近活跃设备（基于 smoke_data 最新真实数据）。
 * 模型可通过 list_devices 查看所有设备，再针对性查询 / 控制某台设备。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentToolService {

    /** 最近告警列表查询条数。 */
    private static final int RECENT_ALARM_LIMIT = 5;

    private final LlmClient llmClient;
    private final LlmProperties llmProperties;
    private final SmokeService smokeService;
    private final DeviceService deviceService;
    private final AlarmService alarmService;
    private final OneBotControlService controlService;
    private final DeviceOnlineStatusService deviceOnlineStatusService;

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
            messages.add(assistant);

            Object toolCalls = assistant.get("tool_calls");
            if (!(toolCalls instanceof List<?> calls) || calls.isEmpty()) {
                Object content = assistant.get("content");
                return content == null ? "" : content.toString();
            }
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
                functionTool("list_devices",
                        "查询数据库中所有烟感设备列表，含设备编号、名称、位置、在线状态、当前浓度、风险等级等", emptyParams()),
                functionTool("get_realtime_status",
                        "查询指定设备的实时烟雾浓度、温度、湿度、风险等级、告警状态和更新时间", optionalDeviceIdParams()),
                functionTool("get_device_status",
                        "查询指定设备的在线状态和最后心跳时间", optionalDeviceIdParams()),
                functionTool("get_alarm_stat",
                        "查询今日告警次数和较昨日的变化率（全局统计，不区分设备）", emptyParams()),
                functionTool("get_recent_alarms",
                        "查询最近告警记录，可按设备过滤", optionalDeviceIdParams()),
                functionTool("control_device",
                        "控制指定设备的联动设备（蜂鸣器/报警灯/总开关），会触发二次确认", controlParams())
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

    /** 工具参数含可选 device_id（不传则后端自动用最近活跃设备）。 */
    private Map<String, Object> optionalDeviceIdParams() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "device_id", Map.of(
                                "type", "string",
                                "description", "设备编号，可选。不传则自动用最近活跃设备；可先调用 list_devices 查询所有设备")));
    }

    private Map<String, Object> controlParams() {
        Map<String, Object> deviceId = Map.of(
                "type", "string",
                "description", "设备编号，可选。不传则自动用最近活跃设备");
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
                "properties", Map.of("device_id", deviceId, "target", target, "action", action),
                "required", List.of("target", "action"));
    }

    // ========================================================================
    // 工具执行
    // ========================================================================

    private String executeTool(long userId, String name, String arguments) {
        try {
            JsonNode args = parseArgs(arguments);
            return switch (name) {
                case "list_devices" -> toJson(deviceService.listDevices(null, null));
                case "get_realtime_status" -> {
                    String devId = resolveDeviceId(args);
                    if (devId == null) {
                        yield "未找到活跃设备，请先调用 list_devices 查看可用设备";
                    }
                    yield toJson(smokeService.getRealtimeSmoke(devId));
                }
                case "get_device_status" -> {
                    String devId = resolveDeviceId(args);
                    if (devId == null) {
                        yield "未找到活跃设备，请先调用 list_devices 查看可用设备";
                    }
                    yield toJson(deviceService.getDeviceStatus(devId));
                }
                case "get_alarm_stat" -> toJson(alarmService.getTodayStat());
                case "get_recent_alarms" -> {
                    String devId = resolveDeviceId(args);
                    if (devId != null) {
                        yield toJson(alarmService.getAlarmsByDeviceId(devId).stream()
                                .limit(RECENT_ALARM_LIMIT).toList());
                    }
                    yield toJson(alarmService.getAlarmList(RECENT_ALARM_LIMIT));
                }
                case "control_device" -> {
                    String devId = resolveDeviceId(args);
                    if (devId == null) {
                        yield "未找到活跃设备，请先调用 list_devices 查看可用设备";
                    }
                    String target = args != null && args.has("target") ? args.get("target").asText() : null;
                    String action = args != null && args.has("action") ? args.get("action").asText() : null;
                    yield controlService.requestControl(userId, devId, target, action);
                }
                default -> "未知工具：" + name;
            };
        } catch (Exception e) {
            log.warn("工具执行失败: tool={}, reason={}", name, e.getMessage());
            return "工具执行失败：" + e.getMessage();
        }
    }

    /**
     * 解析设备编号：优先用模型传入的 device_id，未传则回退到数据库中最近活跃设备。
     *
     * @return 设备编号，或 null（数据库无任何设备数据）
     */
    private String resolveDeviceId(JsonNode args) {
        if (args != null && args.has("device_id")) {
            String id = args.get("device_id").asText();
            if (!id.isBlank() && !"null".equalsIgnoreCase(id)) {
                return id;
            }
        }
        return deviceOnlineStatusService.getLatestStatus()
                .map(s -> s.latestData().getDeviceId())
                .orElse(null);
    }

    private JsonNode parseArgs(String arguments) {
        try {
            return objectMapper.readTree(arguments);
        } catch (Exception e) {
            log.warn("工具参数解析失败: arguments={}, reason={}", arguments, e.getMessage());
            return null;
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

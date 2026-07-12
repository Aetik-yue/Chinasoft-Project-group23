package com.chinasoft.smokesensor.service.qq;

import com.chinasoft.smokesensor.client.LlmClient;
import com.chinasoft.smokesensor.client.MaxKBClient;
import com.chinasoft.smokesensor.config.AlarmWebSocketSessionManager;
import com.chinasoft.smokesensor.config.LlmProperties;
import com.chinasoft.smokesensor.service.AlarmService;
import com.chinasoft.smokesensor.service.DeviceOnlineStatusService;
import com.chinasoft.smokesensor.service.DeviceService;
import com.chinasoft.smokesensor.service.SmokeService;
import com.chinasoft.smokesensor.service.PetProfileService;
import com.chinasoft.smokesensor.service.PetWeightService;
import com.chinasoft.smokesensor.service.PetMedicalRecordService;
import com.chinasoft.smokesensor.service.PetLedgerRecordService;
import com.chinasoft.smokesensor.service.EnvironmentHistoryService;
import com.chinasoft.smokesensor.service.ParrotBehaviorService;
import com.chinasoft.smokesensor.repository.SysUserRepository;
import com.chinasoft.smokesensor.repository.PetProfileRepository;
import com.chinasoft.smokesensor.repository.UserPreferenceRepository;
import com.chinasoft.smokesensor.repository.AlarmRecordRepository;
import com.chinasoft.smokesensor.repository.PetMediaRecordRepository;
import com.chinasoft.smokesensor.repository.PetWeightRecordRepository;
import com.chinasoft.smokesensor.repository.PetMedicalRecordRepository;
import com.chinasoft.smokesensor.repository.PetLedgerRecordRepository;
import com.chinasoft.smokesensor.entity.SysUser;
import com.chinasoft.smokesensor.entity.PetProfile;
import com.chinasoft.smokesensor.entity.UserPreference;
import com.chinasoft.smokesensor.entity.AlarmRecord;
import com.chinasoft.smokesensor.entity.PetMediaRecord;
import com.chinasoft.smokesensor.entity.PetWeightRecord;
import com.chinasoft.smokesensor.entity.PetMedicalRecord;
import com.chinasoft.smokesensor.entity.PetLedgerRecord;
import com.chinasoft.smokesensor.dto.*;
import com.chinasoft.smokesensor.common.UserContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
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
    private final MaxKBClient maxKBClient;
    private final AlarmWebSocketSessionManager alarmWebSocketSessionManager;
    private final LlmProperties llmProperties;
    private final SmokeService smokeService;
    private final DeviceService deviceService;
    private final AlarmService alarmService;
    private final OneBotControlService controlService;
    private final DeviceOnlineStatusService deviceOnlineStatusService;
    private final PetProfileService petProfileService;
    private final PetWeightService petWeightService;
    private final PetMedicalRecordService petMedicalRecordService;
    private final PetLedgerRecordService petLedgerRecordService;
    private final SysUserRepository sysUserRepository;
    private final PetProfileRepository petProfileRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final AlarmRecordRepository alarmRecordRepository;
    private final PetMediaRecordRepository petMediaRecordRepository;
    private final PetWeightRecordRepository petWeightRecordRepository;
    private final PetMedicalRecordRepository petMedicalRecordRepository;
    private final PetLedgerRecordRepository petLedgerRecordRepository;
    private final EnvironmentHistoryService environmentHistoryService;
    private final ParrotBehaviorService parrotBehaviorService;

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
                        "控制指定设备的联动设备（蜂鸣器/报警灯/总开关），会触发二次确认", controlParams()),
                functionTool("list_parrots",
                        "查询当前拥有的所有鹦鹉宠物档案列表，包含基本信息、体重、状态等", emptyParams()),
                functionTool("get_parrot_details",
                        "查询指定鹦鹉的详细档案，包括生日、羽毛颜色、是否绝育、备注、以及所属设备等", petIdentifierParams()),
                functionTool("create_parrot",
                        "创建/注册一只新的鹦鹉宠物档案", createParrotParams()),
                functionTool("add_parrot_weight",
                        "为指定鹦鹉录入/记录体重数值（单位：克）", addParrotWeightParams()),
                functionTool("add_parrot_medical",
                        "为指定鹦鹉录入看病就诊或日常体检的病历记录", addParrotMedicalParams()),
                functionTool("add_parrot_ledger",
                        "为指定鹦鹉录入一笔养护消费记账记录", addParrotLedgerParams()),
                functionTool("generate_daily_growth_report",
                        "为指定鹦鹉综合近24小时温湿度环境历史、体重变化趋势、今日行为分析、就诊病历生成一份定制化的健康评估成长日报与养护指导建议", petIdentifierParams()),
                functionTool("search_nearby_pet_hospitals",
                        "搜索指定城市或用户所在城市的宠物异宠/鸟类专科医院推荐（含地址、电话和特色说明）", searchHospitalsParams()),
                functionTool("bind_account",
                        "关联/绑定当前用户的 QQ 号到系统的特定账号。需要提供系统账号的用户名和密码。", bindAccountParams()),
                functionTool("query_user_table",
                        "查询当前绑定的系统用户在数据库中指定数据表的所有记录（已在底层安全隔离用户数据）。可用表名：sys_user (用户详情), pet_profile (宠物列表), pet_weight_record (体重记录), pet_medical_record (就诊体检病历), pet_ledger_record (消费记账账本), pet_media_record (成长相册照片视频), alarm_record (传感器警情告警记录)", queryUserTableParams()),
                functionTool("query_tutorial_library",
                        "查询系统的鹦鹉喂养和紧急警情应对的百科教程库/知识库。当用户询问鹦鹉能吃什么、怎么养、应急处理、养护常识等非系统数据类的百科问题时使用。", queryTutorialLibraryParams()),
                functionTool("interact_with_parrot",
                        "通过 3D 虚拟场景与鹦鹉进行实时交互控制，包括喂食、加水、逗玩和截图。这会直接在前端触发鹦鹉互动动画并展现效果。", interactParrotParams())
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

    private Map<String, Object> petIdentifierParams() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "pet_identifier", Map.of(
                                "type", "string",
                                "description", "宠物标识符，可以是鹦鹉的名字（如'灰灰'）或宠物ID（如'PET-xxxx'）")),
                "required", List.of("pet_identifier"));
    }

    private Map<String, Object> createParrotParams() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "name", Map.of("type", "string", "description", "鹦鹉的名字，如'灰灰'"),
                        "species", Map.of("type", "string", "description", "鹦鹉的种类/品种，如'玄凤鹦鹉'、'虎皮鹦鹉'"),
                        "sex", Map.of("type", "string", "enum", List.of("male", "female", "unknown"), "description", "性别：male/female/unknown，默认 unknown"),
                        "birthday", Map.of("type", "string", "description", "出生日期，格式：YYYY-MM-DD，不能晚于今天"),
                        "initial_weight", Map.of("type", "number", "description", "初始体重（单位：克）"),
                        "device_id", Map.of("type", "string", "description", "绑定设备ID，如'SMK-001'"),
                        "feather_color", Map.of("type", "string", "description", "羽毛颜色，如'黄色'"),
                        "sterilized", Map.of("type", "boolean", "description", "是否已绝育"),
                        "remark", Map.of("type", "string", "description", "其他备注信息")
                ),
                "required", List.of("name", "species")
        );
    }

    private Map<String, Object> addParrotWeightParams() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "pet_identifier", Map.of("type", "string", "description", "鹦鹉名字或ID"),
                        "weight", Map.of("type", "number", "description", "体重的数值（克）")
                ),
                "required", List.of("pet_identifier", "weight")
        );
    }

    private Map<String, Object> addParrotMedicalParams() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "pet_identifier", Map.of("type", "string", "description", "鹦鹉名字或ID"),
                        "visit_date", Map.of("type", "string", "description", "就诊/体检日期，格式 YYYY-MM-DD，默认今天"),
                        "record_type", Map.of("type", "string", "enum", List.of("symptom", "diagnosis", "medication", "recheck", "other"), "description", "病历类型：symptom症状 / diagnosis诊断 / medication用药 / recheck复查 / other其他，默认 symptom"),
                        "title", Map.of("type", "string", "description", "病历标题，如'感冒'"),
                        "content", Map.of("type", "string", "description", "就诊详情、病情或药方说明（必填）"),
                        "hospital_name", Map.of("type", "string", "description", "就诊医院名称"),
                        "hospital_phone", Map.of("type", "string", "description", "就诊医院联系电话")
                ),
                "required", List.of("pet_identifier", "content")
        );
    }

    private Map<String, Object> addParrotLedgerParams() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "pet_identifier", Map.of("type", "string", "description", "鹦鹉名字或ID"),
                        "expense_date", Map.of("type", "string", "description", "消费日期，格式 YYYY-MM-DD，默认今天"),
                        "category", Map.of("type", "string", "description", "消费分类，如'饲料'、'医疗'、'玩具'、'其它'，默认其它"),
                        "amount", Map.of("type", "number", "description", "消费金额（元）"),
                        "description", Map.of("type", "string", "description", "费用明细或购买内容说明（必填）")
                ),
                "required", List.of("pet_identifier", "amount", "description")
        );
    }

    private Map<String, Object> searchHospitalsParams() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "city", Map.of("type", "string", "description", "城市名字，如'上海','北京','成都'")
                )
        );
    }

    private Map<String, Object> bindAccountParams() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "username", Map.of("type", "string", "description", "系统账号用户名，如 'admin'、'viewer'"),
                        "password", Map.of("type", "string", "description", "系统账号对应的登录密码")
                ),
                "required", List.of("username", "password")
        );
    }

    private Map<String, Object> queryUserTableParams() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "table_name", Map.of("type", "string", "description", "要查询的数据表名，可选值: sys_user, pet_profile, pet_weight_record, pet_medical_record, pet_ledger_record, pet_media_record, alarm_record"),
                        "limit", Map.of("type", "integer", "description", "查询记录的最大条数（默认20，最大50）")
                ),
                "required", List.of("table_name")
        );
    }

    private Map<String, Object> queryTutorialLibraryParams() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "query", Map.of("type", "string", "description", "向教程库提问的问题或搜索关键词，如'烟雾浓度超标怎么办'、'鹦鹉能吃什么蔬菜'")
                ),
                "required", List.of("query")
        );
    }

    private Map<String, Object> interactParrotParams() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "action", Map.of("type", "string", "description", "互动指令类别，可选值: feed (喂食), water (加水), play (逗玩), screenshot (截图)"),
                        "pet_identifier", Map.of("type", "string", "description", "指定要互动的鹦鹉名字或ID（可选，不传对该用户所有宠物触发）")
                ),
                "required", List.of("action")
        );
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
                case "list_parrots" -> toJson(petProfileService.listProfiles());
                case "get_parrot_details" -> {
                    String petId = resolvePetId(args);
                    if (petId == null) {
                        yield "未找到指定的鹦鹉档案，请确认名字或ID是否正确";
                    }
                    yield toJson(petProfileService.getProfile(petId));
                }
                case "create_parrot" -> {
                    String petName = getRequiredString(args, "name");
                    String species = getRequiredString(args, "species");
                    String sex = getOptionalString(args, "sex", "unknown");
                    String birthdayStr = getOptionalString(args, "birthday", null);
                    LocalDate birthday = null;
                    if (birthdayStr != null) {
                        birthday = LocalDate.parse(birthdayStr, DateTimeFormatter.ISO_LOCAL_DATE);
                    }
                    Double initialWeight = getOptionalDouble(args, "initial_weight");
                    String devId = getOptionalString(args, "device_id", null);
                    String featherColor = getOptionalString(args, "feather_color", null);
                    Boolean sterilized = getOptionalBoolean(args, "sterilized");
                    String remark = getOptionalString(args, "remark", null);

                    PetProfileCreateRequest req = new PetProfileCreateRequest();
                    req.setName(petName);
                    req.setSpecies(species);
                    req.setSex(sex);
                    req.setBirthday(birthday);
                    if (initialWeight != null) {
                        req.setInitialWeightGrams(BigDecimal.valueOf(initialWeight));
                    }
                    req.setDeviceId(devId);
                    req.setFeatherColor(featherColor);
                    req.setSterilized(sterilized);
                    req.setRemark(remark);

                    yield toJson(petProfileService.createProfile(req));
                }
                case "add_parrot_weight" -> {
                    String petId = resolvePetId(args);
                    if (petId == null) {
                        yield "未找到指定的鹦鹉档案，请确认名字或ID是否正确";
                    }
                    Double weight = getRequiredDouble(args, "weight");
                    PetWeightRequest req = new PetWeightRequest();
                    req.setWeightGrams(BigDecimal.valueOf(weight));
                    req.setSource("QQ Bot");
                    req.setRemark("通过 QQ 机器人录入");
                    yield toJson(petWeightService.createWeight(petId, req));
                }
                case "add_parrot_medical" -> {
                    String petId = resolvePetId(args);
                    if (petId == null) {
                        yield "未找到指定的鹦鹉档案，请确认名字或ID是否正确";
                    }
                    String visitDateStr = getOptionalString(args, "visit_date", LocalDate.now().toString());
                    LocalDate visitDate = LocalDate.parse(visitDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
                    String recordType = getOptionalString(args, "record_type", "symptom");
                    String title = getOptionalString(args, "title", "通过QQ机器人录入");
                    String content = getRequiredString(args, "content");
                    String hospName = getOptionalString(args, "hospital_name", null);
                    String hospPhone = getOptionalString(args, "hospital_phone", null);

                    PetMedicalRecordRequest req = new PetMedicalRecordRequest();
                    req.setRecordDate(visitDate);
                    req.setRecordType(recordType);
                    req.setTitle(title);
                    req.setContent(content);
                    req.setHospitalName(hospName);
                    req.setHospitalPhone(hospPhone);

                    yield toJson(petMedicalRecordService.createRecord(petId, req));
                }
                case "add_parrot_ledger" -> {
                    String petId = resolvePetId(args);
                    if (petId == null) {
                        yield "未找到指定的鹦鹉档案，请确认名字或ID是否正确";
                    }
                    String expDateStr = getOptionalString(args, "expense_date", LocalDate.now().toString());
                    LocalDate expDate = LocalDate.parse(expDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
                    String category = getOptionalString(args, "category", "其它");
                    Double amount = getRequiredDouble(args, "amount");
                    String desc = getRequiredString(args, "description");

                    PetLedgerRecordRequest req = new PetLedgerRecordRequest();
                    req.setExpenseDate(expDate);
                    req.setCategory(category);
                    req.setAmount(BigDecimal.valueOf(amount));
                    req.setCurrency("CNY");
                    req.setDescription(desc);

                    yield toJson(petLedgerRecordService.createRecord(petId, req));
                }
                case "generate_daily_growth_report" -> {
                    String petId = resolvePetId(args);
                    if (petId == null) {
                        yield "未找到指定的鹦鹉档案，请确认名字或ID是否正确";
                    }
                    yield generateDailyReport(petId);
                }
                case "search_nearby_pet_hospitals" -> {
                    String city = getOptionalString(args, "city", null);
                    yield searchPetHospitals(city);
                }
                case "bind_account" -> {
                    String username = getRequiredString(args, "username");
                    String password = getRequiredString(args, "password");
                    yield executeAccountBinding(userId, username, password);
                }
                case "query_user_table" -> {
                    String tableName = getRequiredString(args, "table_name");
                    Integer limit = getOptionalInteger(args, "limit", 20);
                    yield queryUserTable(tableName, limit);
                }
                case "query_tutorial_library" -> {
                    String query = getRequiredString(args, "query");
                    yield queryTutorialLibrary(userId, query);
                }
                case "interact_with_parrot" -> {
                    String action = getRequiredString(args, "action");
                    yield triggerParrotInteraction(userId, action);
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

    /**
     * 智能解析宠物标识：优先匹配精确 petId，其次根据用户上下文在当前拥有的宠物列表中匹配名字。
     */
    private String resolvePetId(JsonNode args) {
        if (args == null || !args.has("pet_identifier")) {
            return null;
        }
        String idOrName = args.get("pet_identifier").asText().trim();
        if (idOrName.isBlank()) {
            return null;
        }
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            userId = 1L; // 降级为系统管理员
        }
        // 1. 尝试以业务 ID 查找
        Optional<PetProfile> opt = petProfileRepository.findByPetIdAndUserId(idOrName, userId);
        if (opt.isPresent()) {
            return opt.get().getPetId();
        }
        // 2. 尝试以名字进行查找
        List<PetProfile> profiles = petProfileRepository.findByUserIdAndEnabledTrueOrderByUpdatedAtDesc(userId);
        for (PetProfile p : profiles) {
            if (p.getName().equalsIgnoreCase(idOrName)) {
                return p.getPetId();
            }
        }
        return null;
    }

    private String getRequiredString(JsonNode args, String field) {
        if (args == null || !args.has(field)) {
            throw new IllegalArgumentException("缺少必填字段: " + field);
        }
        return args.get(field).asText();
    }

    private String getOptionalString(JsonNode args, String field, String defaultValue) {
        if (args == null || !args.has(field) || args.get(field).isNull()) {
            return defaultValue;
        }
        return args.get(field).asText();
    }

    private Double getRequiredDouble(JsonNode args, String field) {
        if (args == null || !args.has(field)) {
            throw new IllegalArgumentException("缺少必填数值字段: " + field);
        }
        return args.get(field).asDouble();
    }

    private Double getOptionalDouble(JsonNode args, String field) {
        if (args == null || !args.has(field) || args.get(field).isNull()) {
            return null;
        }
        return args.get(field).asDouble();
    }

    private Boolean getOptionalBoolean(JsonNode args, String field) {
        if (args == null || !args.has(field) || args.get(field).isNull()) {
            return null;
        }
        return args.get(field).asBoolean();
    }

    private Integer getOptionalInteger(JsonNode args, String field, Integer defaultValue) {
        if (args == null || !args.has(field) || args.get(field).isNull()) {
            return defaultValue;
        }
        return args.get(field).asInt();
    }

    /**
     * 融合环境时序、体重趋势、今日行为统计和病历数据，由大模型诊断生成精美的成长健康报告。
     */
    private String generateDailyReport(String petId) {
        try {
            Long userId = UserContext.getCurrentUserId();
            if (userId == null) {
                userId = 1L;
            }
            PetProfileResponse pet = petProfileService.getProfile(petId);
            List<PetWeightResponse> weights = petWeightService.listWeights(petId);
            List<PetMedicalRecordResponse> medical = petMedicalRecordService.listRecords(petId);

            Map<String, Object> behaviorStats = null;
            List<EnvironmentHistoryResponse> envHistory = null;
            if (pet.getDeviceId() != null && !pet.getDeviceId().isBlank()) {
                try {
                    String todayStr = LocalDate.now().toString();
                    behaviorStats = parrotBehaviorService.getBehaviorStats(pet.getDeviceId(), "today", todayStr);
                } catch (Exception e) {
                    log.warn("获取行为统计失败: petId={}, reason={}", petId, e.getMessage());
                }
                try {
                    envHistory = environmentHistoryService.getHistory(pet.getDeviceId(), "24h");
                } catch (Exception e) {
                    log.warn("获取环境历史失败: petId={}, reason={}", petId, e.getMessage());
                }
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("pet_profile", pet);
            data.put("recent_weights", weights.stream().limit(5).toList());
            data.put("recent_medical_records", medical.stream().limit(5).toList());
            if (behaviorStats != null) {
                data.put("today_behavior_stats", behaviorStats);
            }
            if (envHistory != null) {
                // 每隔两个小时采样一条以节省 token
                List<EnvironmentHistoryResponse> sparseEnv = envHistory.stream()
                        .filter(e -> e.getTime() != null && e.getTime().getHour() % 2 == 0)
                        .toList();
                data.put("env_history_24h_samples", sparseEnv);
            }

            String dataJson = objectMapper.writeValueAsString(data);

            List<Map<String, Object>> promptMessages = new ArrayList<>();
            promptMessages.add(message("system", """
                    你是「智慧宠物烟感安全系统」的在线执业宠物兽医与鸟类行为健康学者。
                    请仔细分析提供的鹦鹉档案、近期体重趋势（波动、增长、降低）、近期病历就诊历史、今日的视觉监控行为识别统计（如鸣叫、进食、睡眠次数等）以及近24小时内笼舍环境监测历史（温度、湿度、粉尘等），并生成一份极富专业度的【鹦鹉每日成长与健康体检报告】。

                    报告应包含：
                    1. 📝 基本信息与当前状态评估
                    2. ⚖️ 体重健康评估（比如对比正常品种体重，分析近期趋势）
                    3. 📊 行为活跃度与日常表现解读（根据进食、饮水、飞翔、睡觉次数，评估情绪与精神）
                    4. 🌡️ 笼舍环境舒适度分析（对比该品种鹦鹉理想的18-28℃，湿度40-60%RH等标准，指出环境风险）
                    5. 🩺 诊疗就医随访及针对性饲养/预防医学建议

                    语气务必亲切专业，用中文，多使用 emoji。排版美观、简洁明了。不要凭空虚构报告里没有提供的数据。
                    """));
            promptMessages.add(message("user", "下面是该宠物的详细数据，请为我生成报告：\n" + dataJson));

            Map<String, Object> response = llmClient.chat(promptMessages, null);
            if (response != null && response.containsKey("content")) {
                Object content = response.get("content");
                return content != null ? content.toString() : "⚠️ 生成报告内容为空";
            }
            return "⚠️ 大模型未返回报告内容";
        } catch (Exception e) {
            log.error("生成宠物成长日报失败: petId={}, reason={}", petId, e.getMessage());
            return "❌ 生成报告失败：" + e.getMessage();
        }
    }

    /**
     * 智能推荐指定城市的异宠/鸟类专科医院。若城市未传，则读取系统配置或用户 1L 设定的常驻城市。
     */
    private String searchPetHospitals(String city) {
        String queryCity = city;
        if (queryCity == null || queryCity.isBlank()) {
            try {
                queryCity = sysUserRepository.findById(1L)
                        .map(SysUser::getLocation)
                        .orElse(null);
            } catch (Exception e) {
                log.warn("无法从数据库读取用户地址: {}", e.getMessage());
            }
        }
        if (queryCity == null || queryCity.isBlank()) {
            queryCity = "北京";
        }

        String cleanCity = queryCity.replace("市", "").trim();
        StringBuilder sb = new StringBuilder();
        sb.append("🏥 推荐给您的 ").append(cleanCity).append(" 宠物医院（异宠/鸟类专科优先）：\n\n");

        if ("北京".equals(cleanCity)) {
            sb.append("1. 京爱亚动物医院\n")
              .append("📍 地址：北京市朝阳区北苑东路\n")
              .append("📞 电话：010-8493xxxx\n")
              .append("⭐ 特色：国内极负盛名的异宠鸟类专科，拥有多名鸟病/爬宠名医，设施完备。\n\n")
              .append("2. 北京珍爱动物医院\n")
              .append("📍 地址：北京市海淀区远大路甲\n")
              .append("📞 电话：010-8889xxxx\n")
              .append("⭐ 特色：设有鸟类门诊，对鹦鹉拔羽症、嗉囊炎治疗经验极其丰富。\n\n")
              .append("3. 北京美联众合爱康动物医院（异宠专科中心）\n")
              .append("📍 地址：北京市朝阳区大屯路甲\n")
              .append("📞 电话：010-6486xxxx\n")
              .append("⭐ 特色：配备专门的鸟科病房和鸟类气体麻醉系统，擅长复杂外科。");
        } else if ("上海".equals(cleanCity)) {
            sb.append("1. 上海申普宠物医院（总院）\n")
              .append("📍 地址：上海市黄浦区徐家汇路\n")
              .append("📞 电话：021-5306xxxx\n")
              .append("⭐ 特色：上海老牌鸟类与野生动物定点收容治疗医院，异宠内外科实力雄厚。\n\n")
              .append("2. 上海顽皮家族宠物医院\n")
              .append("📍 地址：上海市闵行区虹许路\n")
              .append("📞 电话：021-6262xxxx\n")
              .append("⭐ 特色：设备一流，在鸟类羽毛骨折外固定和麻醉控制上极具声誉。\n\n")
              .append("3. 新瑞鹏虹泰动物医院（上海异宠专科）\n")
              .append("📍 地址：上海市静安区万荣路\n")
              .append("📞 电话：021-6607xxxx\n")
              .append("⭐ 特色：提供24小时异宠鸟类急诊，常规嗉囊清洗和啄羽行为诊治。");
        } else if ("广州".equals(cleanCity)) {
            sb.append("1. 广州严国平动物医院\n")
              .append("📍 地址：广州市越秀区广州大道中\n")
              .append("📞 电话：020-8739xxxx\n")
              .append("⭐ 特色：广州地区著名的异宠与鸟病大夫，对鹦鹉肺炎和寄生虫病治疗效果极佳。\n\n")
              .append("2. 广州立德动物医院\n")
              .append("📍 地址：广州市天河区中山大道\n")
              .append("📞 电话：020-3882xxxx\n")
              .append("⭐ 特色：设有独立的禽鸟诊室和住院舱，避免猫狗啼叫引起鸟类严重的应激反应。\n\n")
              .append("3. 华南农业大学教学动物医院\n")
              .append("📍 地址：广州市天河区五山路\n")
              .append("📞 电话：020-8528xxxx\n")
              .append("⭐ 特色：高校教学背景，专家教授坐诊，擅长复杂的鸟类骨科及消化系统疑难杂症。");
        } else if ("深圳".equals(cleanCity)) {
            sb.append("1. 深圳派特尔动物医院（异宠专科）\n")
              .append("📍 地址：深圳市福田区滨河大道\n")
              .append("📞 电话：0755-8321xxxx\n")
              .append("⭐ 特色：华南异宠骨干医师坐诊，配备精密的小动物血气和生化仪器。\n\n")
              .append("2. 瑞鹏宠物医院（百花旗舰店）\n")
              .append("📍 地址：深圳市福田区百花二路\n")
              .append("📞 电话：0755-8325xxxx\n")
              .append("⭐ 特色：配有专门的异宠及特种宠物医师，擅长鹦鹉剪羽、修喙以及营养性骨病治疗。\n\n")
              .append("3. 深圳安答异宠动物医院\n")
              .append("📍 地址：深圳市南山区沙河东路\n")
              .append("📞 电话：0755-2692xxxx\n")
              .append("⭐ 特色：专注于鹦鹉、鸣禽和鹦鹉外科手术，针对呼吸道感染有独特诊疗方案。");
        } else if ("成都".equals(cleanCity)) {
            sb.append("1. 四川农业大学教学动物医院（成都分院）\n")
              .append("📍 地址：成都市温江区杨柳东路\n")
              .append("📞 电话：028-8629xxxx\n")
              .append("⭐ 特色：高校异宠专家团队，西南地区公认的鸟类手术及疑难病诊疗中心。\n\n")
              .append("2. 成都华西宠物医院\n")
              .append("📍 地址：成都市武侯区洗面桥街\n")
              .append("📞 电话：028-8555xxxx\n")
              .append("⭐ 特色：设立专门的异宠特需科，擅长虎皮、玄凤等小型鹦鹉的营养缺乏症诊断。\n\n")
              .append("3. 成都谐和万家异宠医院\n")
              .append("📍 地址：成都市锦江区东大路\n")
              .append("📞 电话：028-8661xxxx\n")
              .append("⭐ 特色：对观赏鸟传染性鼻炎、球虫感染的治疗口碑良好。");
        } else if ("武汉".equals(cleanCity)) {
            sb.append("1. 华中农业大学教学动物医院\n")
              .append("📍 地址：武汉市洪山区狮子山街\n")
              .append("📞 电话：027-8728xxxx\n")
              .append("⭐ 特色：在鸟类病毒性疾病诊断、寄生虫驱虫及外科微创手术上处于华中地区前列。\n\n")
              .append("2. 武汉艾贝尔宠物医院（异宠专科）\n")
              .append("📍 地址：武汉市江汉区青年路\n")
              .append("📞 电话：027-8360xxxx\n")
              .append("⭐ 特色：设有专属异宠诊疗通道，提供鹦鹉常见细菌感染及应激调理。");
        } else if ("重庆".equals(cleanCity)) {
            sb.append("1. 重庆西南大学教学动物医院\n")
              .append("📍 地址：重庆市北碚区天生路\n")
              .append("📞 电话：023-6825xxxx\n")
              .append("⭐ 特色：西南大学动物医学教授带队，擅长禽鸟外科骨折及腹部手术。\n\n")
              .append("2. 瑞派博特宠物医院（重庆异宠总院）\n")
              .append("📍 地址：重庆市渝北区新牌坊三路\n")
              .append("📞 电话：023-6753xxxx\n")
              .append("⭐ 特色：拥有独立的鸟类住院区，专门治疗鹦鹉啄毛症、消化不良及产卵困难。");
        } else if ("杭州".equals(cleanCity)) {
            sb.append("1. 杭州虹泰宠物医院（环城北路总院）\n")
              .append("📍 地址：杭州市拱墅区环城北路\n")
              .append("📞 电话：0571-8519xxxx\n")
              .append("⭐ 特色：浙江省内极其完备的异宠医学中心，针对中大型金刚、灰鹦鹉诊疗经验十分丰富。\n\n")
              .append("2. 浙江大学教学动物医院\n")
              .append("📍 地址：杭州市西湖区余杭塘路\n")
              .append("📞 电话：0571-8898xxxx\n")
              .append("⭐ 特色：高校团队把关，在鸟类血常规生化分析、X光及CT影像阅片上极为精准。");
        } else {
            sb.append("1. 您的当前城市 [").append(cleanCity).append("] 暂时未被收录进本系统的鸟类/异宠专家推荐名册中。\n")
              .append("💡 鸟类看病小建议：\n")
              .append("   - 鹦鹉等禽类生理构造特殊，国内多数一般猫狗宠物医院可能缺乏鸟类诊断和用药经验，甚至无法进行安全麻醉；\n")
              .append("   - 建议在地图软件上搜索关键词「异宠医院」或「鸟类专科」寻找您附近的诊所；\n")
              .append("   - 如果情况紧急，可就近选择当地大型农业大学附属的「教学动物医院」或「瑞鹏/瑞派等连锁旗舰店」，询问是否有能看鹦鹉的专家门诊；\n")
              .append("   - 带鸟就医途中必须保暖避风，可在运输箱外盖上毛毯，尽量使用深色箱盒以防鹦鹉应激。");
        }
        return sb.toString();
    }

    private JsonNode parseArgs(String arguments) {
        try {
            return objectMapper.readTree(arguments);
        } catch (Exception e) {
            log.warn("工具参数解析失败: arguments={}, reason={}", arguments, e.getMessage());
            return null;
        }
    }

    /**
     * 大模型工具调用的账号绑定执行逻辑。
     */
    private String executeAccountBinding(long qqNumber, String username, String password) {
        try {
            Optional<SysUser> optUser = sysUserRepository.findByUsername(username);
            if (optUser.isEmpty()) {
                return "❌ 绑定失败：用户名不存在。";
            }
            SysUser user = optUser.get();
            if (user.getStatus() == null || user.getStatus() != 1) {
                return "❌ 绑定失败：该账号已被禁用。";
            }
            if (!password.equals(user.getPassword())) {
                return "❌ 绑定失败：密码错误。";
            }

            // 检查该 QQ 号是否已被其它账号绑定，如果是则清理旧绑定
            Optional<UserPreference> existingQqBinding = userPreferenceRepository.findByPrefKeyAndPrefValue("bound_qq", String.valueOf(qqNumber));
            if (existingQqBinding.isPresent()) {
                userPreferenceRepository.delete(existingQqBinding.get());
            }

            // 保存或更新当前账号的绑定偏好
            UserPreference preference = userPreferenceRepository.findByUserIdAndPrefKey(user.getId(), "bound_qq")
                    .orElseGet(() -> UserPreference.builder()
                            .userId(user.getId())
                            .prefKey("bound_qq")
                            .prefGroup("qq_bot")
                            .description("绑定QQ机器人")
                            .build());
            preference.setPrefValue(String.valueOf(qqNumber));
            userPreferenceRepository.save(preference);

            return "✅ 绑定成功！您的 QQ 号已与系统账号 [" + username + "] 成功关联。从现在起，系统将为您提供专属的数据隔离查询与管家式服务！";
        } catch (Exception e) {
            log.error("大模型触发 QQ 绑定失败: qq={}, username={}, reason={}", qqNumber, username, e.getMessage());
            return "❌ 绑定过程出错：" + e.getMessage();
        }
    }

    /**
     * 查询指定数据表内容，自动绑定当前绑定的用户ID以隔离数据。
     */
    private String queryUserTable(String tableName, int limit) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            userId = 1L; // 默认降级为管理员(1L)
        }
        int maxLimit = Math.min(limit, 50);

        try {
            return switch (tableName.toLowerCase().trim()) {
                case "sys_user" -> {
                    Optional<SysUser> user = sysUserRepository.findById(userId);
                    if (user.isPresent()) {
                        SysUser u = user.get();
                        Map<String, Object> uMap = new LinkedHashMap<>();
                        uMap.put("id", u.getId());
                        uMap.put("username", u.getUsername());
                        uMap.put("realName", u.getRealName());
                        uMap.put("role", u.getRole());
                        uMap.put("phone", u.getPhone());
                        uMap.put("email", u.getEmail());
                        uMap.put("location", u.getLocation());
                        yield toJson(uMap);
                    }
                    yield "未找到当前用户信息";
                }
                case "pet_profile" -> toJson(petProfileRepository.findByUserId(userId).stream().limit(maxLimit).toList());
                case "pet_weight_record" -> {
                    List<PetProfile> pets = petProfileRepository.findByUserId(userId);
                    List<String> petIds = pets.stream().map(PetProfile::getPetId).toList();
                    if (petIds.isEmpty()) {
                        yield "[]";
                    }
                    List<PetWeightRecord> weights = new ArrayList<>();
                    for (String petId : petIds) {
                        weights.addAll(petWeightRecordRepository.findByPetIdOrderByMeasuredAtDesc(petId));
                    }
                    yield toJson(weights.stream().sorted((a, b) -> b.getMeasuredAt().compareTo(a.getMeasuredAt())).limit(maxLimit).toList());
                }
                case "pet_medical_record" -> {
                    List<PetProfile> pets = petProfileRepository.findByUserId(userId);
                    List<String> petIds = pets.stream().map(PetProfile::getPetId).toList();
                    if (petIds.isEmpty()) {
                        yield "[]";
                    }
                    List<PetMedicalRecord> medicals = new ArrayList<>();
                    for (String petId : petIds) {
                        medicals.addAll(petMedicalRecordRepository.findByPetIdOrderByRecordDateDescCreatedAtDesc(petId));
                    }
                    yield toJson(medicals.stream().sorted((a, b) -> b.getRecordDate().compareTo(a.getRecordDate())).limit(maxLimit).toList());
                }
                case "pet_ledger_record" -> {
                    List<PetProfile> pets = petProfileRepository.findByUserId(userId);
                    List<String> petIds = pets.stream().map(PetProfile::getPetId).toList();
                    if (petIds.isEmpty()) {
                        yield "[]";
                    }
                    List<PetLedgerRecord> ledgers = new ArrayList<>();
                    for (String petId : petIds) {
                        ledgers.addAll(petLedgerRecordRepository.findByPetIdOrderByExpenseDateDescCreatedAtDesc(petId));
                    }
                    yield toJson(ledgers.stream().sorted((a, b) -> b.getExpenseDate().compareTo(a.getExpenseDate())).limit(maxLimit).toList());
                }
                case "pet_media_record" -> {
                    List<PetProfile> pets = petProfileRepository.findByUserId(userId);
                    List<String> petIds = pets.stream().map(PetProfile::getPetId).toList();
                    if (petIds.isEmpty()) {
                        yield "[]";
                    }
                    List<PetMediaRecord> medias = new ArrayList<>();
                    for (String petId : petIds) {
                        medias.addAll(petMediaRecordRepository.findByPetIdAndMediaTypeInOrderByCapturedAtDesc(
                                petId, List.of("photo", "screenshot", "recording", "video")));
                    }
                    yield toJson(medias.stream().sorted((a, b) -> b.getCapturedAt().compareTo(a.getCapturedAt())).limit(maxLimit).toList());
                }
                case "alarm_record" -> toJson(alarmRecordRepository.findByUserIdOrderByTriggeredAtDesc(userId).stream().limit(maxLimit).toList());
                default -> "不支持直接查询该表，只能查询与用户相关的表：" +
                        "sys_user, pet_profile, pet_weight_record, pet_medical_record, pet_ledger_record, pet_media_record, alarm_record";
            };
        } catch (Exception e) {
            log.error("大模型查询用户数据库表失败: table={}, reason={}", tableName, e.getMessage());
            return "查询失败：" + e.getMessage();
        }
    }

    /**
     * 查询教程库/知识库中的百科与应急处理指南。
     */
    private String queryTutorialLibrary(long userId, String query) {
        if (!maxKBClient.isEnabled()) {
            return "❌ 百科知识库暂未配置或未启用。";
        }
        try {
            String answer = maxKBClient.chat(userId, query);
            if (answer == null || answer.isBlank()) {
                return "未找到相关教程。";
            }
            return answer;
        } catch (Exception e) {
            log.error("大模型查询教程库失败: query={}, reason={}", query, e.getMessage());
            return "查询教程库失败：" + e.getMessage();
        }
    }

    /**
     * 向指定用户的活跃前端 WebSocket 会话推送实时鹦鹉交互控制指令。
     */
    private String triggerParrotInteraction(long qqNumber, String action) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            userId = 1L; // 默认降级为管理员(1L)
        }

        try {
            AlarmWebSocketPayload payload = AlarmWebSocketPayload.builder()
                    .type("parrot_interaction")
                    .userId(userId)
                    .action(action)
                    .message("大模型触发 QQ 互动操作: " + action)
                    .alarmTime(LocalDateTime.now())
                    .build();

            alarmWebSocketSessionManager.broadcastAlarmToUser(userId, payload);

            String actionText = switch (action.toLowerCase().trim()) {
                case "feed" -> "喂食";
                case "water" -> "加水";
                case "play" -> "逗玩";
                case "screenshot" -> "截图";
                default -> action;
            };

            return "✅ 已向您的智能鸟笼和前端 3D 模拟场景发送了【" + actionText + "】指令！请查看您的前端实时画面或刷新监控面板，即可观赏鹦鹉的真实交互反馈与动作效果！";
        } catch (Exception e) {
            log.error("大模型触发 QQ 互动操作失败: action={}, reason={}", action, e.getMessage());
            return "❌ 互动操作执行失败：" + e.getMessage();
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

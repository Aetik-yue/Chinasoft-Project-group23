package com.chinasoft.smokesensor.service.qq;

import com.chinasoft.smokesensor.dto.DeviceControlRequest;
import com.chinasoft.smokesensor.dto.DeviceControlResponse;
import com.chinasoft.smokesensor.service.DeviceService;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * QQ 控制指令处理：解析"开/关+设备"指令，并通过 Redis 实现 60 秒二次确认流程。
 *
 * <p>由于控制 IoT 设备是有副作用的操作，直接执行存在误操作风险（如用户误发"开蜂鸣器"）。
 * 因此采用两步交互：
 * <ol>
 *   <li>用户发"开蜂鸣器" -> 解析出 (target=buzzer, action=on)，写入 Redis 待确认队列
 *      （key=qq:pending:{userId}，TTL=60s），回复确认提示</li>
 *   <li>用户回复"确认" -> 从 Redis 取出待确认操作，调用 {@link DeviceService#controlDevice}
 *       下发，回复执行结果；超时或回复"取消"则放弃</li>
 * </ol>
 *
 * <p>控制对象仅支持后端 {@code DeviceServiceImpl} 已实现的 switch / buzzer / alarm_light 三种
 * （fan / air_purifier 为自动联动，不由用户手动控制）。
 *
 * <p>Redis 不可用时，待确认操作无法暂存，控制指令降级为提示"暂不可用"，不影响其他功能。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OneBotControlService {

    private static final String DEFAULT_DEVICE_ID = "SMK-001";
    private static final String PENDING_KEY_PREFIX = "qq:pending:";
    private static final Duration PENDING_TTL = Duration.ofSeconds(60);

    /** 确认关键词（精确匹配，已 trim 并转小写）。 */
    private static final Set<String> CONFIRM_WORDS = Set.of("确认", "确定", "是", "好", "yes", "y");
    /** 取消关键词。 */
    private static final Set<String> CANCEL_WORDS = Set.of("取消", "不了", "放弃", "no", "n");

    /** target 编码 -> 中文名。 */
    private static final Map<String, String> CONTROL_NAMES = Map.of(
            "buzzer", "蜂鸣器",
            "alarm_light", "报警灯",
            "switch", "总开关");

    private final DeviceService deviceService;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 尝试解析控制指令。若 msg 是"开/关+设备"格式，暂存待确认操作并返回确认提示；否则返回 null。
     *
     * @param userId 用户 QQ 号
     * @param msg    消息原文（已 trim）
     * @return 确认提示文本，或 null（非控制指令）
     */
    public String tryControl(long userId, String msg) {
        String action = parseAction(msg);
        String target = parseTarget(msg);
        if (action == null || target == null) {
            return null;
        }
        if (!savePending(userId, new PendingControlOp(DEFAULT_DEVICE_ID, target, action))) {
            return "⚠️ 暂存确认操作失败（Redis 不可用），控制指令暂不可用。";
        }
        return "⚠️ 确认" + actionText(action) + " " + DEFAULT_DEVICE_ID + " " + name(target)
                + "？\n回复“确认”执行（60秒内有效）\n回复“取消”放弃";
    }

    /**
     * 确认并执行待确认操作。无待确认操作返回 null（由调用方继续其他意图）。
     */
    /**
     * 发起控制请求（供 LLM control_device 工具调用）：校验参数、暂存 pendingOp、返回确认提示。
     * 不直接执行，需用户回复“确认”后由 {@link #confirmPending} 执行。
     *
     * @param userId 用户 QQ 号
     * @param target 控制对象（buzzer / alarm_light / switch）
     * @param action 动作（on / off）
     * @return 确认提示文本，或参数错误提示
     */
    public String requestControl(long userId, String target, String action) {
        if (!CONTROL_NAMES.containsKey(target)) {
            return "❌ 不支持的控制对象：" + target + "，仅支持蜂鸣器 / 报警灯 / 总开关";
        }
        if (!"on".equals(action) && !"off".equals(action)) {
            return "❌ 无效的动作：" + action + "，仅支持 on / off";
        }
        if (!savePending(userId, new PendingControlOp(DEFAULT_DEVICE_ID, target, action))) {
            return "⚠️ 暂存确认操作失败（Redis 不可用），控制指令暂不可用。";
        }
        return "⚠️ 确认" + actionText(action) + " " + DEFAULT_DEVICE_ID + " " + name(target)
                + "？\n回复“确认”执行（60秒内有效）\n回复“取消”放弃";
    }

    /**
     * 发起控制请求（指定设备，供 LLM control_device 工具调用）。
     * 与 {@link #requestControl(long, String, String)} 区别：可指定 deviceId，不写死默认设备。
     *
     * @param deviceId 设备编号（为空则用默认设备 SMK-001）
     */
    public String requestControl(long userId, String deviceId, String target, String action) {
        if (!CONTROL_NAMES.containsKey(target)) {
            return "❌ 不支持的控制对象：" + target + "，仅支持蜂鸣器 / 报警灯 / 总开关";
        }
        if (!"on".equals(action) && !"off".equals(action)) {
            return "❌ 无效的动作：" + action + "，仅支持 on / off";
        }
        String dev = (deviceId == null || deviceId.isBlank()) ? DEFAULT_DEVICE_ID : deviceId;
        if (!savePending(userId, new PendingControlOp(dev, target, action))) {
            return "⚠️ 暂存确认操作失败（Redis 不可用），控制指令暂不可用。";
        }
        return "⚠️ 确认" + actionText(action) + " " + dev + " " + name(target)
                + "？\n回复“确认”执行（60秒内有效）\n回复“取消”放弃";
    }

    public String confirmPending(long userId) {
        PendingControlOp op = loadPending(userId);
        if (op == null) {
            return null;
        }
        deletePending(userId);
        try {
            DeviceControlResponse resp = deviceService.controlDevice(DeviceControlRequest.builder()
                    .deviceId(op.deviceId())
                    .target(op.target())
                    .action(op.action())
                    .build());
            if (Boolean.TRUE.equals(resp.getSuccess())) {
                return "✅ 已" + actionText(op.action()) + " " + op.deviceId() + " " + name(op.target());
            }
            return "❌ 控制失败：" + (resp.getMessage() == null ? "未知原因" : resp.getMessage());
        } catch (Exception e) {
            log.warn("QQ 控制指令执行失败: userId={}, op={}, reason={}", userId, op, e.getMessage());
            return "❌ 控制失败：" + e.getMessage();
        }
    }

    /**
     * 取消待确认操作。无待确认操作返回 null。
     */
    public String cancelPending(long userId) {
        PendingControlOp op = loadPending(userId);
        if (op == null) {
            return null;
        }
        deletePending(userId);
        return "已取消 " + name(op.target()) + " 控制操作";
    }

    /** 判断是否为确认指令。 */
    public static boolean isConfirm(String msg) {
        return msg != null && CONFIRM_WORDS.contains(msg.toLowerCase());
    }

    /** 判断是否为取消指令。 */
    public static boolean isCancel(String msg) {
        return msg != null && CANCEL_WORDS.contains(msg.toLowerCase());
    }

    // ========================================================================
    // 解析与文案
    // ========================================================================

    /** 解析动作：msg 以"开/打"开头为 on，以"关"开头为 off，否则 null。 */
    private String parseAction(String msg) {
        if (msg.startsWith("开") || msg.startsWith("打")) {
            return "on";
        }
        if (msg.startsWith("关")) {
            return "off";
        }
        return null;
    }

    /** 解析控制对象：按关键词匹配 target 编码。 */
    private String parseTarget(String msg) {
        if (msg.contains("报警灯") || msg.contains("警灯")) {
            return "alarm_light";
        }
        if (msg.contains("蜂鸣器")) {
            return "buzzer";
        }
        if (msg.contains("总开关") || msg.contains("联动")) {
            return "switch";
        }
        return null;
    }

    private String actionText(String action) {
        return "on".equals(action) ? "开启" : "关闭";
    }

    private String name(String target) {
        return CONTROL_NAMES.getOrDefault(target, target);
    }

    // ========================================================================
    // Redis 待确认操作存取（失败容错，参照 AlarmServiceImpl 的缓存容错模式）
    // ========================================================================

    private boolean savePending(long userId, PendingControlOp op) {
        try {
            redisTemplate.opsForValue().set(PENDING_KEY_PREFIX + userId, op, PENDING_TTL);
            return true;
        } catch (Exception e) {
            log.warn("Redis 写入待确认操作失败: userId={}, reason={}", userId, e.getMessage());
            return false;
        }
    }

    private PendingControlOp loadPending(long userId) {
        try {
            Object value = redisTemplate.opsForValue().get(PENDING_KEY_PREFIX + userId);
            return value instanceof PendingControlOp op ? op : null;
        } catch (Exception e) {
            log.warn("Redis 读取待确认操作失败: userId={}, reason={}", userId, e.getMessage());
            return null;
        }
    }

    private void deletePending(long userId) {
        try {
            redisTemplate.delete(PENDING_KEY_PREFIX + userId);
        } catch (Exception e) {
            log.warn("Redis 删除待确认操作失败: userId={}, reason={}", userId, e.getMessage());
        }
    }

    /** 待确认控制操作。 */
    public record PendingControlOp(String deviceId, String target, String action) {
    }
}

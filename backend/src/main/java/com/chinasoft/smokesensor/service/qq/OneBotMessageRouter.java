package com.chinasoft.smokesensor.service.qq;

import com.chinasoft.smokesensor.client.MaxKBClient;
import com.chinasoft.smokesensor.config.OneBotProperties;
import com.chinasoft.smokesensor.dto.AlarmRecordResponse;
import com.chinasoft.smokesensor.dto.AlarmTodayStatResponse;
import com.chinasoft.smokesensor.dto.DeviceStatusResponse;
import com.chinasoft.smokesensor.dto.SmokeRealtimeResponse;
import com.chinasoft.smokesensor.service.AlarmService;
import com.chinasoft.smokesensor.service.DeviceService;
import com.chinasoft.smokesensor.service.SmokeService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.chinasoft.smokesensor.common.UserContext;
import com.chinasoft.smokesensor.entity.SystemSetting;
import com.chinasoft.smokesensor.entity.SysUser;
import com.chinasoft.smokesensor.entity.UserPreference;
import com.chinasoft.smokesensor.repository.SystemSettingRepository;
import com.chinasoft.smokesensor.repository.SysUserRepository;
import com.chinasoft.smokesensor.repository.UserPreferenceRepository;
import org.springframework.stereotype.Service;

/**
 * QQ 消息路由与意图识别。
 *
 * <p>用户私聊消息经 {@link com.chinasoft.smokesensor.controller.OneBotCallbackController} 进入
 * {@link #handle}，处理顺序：
 * <ol>
 *   <li><b>确认 / 取消拦截</b> - 控制二次确认的执行 / 放弃（最高优先级）</li>
 *   <li><b>LLM function calling agent</b> - DeepSeek 理解自然语言，调用查询 / 控制工具（启用时优先）</li>
 *   <li><b>规则回退</b> - 关键词意图识别 + MaxKB 问答（LLM 未启用时）</li>
 * </ol>
 *
 * <p>三层降级：LLM -> MaxKB -> 规则。控制指令二次确认由 {@link OneBotControlService} + Redis 实现，
 * LLM 的 control_device 工具只发起请求（暂存 pendingOp），用户回复"确认"后由确认拦截执行，
 * 智能与安全兼顾。
 *
 * <p>白名单策略：{@code allowed-users} 非空时只允许白名单内 QQ 号交互；为空时放行所有人
 * （调试友好，生产建议配置白名单）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OneBotMessageRouter {

    /** 默认设备编号，与业务层统一定义。 */
    private static final String DEFAULT_DEVICE_ID = "SMK-001";
    /** 最近告警列表查询条数。 */
    private static final int RECENT_ALARM_LIMIT = 5;

    private final SmokeService smokeService;
    private final DeviceService deviceService;
    private final AlarmService alarmService;
    private final OneBotProperties properties;
    private final OneBotControlService controlService;
    private final MaxKBClient maxKBClient;
    private final AgentToolService agentToolService;
    private final SystemSettingRepository systemSettingRepository;
    private final SysUserRepository sysUserRepository;
    private final UserPreferenceRepository userPreferenceRepository;

    /**
     * 处理用户私聊消息，返回回复文本；返回 null 表示不回复（如未通过白名单或空消息）。
     *
     * @param userId  用户 QQ 号
     * @param message 消息原文
     * @return 回复文本，或 null
     */
    public String handle(long userId, String message) {
        if (!isAllowed(userId)) {
            log.debug("非白名单用户，忽略消息: userId={}, msg={}", userId, message);
            return null;
        }
        String msg = message == null ? "" : message.trim();
        if (msg.isEmpty()) {
            return null;
        }

        // 清除消息首尾常见的各种单双引号和中文智能引号，解决用户发送包含引号消息无法识别的问题
        while (msg.startsWith("\"") || msg.startsWith("'") || msg.startsWith("“") || msg.startsWith("”") ||
               msg.endsWith("\"") || msg.endsWith("'") || msg.endsWith("“") || msg.endsWith("”")) {
            if (msg.startsWith("\"") || msg.startsWith("'") || msg.startsWith("“") || msg.startsWith("”")) {
                msg = msg.substring(1);
            }
            if (msg.endsWith("\"") || msg.endsWith("'") || msg.endsWith("“") || msg.endsWith("”")) {
                msg = msg.substring(0, msg.length() - 1);
            }
            msg = msg.trim();
        }

        if (msg.isEmpty()) {
            return null;
        }

        // 拦截账号绑定指令（支持是否有空格，如“绑定账号admin 123456”或“绑定账号 admin 123456”）
        if (msg.startsWith("绑定账号")) {
            return handleAccountBinding(userId, msg);
        }

        // 尝试通过绑定的 QQ 号在偏好设置中匹配系统用户 ID
        Long sysUserId = 1L; // 默认降级为系统管理员 (1L)
        try {
            Optional<UserPreference> binding = userPreferenceRepository.findByPrefKeyAndPrefValue("bound_qq", String.valueOf(userId));
            if (binding.isPresent()) {
                sysUserId = binding.get().getUserId();
            }
        } catch (Exception e) {
            log.warn("查询 QQ 绑定偏好设置失败: userId={}, reason={}", userId, e.getMessage());
        }

        try {
            // 设置系统用户上下文，绑定为当前 QQ 所绑定的系统账号
            UserContext.setCurrentUserId(sysUserId);
            
            // 1. 确认/取消待确认操作（控制二次确认，最高优先级）
            if (OneBotControlService.isConfirm(msg)) {
                String result = controlService.confirmPending(userId);
                if (result != null) {
                    return result;
                }
            }
            if (OneBotControlService.isCancel(msg)) {
                String result = controlService.cancelPending(userId);
                if (result != null) {
                    return result;
                }
            }
            // 2. LLM function calling agent（启用时优先，自然语言理解 + 工具调用）
            String llmReply = agentToolService.runAgent(userId, msg);
            if (llmReply != null) {
                return llmReply;
            }
            // 3. 规则回退（LLM 未启用时：关键词意图识别 + MaxKB 问答）
            return ruleBasedFallback(userId, msg);
        } catch (Exception e) {
            log.warn("处理 QQ 消息失败: userId={}, msg={}, reason={}", userId, msg, e.getMessage());
            return "⚠️ 处理消息时出错：" + e.getMessage();
        } finally {
            UserContext.clear(); // 清理上下文，防止线程重用导致数据泄露/串号
        }
    }

    /**
     * 处理 QQ 用户绑定系统账号指令：“绑定账号 [用户名] [密码]” 或 “绑定账号[用户名] [密码]”
     */
    private String handleAccountBinding(long qqNumber, String msg) {
        String remainder = msg.substring("绑定账号".length()).trim();
        String[] parts = remainder.split("\\s+");
        if (parts.length < 2 || parts[0].isBlank() || parts[1].isBlank()) {
            return "❌ 绑定格式错误。\n请发送：“绑定账号 [用户名] [密码]”进行绑定。\n例如：“绑定账号 admin 123456”。";
        }
        String username = parts[0].trim();
        String password = parts[1].trim();

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

            return "✅ 绑定成功！您的 QQ 号已与系统账号 [" + username + "] 成功关联。现在您可以直接通过 QQ 查询和管理您账号下的鹦鹉与环境设备了！";
        } catch (Exception e) {
            log.error("绑定 QQ 账号失败: qq={}, username={}, reason={}", qqNumber, username, e.getMessage());
            return "❌ 绑定过程出错：" + e.getMessage();
        }
    }

    // ========================================================================
    // 规则回退（LLM 未启用时的降级路径）
    // ========================================================================

    /**
     * 规则意图识别 + MaxKB 问答兜底，作为 LLM 未启用时的降级方案。
     */
    private String ruleBasedFallback(long userId, String msg) {
        if (matchHelp(msg)) {
            return buildHelp();
        }
        if (matchRealtime(msg)) {
            return buildRealtime();
        }
        if (matchDeviceStatus(msg)) {
            return buildDeviceStatus();
        }
        if (matchAlarmStat(msg)) {
            return buildAlarmStat();
        }
        if (matchAlarmList(msg)) {
            return buildAlarmList();
        }
        String controlReply = controlService.tryControl(userId, msg);
        if (controlReply != null) {
            return controlReply;
        }
        return buildFallback(userId, msg);
    }

    // ========================================================================
    // 白名单校验
    // ========================================================================

    private boolean isAllowed(long userId) {
        // 1. 先查数据库配置的 QQ 白名单
        String dbQqList = systemSettingRepository.findBySettingKey("qq_white_list")
                .map(SystemSetting::getSettingValue)
                .orElse("");
        if (dbQqList != null && !dbQqList.isBlank()) {
            String[] qqs = dbQqList.split(",");
            for (String qqStr : qqs) {
                try {
                    if (Long.parseLong(qqStr.trim()) == userId) {
                        return true;
                    }
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }

        // 2. 如果数据库没有配置，回退到配置文件
        List<Long> allowed = properties.getAllowedUsers();
        if (allowed == null || allowed.isEmpty()) {
            // 配置文件如果也是空的，且数据库也没有配，就表示不限制白名单（放行所有人，调试友好）
            if (dbQqList == null || dbQqList.isBlank()) {
                return true;
            }
            return false;
        }
        return allowed.contains(userId);
    }

    // ========================================================================
    // 意图识别（关键词匹配，规则回退用）
    // ========================================================================

    private boolean matchHelp(String msg) {
        return msg.contains("帮助") || msg.equalsIgnoreCase("help")
                || msg.contains("菜单") || msg.contains("指令");
    }

    private boolean matchRealtime(String msg) {
        return msg.contains("状态") || msg.contains("实时") || msg.contains("浓度")
                || msg.contains("温度") || msg.contains("湿度") || msg.contains("当前");
    }

    private boolean matchDeviceStatus(String msg) {
        return msg.contains("在线") || msg.contains("设备状态") || msg.contains("心跳");
    }

    private boolean matchAlarmStat(String msg) {
        return msg.contains("告警次数") || msg.contains("今天告警") || msg.contains("今日告警")
                || (msg.contains("告警") && !msg.contains("最近") && !msg.contains("列表")
                        && !msg.contains("历史") && !msg.contains("处理"));
    }

    private boolean matchAlarmList(String msg) {
        return msg.contains("最近告警") || msg.contains("告警列表") || msg.contains("告警记录")
                || msg.contains("历史告警");
    }

    // ========================================================================
    // 回复文本组装
    // ========================================================================

    private String buildHelp() {
        return """
                🐦 智慧宠物烟感助手
                直接用自然语言告诉我，或发指令：

                【查询】
                 状态 / 现在浓度多少 / 笼子温度
                 在线吗 / 设备还连着吗
                 告警 / 今天告警几次
                 最近告警

                【控制】
                 开蜂鸣器 / 关报警灯 / 开总开关
                 （控制需回复“确认”二次确认）

                【账号关联】
                 绑定账号 [用户名] [密码]
                 （实现多用户数据隔离与个性化管理）

                【其他】
                 鹦鹉能吃XX吗 / 烟雾超标怎么办
                 帮助""";
    }

    private String buildRealtime() {
        SmokeRealtimeResponse r = smokeService.getRealtimeSmoke(DEFAULT_DEVICE_ID);
        StringBuilder sb = new StringBuilder();
        sb.append("📊 实时状态（").append(DEFAULT_DEVICE_ID).append("）\n");
        if (Boolean.FALSE.equals(r.getConnected())) {
            sb.append("⚠️ 设备当前离线\n");
            if (r.getUpdateTime() != null) {
                sb.append("最后数据：").append(r.getUpdateTime()).append("\n");
            }
            if (r.getMessage() != null) {
                sb.append(r.getMessage());
            }
            return sb.toString();
        }
        sb.append("浓度：").append(r.getSmokeValue() == null ? "-" : r.getSmokeValue())
                .append(r.getUnit() == null ? "ppm" : r.getUnit());
        if (r.getRiskLevel() != null) {
            sb.append("（").append(riskLevelText(r.getRiskLevel())).append("）");
        }
        sb.append("\n");
        sb.append("温度：").append(r.getTemperature() == null ? "-" : r.getTemperature()).append("℃\n");
        sb.append("湿度：").append(r.getHumidity() == null ? "-" : r.getHumidity()).append("%RH\n");
        sb.append("告警：").append(r.getAlarmStatus() == null ? "-" : r.getAlarmStatus()).append("\n");
        if (r.getUpdateTime() != null) {
            sb.append("更新：").append(r.getUpdateTime());
        }
        return sb.toString();
    }

    private String buildDeviceStatus() {
        DeviceStatusResponse s = deviceService.getDeviceStatus(DEFAULT_DEVICE_ID);
        StringBuilder sb = new StringBuilder();
        sb.append("设备 ").append(s.getDeviceId() == null ? DEFAULT_DEVICE_ID : s.getDeviceId()).append("：");
        sb.append(Boolean.TRUE.equals(s.getConnected()) ? "🟢 在线" : "🔴 离线").append("\n");
        if (s.getLastHeartbeat() != null) {
            sb.append("最后心跳：").append(s.getLastHeartbeat());
        }
        if (s.getMessage() != null && !s.getMessage().isBlank()) {
            sb.append("\n").append(s.getMessage());
        }
        return sb.toString();
    }

    private String buildAlarmStat() {
        AlarmTodayStatResponse stat = alarmService.getTodayStat();
        StringBuilder sb = new StringBuilder();
        sb.append("🔔 今日告警：").append(stat.getTodayCount()).append(" 次\n");
        sb.append("昨日：").append(stat.getYesterdayCount()).append(" 次");
        if (stat.getChangeRate() != null) {
            String arrow = stat.getChangeRate() >= 0 ? "↑" : "↓";
            sb.append("（").append(arrow).append(String.format("%.0f%%", Math.abs(stat.getChangeRate()))).append("）");
        }
        return sb.toString();
    }

    private String buildAlarmList() {
        List<AlarmRecordResponse> alarms = alarmService.getAlarmList(RECENT_ALARM_LIMIT);
        if (alarms.isEmpty()) {
            return "📋 暂无告警记录";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("📋 最近告警（").append(alarms.size()).append("条）\n");
        for (int i = 0; i < alarms.size(); i++) {
            AlarmRecordResponse a = alarms.get(i);
            sb.append(i + 1).append(". [").append(a.getRiskLevel()).append("] ");
            sb.append(a.getDeviceId());
            if (a.getSmokeValue() != null) {
                sb.append(" ").append(a.getSmokeValue()).append("ppm");
            }
            if (a.getTriggeredAt() != null) {
                sb.append(" ").append(a.getTriggeredAt());
            }
            sb.append(" ").append(a.getStatus()).append("\n");
        }
        return sb.toString().stripTrailing();
    }

    private String buildFallback(long userId, String msg) {
        // MaxKB 智能问答兜底（未配置 / 调用失败则回退到指令提示）
        if (maxKBClient.isEnabled()) {
            String answer = maxKBClient.chat(userId, msg);
            if (answer != null && !answer.isBlank()) {
                return answer;
            }
            return "🤔 暂时无法回答该问题，回复“帮助”查看支持的指令。";
        }
        return "未识别的指令，回复“帮助”查看支持的指令。\n（LLM 与 MaxKB 均未启用，请在 application.yml 配置 qq.llm 或 qq.maxkb）";
    }

    /** 风险等级中文文案。 */
    private String riskLevelText(String level) {
        return switch (level) {
            case "high" -> "高危";
            case "medium" -> "中危";
            case "low" -> "低危";
            default -> "正常";
        };
    }
}

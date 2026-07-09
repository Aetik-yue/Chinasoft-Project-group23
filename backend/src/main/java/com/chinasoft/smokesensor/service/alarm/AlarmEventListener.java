package com.chinasoft.smokesensor.service.alarm;

import com.chinasoft.smokesensor.service.qq.OneBotPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 告警事件监听器：在告警入库事务提交成功后触发 QQ 推送。
 *
 * <p>使用 {@link TransactionalEventListener} + {@link TransactionPhase#AFTER_COMMIT}，
 * 保证只有 alarm_record 成功落库后才推送，避免事务回滚后用户收到"幽灵告警"。
 *
 * <p>监听方法内部 catch 所有异常，确保推送失败不影响告警主流程（告警已落库即为成功）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlarmEventListener {

    private final OneBotPushService oneBotPushService;

    /**
     * 告警触发事件回调：转发给 OneBotPushService 推送到 QQ。
     *
     * @param event 告警触发事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAlarmTriggered(AlarmTriggeredEvent event) {
        try {
            log.info("收到告警事件，触发 QQ 推送: alarmId={}, deviceId={}, level={}",
                    event.alarmId(), event.deviceId(), event.riskLevel());
            oneBotPushService.pushAlarm(event);
        } catch (Exception e) {
            log.warn("告警事件 QQ 推送失败: alarmId={}, reason={}", event.alarmId(), e.getMessage());
        }
    }
}

package com.chinasoft.postdata.service;

import com.chinasoft.postdata.model.ControlSignal;
import com.chinasoft.postdata.mqtt.ControlSignalPublisher;
import com.chinasoft.postdata.repository.ControlStateReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DatabaseControlMonitor {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseControlMonitor.class);
    private static final List<String> CONTROL_ORDER =
            Arrays.asList("switch", "buzzer", "alarm_light");

    private final ControlStateReader stateReader;
    private final ControlStateMapper stateMapper;
    private final ControlSignalPublisher publisher;
    private final String deviceId;
    private final Map<String, String> lastPublishedStatuses = new HashMap<>();

    public DatabaseControlMonitor(
            ControlStateReader stateReader,
            ControlStateMapper stateMapper,
            ControlSignalPublisher publisher,
            @Value("${control.device-id:SMK-001}") String deviceId) {
        this.stateReader = stateReader;
        this.stateMapper = stateMapper;
        this.publisher = publisher;
        this.deviceId = deviceId;
    }

    @Scheduled(fixedDelayString = "${control.poll-interval-ms:1000}")
    public void poll() {
        final Map<String, String> states;
        try {
            states = stateReader.readStates(deviceId);
        } catch (Exception exception) {
            logger.error("读取 device_control 失败，将在下一轮重试", exception);
            return;
        }

        for (String controlType : CONTROL_ORDER) {
            String status = states.get(controlType);
            if (status == null) {
                logger.warn("缺少控制记录: deviceId={}, controlType={}", deviceId, controlType);
                continue;
            }
            if (status.equalsIgnoreCase(lastPublishedStatuses.get(controlType))) {
                continue;
            }

            try {
                ControlSignal signal = stateMapper.map(controlType, status);
                publisher.publish(signal);
                lastPublishedStatuses.put(controlType, status.toLowerCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                logger.warn("忽略非法控制状态: deviceId={}, controlType={}, status={}, reason={}",
                        deviceId, controlType, status, exception.getMessage());
            } catch (Exception exception) {
                logger.error("发送控制信号失败，将在下一轮重试: controlType=" + controlType, exception);
            }
        }
    }
}

package com.chinasoft.postdata.service;

import com.chinasoft.postdata.model.ControlSignal;
import com.chinasoft.postdata.mqtt.ControlSignalPublisher;
import com.chinasoft.postdata.repository.ControlStateReader;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DatabaseControlMonitor {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseControlMonitor.class);
    private static final String SWITCH_KEY = "switch";
    private static final String THRESHOLD_KEY = "warning_threshold";

    private final ControlStateReader stateReader;
    private final ControlStateMapper stateMapper;
    private final ControlSignalPublisher publisher;
    private final String deviceId;
    private final Map<String, Integer> lastPublishedValues = new HashMap<>();

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
            logger.error("Failed to read control state; retrying on the next poll", exception);
            return;
        }

        Integer sensor = mapSensor(states.get(SWITCH_KEY));
        Integer threshold = mapThreshold(states.get(THRESHOLD_KEY));
        boolean sensorChanged = sensor != null && !sensor.equals(lastPublishedValues.get(SWITCH_KEY));
        boolean thresholdChanged = threshold != null && !threshold.equals(lastPublishedValues.get(THRESHOLD_KEY));
        if (!sensorChanged && !thresholdChanged) {
            return;
        }

        try {
            publisher.publish(new ControlSignal(
                    sensorChanged ? sensor : null,
                    thresholdChanged ? threshold : null));
            if (sensorChanged) {
                lastPublishedValues.put(SWITCH_KEY, sensor);
            }
            if (thresholdChanged) {
                lastPublishedValues.put(THRESHOLD_KEY, threshold);
            }
        } catch (Exception exception) {
            logger.error("Failed to publish control signal; retrying on the next poll", exception);
        }
    }

    private Integer mapSensor(String status) {
        if (status == null) {
            logger.warn("Missing device_control row: deviceId={}, controlType=switch", deviceId);
            return null;
        }
        try {
            return stateMapper.mapSensor(status);
        } catch (IllegalArgumentException exception) {
            logger.warn("Ignoring invalid switch status: deviceId={}, status={}, reason={}",
                    deviceId, status, exception.getMessage());
            return null;
        }
    }

    private Integer mapThreshold(String settingValue) {
        if (settingValue == null) {
            logger.warn("Missing system_setting row: settingKey=warning_threshold");
            return null;
        }
        try {
            return stateMapper.mapThreshold(settingValue);
        } catch (IllegalArgumentException exception) {
            logger.warn("Ignoring invalid warning_threshold: value={}, reason={}",
                    settingValue, exception.getMessage());
            return null;
        }
    }
}

package com.chinasoft.postdata.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.chinasoft.postdata.model.ControlSignal;
import com.chinasoft.postdata.mqtt.ControlSignalPublisher;
import com.chinasoft.postdata.repository.ControlStateReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DatabaseControlMonitorTest {

    @Test
    void publishesFullStateOnStartupAndOnlyChangesAfterward() {
        MutableStateReader reader = new MutableStateReader(states("on", "off", "on"));
        RecordingPublisher publisher = new RecordingPublisher();
        DatabaseControlMonitor monitor = monitor(reader, publisher);

        monitor.poll();
        assertEquals(Arrays.asList("{\"switch\":1}", "{\"buzzer\":0}", "{\"led\":1}"), publisher.payloads);

        monitor.poll();
        assertEquals(3, publisher.payloads.size());

        reader.states.put("buzzer", "on");
        monitor.poll();
        assertEquals(Arrays.asList("{\"switch\":1}", "{\"buzzer\":0}", "{\"led\":1}", "{\"buzzer\":1}"),
                publisher.payloads);
    }

    @Test
    void retriesUntilPublishSucceeds() {
        MutableStateReader reader = new MutableStateReader(states("on", "off", "off"));
        RecordingPublisher publisher = new RecordingPublisher();
        publisher.failuresRemaining = 1;
        DatabaseControlMonitor monitor = monitor(reader, publisher);

        monitor.poll();
        monitor.poll();

        assertEquals(2, publisher.switchAttempts);
        assertEquals(3, publisher.payloads.size());
    }

    @Test
    void ignoresInvalidOrMissingRecords() {
        Map<String, String> invalid = new LinkedHashMap<>();
        invalid.put("switch", "invalid");
        invalid.put("buzzer", "on");
        RecordingPublisher publisher = new RecordingPublisher();

        monitor(new MutableStateReader(invalid), publisher).poll();

        assertEquals(Arrays.asList("{\"buzzer\":1}"), publisher.payloads);
    }

    private DatabaseControlMonitor monitor(ControlStateReader reader, ControlSignalPublisher publisher) {
        return new DatabaseControlMonitor(reader, new ControlStateMapper(), publisher, "SMK-001");
    }

    private Map<String, String> states(String switchStatus, String buzzerStatus, String ledStatus) {
        Map<String, String> states = new LinkedHashMap<>();
        states.put("switch", switchStatus);
        states.put("buzzer", buzzerStatus);
        states.put("alarm_light", ledStatus);
        return states;
    }

    private static class MutableStateReader implements ControlStateReader {
        private final Map<String, String> states;

        private MutableStateReader(Map<String, String> states) {
            this.states = states;
        }

        @Override
        public Map<String, String> readStates(String deviceId) {
            return new LinkedHashMap<>(states);
        }
    }

    private static class RecordingPublisher implements ControlSignalPublisher {
        private final List<String> payloads = new ArrayList<>();
        private int failuresRemaining;
        private int switchAttempts;

        @Override
        public void publish(ControlSignal signal) throws Exception {
            if ("switch".equals(signal.getField())) {
                switchAttempts++;
            }
            if (failuresRemaining > 0) {
                failuresRemaining--;
                throw new Exception("temporary MQTT failure");
            }
            payloads.add(signal.toJson());
        }
    }
}

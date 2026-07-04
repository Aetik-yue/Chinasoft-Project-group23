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
    void publishesCombinedStateOnStartupAndOnlyChangedValuesAfterward() {
        MutableStateReader reader = new MutableStateReader(states("on", "100"));
        RecordingPublisher publisher = new RecordingPublisher();
        DatabaseControlMonitor monitor = monitor(reader, publisher);

        monitor.poll();
        assertEquals(Arrays.asList("{\"sensor\":1,\"threshold\":100}"), publisher.payloads);

        monitor.poll();
        assertEquals(1, publisher.payloads.size());

        reader.states.put("switch", "off");
        monitor.poll();
        assertEquals("{\"sensor\":0}", publisher.payloads.get(1));

        reader.states.put("warning_threshold", "250");
        monitor.poll();
        assertEquals("{\"threshold\":250}", publisher.payloads.get(2));

        reader.states.put("switch", "on");
        reader.states.put("warning_threshold", "300");
        monitor.poll();
        assertEquals("{\"sensor\":1,\"threshold\":300}", publisher.payloads.get(3));
    }

    @Test
    void retriesCombinedSignalUntilPublishSucceeds() {
        RecordingPublisher publisher = new RecordingPublisher();
        publisher.failuresRemaining = 1;
        DatabaseControlMonitor monitor = monitor(
                new MutableStateReader(states("on", "200")), publisher);

        monitor.poll();
        monitor.poll();

        assertEquals(2, publisher.attempts);
        assertEquals(Arrays.asList("{\"sensor\":1,\"threshold\":200}"), publisher.payloads);
    }

    @Test
    void invalidOrMissingValueDoesNotBlockTheOtherValue() {
        Map<String, String> invalidSensor = new LinkedHashMap<>();
        invalidSensor.put("switch", "invalid");
        invalidSensor.put("warning_threshold", "200");
        RecordingPublisher firstPublisher = new RecordingPublisher();

        monitor(new MutableStateReader(invalidSensor), firstPublisher).poll();
        assertEquals(Arrays.asList("{\"threshold\":200}"), firstPublisher.payloads);

        Map<String, String> invalidThreshold = new LinkedHashMap<>();
        invalidThreshold.put("switch", "off");
        invalidThreshold.put("warning_threshold", "10001");
        RecordingPublisher secondPublisher = new RecordingPublisher();

        monitor(new MutableStateReader(invalidThreshold), secondPublisher).poll();
        assertEquals(Arrays.asList("{\"sensor\":0}"), secondPublisher.payloads);

        Map<String, String> missingThreshold = new LinkedHashMap<>();
        missingThreshold.put("switch", "on");
        RecordingPublisher thirdPublisher = new RecordingPublisher();

        monitor(new MutableStateReader(missingThreshold), thirdPublisher).poll();
        assertEquals(Arrays.asList("{\"sensor\":1}"), thirdPublisher.payloads);
    }

    private DatabaseControlMonitor monitor(ControlStateReader reader, ControlSignalPublisher publisher) {
        return new DatabaseControlMonitor(reader, new ControlStateMapper(), publisher, "SMK-001");
    }

    private Map<String, String> states(String switchStatus, String threshold) {
        Map<String, String> states = new LinkedHashMap<>();
        states.put("switch", switchStatus);
        states.put("warning_threshold", threshold);
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
        private int attempts;

        @Override
        public void publish(ControlSignal signal) throws Exception {
            attempts++;
            if (failuresRemaining > 0) {
                failuresRemaining--;
                throw new Exception("temporary MQTT failure");
            }
            payloads.add(signal.toJson());
        }
    }
}

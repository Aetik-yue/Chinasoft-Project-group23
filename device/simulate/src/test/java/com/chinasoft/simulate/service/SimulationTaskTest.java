package com.chinasoft.simulate.service;

import static org.junit.Assert.assertEquals;

import com.chinasoft.simulate.config.SimulationProperties;
import com.chinasoft.simulate.mqtt.SimulationPublisher;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.Test;

public class SimulationTaskTest {

    @Test
    public void publishesTemperatureThenHumidityAsSeparateOneDecimalJson() {
        SimulationProperties properties = new SimulationProperties();
        properties.setTemperatureStandardDeviation(0.0);
        properties.setHumidityStandardDeviation(0.0);
        NormalSensorValueGenerator generator =
                new NormalSensorValueGenerator(properties, new Random(1L));
        RecordingPublisher publisher = new RecordingPublisher();

        new SimulationTask(generator, publisher).publishCycle();

        assertEquals(2, publisher.payloads.size());
        assertEquals("{\"℃\":25.0}", publisher.payloads.get(0));
        assertEquals("{\"%RH\":50.0}", publisher.payloads.get(1));
    }

    @Test
    public void onePublishFailureDoesNotBlockTheOtherMessage() {
        SimulationProperties properties = new SimulationProperties();
        properties.setTemperatureStandardDeviation(0.0);
        properties.setHumidityStandardDeviation(0.0);
        RecordingPublisher publisher = new RecordingPublisher();
        publisher.failFirst = true;

        new SimulationTask(
                new NormalSensorValueGenerator(properties, new Random(1L)), publisher).publishCycle();

        assertEquals(1, publisher.payloads.size());
        assertEquals("{\"%RH\":50.0}", publisher.payloads.get(0));
    }

    private static class RecordingPublisher implements SimulationPublisher {
        private final List<String> payloads = new ArrayList<String>();
        private boolean failFirst;

        @Override
        public void publish(String payload) throws Exception {
            if (failFirst) {
                failFirst = false;
                throw new Exception("test failure");
            }
            payloads.add(payload);
        }
    }
}

package com.chinasoft.simulate.service;

import com.chinasoft.simulate.mqtt.SimulationPublisher;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SimulationTask {

    private static final Logger logger = LoggerFactory.getLogger(SimulationTask.class);

    private final NormalSensorValueGenerator generator;
    private final SimulationPublisher publisher;

    public SimulationTask(NormalSensorValueGenerator generator, SimulationPublisher publisher) {
        this.generator = generator;
        this.publisher = publisher;
    }

    @Scheduled(fixedDelayString = "${simulation.interval-ms:1000}")
    public void publishCycle() {
        publishSafely(toJson("℃", generator.nextTemperature()));
        publishSafely(toJson("%RH", generator.nextHumidity()));
    }

    String toJson(String field, double value) {
        return String.format(Locale.ROOT, "{\"%s\":%.1f}", field, value);
    }

    private void publishSafely(String payload) {
        try {
            publisher.publish(payload);
        } catch (Exception exception) {
            logger.error("模拟数据发送失败，将跳过本条数据: payload=" + payload, exception);
        }
    }
}

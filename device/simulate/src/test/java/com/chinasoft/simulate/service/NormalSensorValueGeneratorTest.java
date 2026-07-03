package com.chinasoft.simulate.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.chinasoft.simulate.config.SimulationProperties;
import java.util.Random;
import org.junit.Test;

public class NormalSensorValueGeneratorTest {

    @Test
    public void generatedValuesStayInRangeAndMeansApproachTargets() {
        SimulationProperties properties = new SimulationProperties();
        NormalSensorValueGenerator generator =
                new NormalSensorValueGenerator(properties, new Random(230725L));
        double temperatureSum = 0.0;
        double humiditySum = 0.0;
        int samples = 20000;

        for (int index = 0; index < samples; index++) {
            double temperature = generator.nextTemperature();
            double humidity = generator.nextHumidity();
            assertTrue(temperature >= 0.0 && temperature <= 50.0);
            assertTrue(humidity >= 1.0 && humidity <= 99.0);
            assertEquals(temperature * 10.0, Math.rint(temperature * 10.0), 0.000001);
            assertEquals(humidity * 10.0, Math.rint(humidity * 10.0), 0.000001);
            temperatureSum += temperature;
            humiditySum += humidity;
        }

        assertEquals(25.0, temperatureSum / samples, 0.1);
        assertEquals(50.0, humiditySum / samples, 0.2);
    }
}

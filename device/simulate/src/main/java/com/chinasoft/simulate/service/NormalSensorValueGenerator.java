package com.chinasoft.simulate.service;

import com.chinasoft.simulate.config.SimulationProperties;
import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NormalSensorValueGenerator {

    private final SimulationProperties properties;
    private final Random random;

    @Autowired
    public NormalSensorValueGenerator(SimulationProperties properties) {
        this(properties, new Random());
    }

    public NormalSensorValueGenerator(SimulationProperties properties, Random random) {
        this.properties = properties;
        this.random = random;
        validateDistribution("temperature", properties.getTemperatureMean(),
                properties.getTemperatureStandardDeviation(),
                properties.getTemperatureMinimum(), properties.getTemperatureMaximum());
        validateDistribution("humidity", properties.getHumidityMean(),
                properties.getHumidityStandardDeviation(),
                properties.getHumidityMinimum(), properties.getHumidityMaximum());
    }

    public double nextTemperature() {
        return sample(properties.getTemperatureMean(),
                properties.getTemperatureStandardDeviation(),
                properties.getTemperatureMinimum(), properties.getTemperatureMaximum());
    }

    public double nextHumidity() {
        return sample(properties.getHumidityMean(),
                properties.getHumidityStandardDeviation(),
                properties.getHumidityMinimum(), properties.getHumidityMaximum());
    }

    private double sample(double mean, double standardDeviation, double minimum, double maximum) {
        double value;
        do {
            value = mean + random.nextGaussian() * standardDeviation;
        } while (value < minimum || value > maximum);
        return Math.round(value * 10.0) / 10.0;
    }

    private void validateDistribution(String name, double mean, double standardDeviation,
                                      double minimum, double maximum) {
        if (!Double.isFinite(mean) || !Double.isFinite(standardDeviation)
                || !Double.isFinite(minimum) || !Double.isFinite(maximum)
                || standardDeviation < 0.0 || minimum > maximum
                || mean < minimum || mean > maximum) {
            throw new IllegalArgumentException("Invalid " + name + " distribution settings");
        }
    }
}

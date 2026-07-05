package com.chinasoft.simulate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "simulation")
public class SimulationProperties {

    private long intervalMs = 1000L;
    private double temperatureMean = 25.0;
    private double temperatureStandardDeviation = 0.2;
    private double temperatureMinimum = 18.0;
    private double temperatureMaximum = 32.0;
    private double humidityMean = 50.0;
    private double humidityStandardDeviation = 1.0;
    private double humidityMinimum = 35.0;
    private double humidityMaximum = 65.0;

    public long getIntervalMs() { return intervalMs; }
    public void setIntervalMs(long intervalMs) { this.intervalMs = intervalMs; }
    public double getTemperatureMean() { return temperatureMean; }
    public void setTemperatureMean(double temperatureMean) { this.temperatureMean = temperatureMean; }
    public double getTemperatureStandardDeviation() { return temperatureStandardDeviation; }
    public void setTemperatureStandardDeviation(double value) { this.temperatureStandardDeviation = value; }
    public double getTemperatureMinimum() { return temperatureMinimum; }
    public void setTemperatureMinimum(double temperatureMinimum) { this.temperatureMinimum = temperatureMinimum; }
    public double getTemperatureMaximum() { return temperatureMaximum; }
    public void setTemperatureMaximum(double temperatureMaximum) { this.temperatureMaximum = temperatureMaximum; }
    public double getHumidityMean() { return humidityMean; }
    public void setHumidityMean(double humidityMean) { this.humidityMean = humidityMean; }
    public double getHumidityStandardDeviation() { return humidityStandardDeviation; }
    public void setHumidityStandardDeviation(double value) { this.humidityStandardDeviation = value; }
    public double getHumidityMinimum() { return humidityMinimum; }
    public void setHumidityMinimum(double humidityMinimum) { this.humidityMinimum = humidityMinimum; }
    public double getHumidityMaximum() { return humidityMaximum; }
    public void setHumidityMaximum(double humidityMaximum) { this.humidityMaximum = humidityMaximum; }
}

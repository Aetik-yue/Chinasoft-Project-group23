package com.chinasoft.getdata.model;

public class SensorDataMessage {

    private final SensorDataType type;
    private final double value;
    private final String riskLevel;

    public SensorDataMessage(SensorDataType type, double value, String riskLevel) {
        this.type = type;
        this.value = value;
        this.riskLevel = riskLevel;
    }

    public SensorDataType getType() {
        return type;
    }

    public double getValue() {
        return value;
    }

    public String getRiskLevel() {
        return riskLevel;
    }
}

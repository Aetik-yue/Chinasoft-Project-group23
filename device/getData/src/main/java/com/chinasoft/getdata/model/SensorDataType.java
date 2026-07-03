package com.chinasoft.getdata.model;

public enum SensorDataType {
    SMOKE("ppm", 0.0, 999.0),
    TEMPERATURE("℃", 0.0, 50.0),
    HUMIDITY("%RH", 1.0, 99.0);

    private final String jsonField;
    private final double minimum;
    private final double maximum;

    SensorDataType(String jsonField, double minimum, double maximum) {
        this.jsonField = jsonField;
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public String getJsonField() {
        return jsonField;
    }

    public double getMinimum() {
        return minimum;
    }

    public double getMaximum() {
        return maximum;
    }
}

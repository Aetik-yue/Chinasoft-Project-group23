package com.chinasoft.postdata.model;

public class ControlSignal {

    private final Integer sensor;
    private final Integer threshold;

    public ControlSignal(Integer sensor, Integer threshold) {
        if (sensor == null && threshold == null) {
            throw new IllegalArgumentException("At least one control value is required");
        }
        this.sensor = sensor;
        this.threshold = threshold;
    }

    public Integer getSensor() {
        return sensor;
    }

    public Integer getThreshold() {
        return threshold;
    }

    public String toJson() {
        StringBuilder json = new StringBuilder("{");
        if (sensor != null) {
            json.append("\"sensor\":").append(sensor);
        }
        if (threshold != null) {
            if (sensor != null) {
                json.append(',');
            }
            json.append("\"threshold\":").append(threshold);
        }
        return json.append('}').toString();
    }
}

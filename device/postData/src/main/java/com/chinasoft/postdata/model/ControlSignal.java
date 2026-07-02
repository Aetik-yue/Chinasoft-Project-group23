package com.chinasoft.postdata.model;

public class ControlSignal {

    private final String field;
    private final int value;

    public ControlSignal(String field, int value) {
        this.field = field;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public int getValue() {
        return value;
    }

    public String toJson() {
        return "{\"" + field + "\":" + value + "}";
    }
}

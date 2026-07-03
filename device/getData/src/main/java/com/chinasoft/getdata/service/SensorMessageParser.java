package com.chinasoft.getdata.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.chinasoft.getdata.model.SensorDataMessage;
import com.chinasoft.getdata.model.SensorDataType;
import org.springframework.stereotype.Component;

@Component
public class SensorMessageParser {

    public SensorDataMessage parse(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            throw new IllegalArgumentException("MQTT payload cannot be empty");
        }

        String normalizedPayload = payload.trim();
        if (!normalizedPayload.startsWith("{") || !normalizedPayload.endsWith("}")) {
            throw new IllegalArgumentException("MQTT payload must be a JSON object");
        }

        final JSONObject json;
        try {
            json = JSONUtil.parseObj(normalizedPayload);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("MQTT payload is not valid JSON", exception);
        }

        SensorDataType selectedType = null;
        int recognizedFields = 0;
        for (SensorDataType type : SensorDataType.values()) {
            if (json.containsKey(type.getJsonField())) {
                selectedType = type;
                recognizedFields++;
            }
        }
        if (recognizedFields != 1) {
            throw new IllegalArgumentException("payload must contain exactly one of: ppm, ℃, %RH");
        }

        Object rawValue = json.get(selectedType.getJsonField());
        if (!(rawValue instanceof Number)) {
            throw invalidValue(selectedType);
        }
        double value = ((Number) rawValue).doubleValue();
        if (!Double.isFinite(value)
                || value < selectedType.getMinimum()
                || value > selectedType.getMaximum()) {
            throw invalidValue(selectedType);
        }

        String riskLevel = selectedType == SensorDataType.SMOKE
                ? RiskLevelCalculator.calculate(value)
                : null;
        return new SensorDataMessage(selectedType, value, riskLevel);
    }

    private IllegalArgumentException invalidValue(SensorDataType type) {
        return new IllegalArgumentException(type.getJsonField() + " must be a number between "
                + type.getMinimum() + " and " + type.getMaximum());
    }
}

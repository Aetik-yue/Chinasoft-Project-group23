package com.chinasoft.getdata.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.chinasoft.getdata.model.SmokeDataMessage;
import org.springframework.stereotype.Component;

@Component
public class SmokeMessageParser {

    public SmokeDataMessage parse(String payload) {
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

        Object ppmRaw = json.get("ppm");
        if (!(ppmRaw instanceof Number)) {
            throw new IllegalArgumentException("ppm must be a number between 0 and 999");
        }

        double ppm = ((Number) ppmRaw).doubleValue();
        if (!Double.isFinite(ppm) || ppm < 0 || ppm > 999) {
            throw new IllegalArgumentException("ppm must be a number between 0 and 999");
        }

        return new SmokeDataMessage(ppm, RiskLevelCalculator.calculate(ppm));
    }
}

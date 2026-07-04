package com.chinasoft.postdata.service;

import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ControlStateMapper {

    public int mapSensor(String status) {
        String normalizedStatus = normalize(status, "status");
        if ("on".equals(normalizedStatus)) {
            return 1;
        }
        if ("off".equals(normalizedStatus)) {
            return 0;
        }
        throw new IllegalArgumentException("Unsupported status: " + status);
    }

    public int mapThreshold(String settingValue) {
        String normalizedValue = normalize(settingValue, "warning_threshold");
        final int threshold;
        try {
            threshold = Integer.parseInt(normalizedValue);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("warning_threshold must be an integer: " + settingValue);
        }
        if (threshold < 1 || threshold > 10000) {
            throw new IllegalArgumentException("warning_threshold must be between 1 and 10000: " + settingValue);
        }
        return threshold;
    }

    private String normalize(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}

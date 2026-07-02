package com.chinasoft.postdata.service;

import com.chinasoft.postdata.model.ControlSignal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ControlStateMapper {

    private static final Map<String, String> FIELD_BY_CONTROL_TYPE;

    static {
        Map<String, String> fields = new HashMap<>();
        fields.put("switch", "switch");
        fields.put("buzzer", "buzzer");
        fields.put("alarm_light", "led");
        FIELD_BY_CONTROL_TYPE = Collections.unmodifiableMap(fields);
    }

    public ControlSignal map(String controlType, String status) {
        String normalizedType = normalize(controlType, "control_type");
        String normalizedStatus = normalize(status, "status");
        String field = FIELD_BY_CONTROL_TYPE.get(normalizedType);
        if (field == null) {
            throw new IllegalArgumentException("不支持的 control_type: " + controlType);
        }

        if ("on".equals(normalizedStatus)) {
            return new ControlSignal(field, 1);
        }
        if ("off".equals(normalizedStatus)) {
            return new ControlSignal(field, 0);
        }
        throw new IllegalArgumentException("不支持的 status: " + status);
    }

    private String normalize(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " 不能为空");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}

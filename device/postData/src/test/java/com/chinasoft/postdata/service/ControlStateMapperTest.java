package com.chinasoft.postdata.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ControlStateMapperTest {

    private final ControlStateMapper mapper = new ControlStateMapper();

    @Test
    void mapsDatabaseTypesAndStatusesToHardwareJson() {
        assertEquals("{\"switch\":1}", mapper.map("switch", "on").toJson());
        assertEquals("{\"buzzer\":0}", mapper.map("buzzer", "off").toJson());
        assertEquals("{\"led\":1}", mapper.map("alarm_light", "on").toJson());
    }

    @Test
    void rejectsUnknownTypeAndStatus() {
        assertThrows(IllegalArgumentException.class, () -> mapper.map("fan", "on"));
        assertThrows(IllegalArgumentException.class, () -> mapper.map("switch", "standby"));
        assertThrows(IllegalArgumentException.class, () -> mapper.map(null, "on"));
    }
}

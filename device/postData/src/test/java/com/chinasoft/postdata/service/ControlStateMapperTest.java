package com.chinasoft.postdata.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.chinasoft.postdata.model.ControlSignal;
import org.junit.jupiter.api.Test;

class ControlStateMapperTest {

    private final ControlStateMapper mapper = new ControlStateMapper();

    @Test
    void mapsSwitchStatusToSensorValue() {
        assertEquals(1, mapper.mapSensor("on"));
        assertEquals(0, mapper.mapSensor(" OFF "));
    }

    @Test
    void mapsThresholdWithinFirmwareRange() {
        assertEquals(1, mapper.mapThreshold("1"));
        assertEquals(100, mapper.mapThreshold(" 100 "));
        assertEquals(10000, mapper.mapThreshold("10000"));
    }

    @Test
    void rejectsInvalidSwitchStatusAndThreshold() {
        assertThrows(IllegalArgumentException.class, () -> mapper.mapSensor("standby"));
        assertThrows(IllegalArgumentException.class, () -> mapper.mapSensor(null));
        assertThrows(IllegalArgumentException.class, () -> mapper.mapThreshold("0"));
        assertThrows(IllegalArgumentException.class, () -> mapper.mapThreshold("10001"));
        assertThrows(IllegalArgumentException.class, () -> mapper.mapThreshold("10.5"));
        assertThrows(IllegalArgumentException.class, () -> mapper.mapThreshold("invalid"));
        assertThrows(IllegalArgumentException.class, () -> mapper.mapThreshold(null));
    }

    @Test
    void serializesSingleAndCombinedSignalsInDeterministicOrder() {
        assertEquals("{\"sensor\":1}", new ControlSignal(1, null).toJson());
        assertEquals("{\"threshold\":200}", new ControlSignal(null, 200).toJson());
        assertEquals("{\"sensor\":0,\"threshold\":300}", new ControlSignal(0, 300).toJson());
        assertThrows(IllegalArgumentException.class, () -> new ControlSignal(null, null));
    }
}

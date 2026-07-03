package com.chinasoft.getdata.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.chinasoft.getdata.model.SensorDataMessage;
import com.chinasoft.getdata.model.SensorDataType;
import org.junit.Test;

public class SensorMessageParserTest {

    private final SensorMessageParser parser = new SensorMessageParser();

    @Test
    public void parsesAllThreeSensorFormats() {
        SensorDataMessage smoke = parser.parse("{\"ppm\":86.5}");
        SensorDataMessage temperature = parser.parse("{\"℃\":23}");
        SensorDataMessage humidity = parser.parse("{\"%RH\":50.5}");

        assertEquals(SensorDataType.SMOKE, smoke.getType());
        assertEquals(86.5, smoke.getValue(), 0.000001);
        assertEquals("normal", smoke.getRiskLevel());
        assertEquals(SensorDataType.TEMPERATURE, temperature.getType());
        assertEquals(23.0, temperature.getValue(), 0.000001);
        assertNull(temperature.getRiskLevel());
        assertEquals(SensorDataType.HUMIDITY, humidity.getType());
        assertEquals(50.5, humidity.getValue(), 0.000001);
        assertNull(humidity.getRiskLevel());
    }

    @Test
    public void acceptsAllRangeBoundaries() {
        assertEquals(0.0, parser.parse("{\"ppm\":0}").getValue(), 0.000001);
        assertEquals(999.0, parser.parse("{\"ppm\":999}").getValue(), 0.000001);
        assertEquals(0.0, parser.parse("{\"℃\":0}").getValue(), 0.000001);
        assertEquals(50.0, parser.parse("{\"℃\":50}").getValue(), 0.000001);
        assertEquals(1.0, parser.parse("{\"%RH\":1}").getValue(), 0.000001);
        assertEquals(99.0, parser.parse("{\"%RH\":99}").getValue(), 0.000001);
    }

    @Test
    public void keepsSmokeRiskBoundaries() {
        double[] values = {99.999, 100.0, 199.999, 200.0, 399.999, 400.0};
        String[] levels = {"normal", "low", "low", "medium", "medium", "high"};
        for (int index = 0; index < values.length; index++) {
            assertEquals(levels[index],
                    parser.parse("{\"ppm\":" + values[index] + "}").getRiskLevel());
        }
    }

    @Test
    public void rejectsMissingMixedNonNumericAndOutOfRangeValues() {
        String[] invalidPayloads = {
                "", "[]", "{}",
                "{\"ppm\":1,\"℃\":20}",
                "{\"℃\":20,\"%RH\":50}",
                "{\"ppm\":\"86.5\"}",
                "{\"℃\":null}",
                "{\"%RH\":true}",
                "{\"ppm\":-0.1}",
                "{\"ppm\":1000}",
                "{\"℃\":-0.1}",
                "{\"℃\":50.1}",
                "{\"%RH\":0.9}",
                "{\"%RH\":99.1}"
        };

        for (String payload : invalidPayloads) {
            try {
                parser.parse(payload);
                throw new AssertionError("Expected payload to be rejected: " + payload);
            } catch (IllegalArgumentException expected) {
                // expected
            }
        }
    }

    @Test
    public void ignoresMetadataButNotAdditionalSensorFields() {
        SensorDataMessage message = parser.parse(
                "{\"ppm\":150.25,\"deviceId\":\"OTHER\",\"source\":\"simulate\"}");
        assertEquals(SensorDataType.SMOKE, message.getType());
        assertEquals(150.25, message.getValue(), 0.000001);
        assertEquals("low", message.getRiskLevel());
    }
}

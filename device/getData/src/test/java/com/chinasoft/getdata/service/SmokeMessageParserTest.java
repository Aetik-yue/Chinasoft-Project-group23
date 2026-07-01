package com.chinasoft.getdata.service;

import static org.junit.Assert.assertEquals;

import com.chinasoft.getdata.model.SmokeDataMessage;
import org.junit.Test;

public class SmokeMessageParserTest {

    private final SmokeMessageParser parser = new SmokeMessageParser();

    @Test
    public void parsesFloatingPointPpm() {
        SmokeDataMessage message = parser.parse("{\"ppm\":86.5}");

        assertEquals(86.5, message.getPpm(), 0.000001);
        assertEquals("normal", message.getRiskLevel());
    }

    @Test
    public void parsesIntegerPpmAsNumber() {
        SmokeDataMessage message = parser.parse("{\"ppm\":200}");

        assertEquals(200.0, message.getPpm(), 0.000001);
        assertEquals("medium", message.getRiskLevel());
    }

    @Test
    public void ignoresUntrustedMetadataFields() {
        SmokeDataMessage message = parser.parse(
                "{\"ppm\":150.25,\"deviceId\":\"OTHER\","
                        + "\"recordTime\":\"2000-01-01 00:00:00\",\"source\":\"simulate\"}");

        assertEquals(150.25, message.getPpm(), 0.000001);
        assertEquals("low", message.getRiskLevel());
    }

    @Test
    public void calculatesAllRiskBoundaries() {
        double[] values = {0.0, 99.999, 100.0, 199.999, 200.0, 399.999, 400.0, 999.0};
        String[] levels = {"normal", "normal", "low", "low", "medium", "medium", "high", "high"};

        for (int index = 0; index < values.length; index++) {
            SmokeDataMessage message = parser.parse("{\"ppm\":" + values[index] + "}");
            assertEquals(levels[index], message.getRiskLevel());
        }
    }

    @Test
    public void rejectsInvalidMessages() {
        String[] invalidPayloads = {
                "",
                "[]",
                "{}",
                "{\"ppm\":null}",
                "{\"ppm\":\"86.5\"}",
                "{\"ppm\":-0.1}",
                "{\"ppm\":1000}"
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
}

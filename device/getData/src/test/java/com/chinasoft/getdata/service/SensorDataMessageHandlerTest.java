package com.chinasoft.getdata.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.chinasoft.getdata.config.DatabaseProperties;
import com.chinasoft.getdata.model.SensorDataMessage;
import com.chinasoft.getdata.model.SensorDataType;
import com.chinasoft.getdata.repository.SensorDataWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class SensorDataMessageHandlerTest {

    @Test
    public void writesEachValidSensorMessageOnce() {
        CountingWriter writer = new CountingWriter(false);
        SensorDataMessageHandler handler = new SensorDataMessageHandler(new SensorMessageParser(), writer, new DatabaseProperties());

        assertTrue(handler.handle("{\"ppm\":86.5}"));
        assertTrue(handler.handle("{\"℃\":25.2}"));
        assertTrue(handler.handle("{\"%RH\":49.8}"));
        assertEquals(3, writer.messages.size());
        assertEquals(SensorDataType.SMOKE, writer.messages.get(0).getType());
        assertEquals(SensorDataType.TEMPERATURE, writer.messages.get(1).getType());
        assertEquals(SensorDataType.HUMIDITY, writer.messages.get(2).getType());
    }

    @Test
    public void doesNotWriteInvalidMessage() {
        CountingWriter writer = new CountingWriter(false);
        SensorDataMessageHandler handler = new SensorDataMessageHandler(new SensorMessageParser(), writer, new DatabaseProperties());

        assertFalse(handler.handle("{\"℃\":51}"));
        assertEquals(0, writer.messages.size());
    }

    @Test
    public void reportsDatabaseFailureWithoutThrowing() {
        CountingWriter writer = new CountingWriter(true);
        SensorDataMessageHandler handler = new SensorDataMessageHandler(new SensorMessageParser(), writer, new DatabaseProperties());

        assertFalse(handler.handle("{\"%RH\":50}"));
        assertEquals(1, writer.messages.size());
    }

    private static class CountingWriter implements SensorDataWriter {

        private final boolean fail;
        private final List<SensorDataMessage> messages = new ArrayList<SensorDataMessage>();

        private CountingWriter(boolean fail) {
            this.fail = fail;
        }

        @Override
        public void save(SensorDataMessage message) throws SQLException {
            messages.add(message);
            if (fail) {
                throw new SQLException("test failure");
            }
        }
    }
}

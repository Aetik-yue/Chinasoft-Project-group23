package com.chinasoft.getdata.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.chinasoft.getdata.model.SmokeDataMessage;
import com.chinasoft.getdata.repository.SmokeDataWriter;
import java.sql.SQLException;
import org.junit.Test;

public class SmokeDataMessageHandlerTest {

    @Test
    public void writesValidMessageOnce() {
        CountingWriter writer = new CountingWriter(false);
        SmokeDataMessageHandler handler = new SmokeDataMessageHandler(new SmokeMessageParser(), writer);

        assertTrue(handler.handle("{\"ppm\":86.5}"));
        assertTrue(writer.count == 1);
    }

    @Test
    public void doesNotWriteInvalidMessage() {
        CountingWriter writer = new CountingWriter(false);
        SmokeDataMessageHandler handler = new SmokeDataMessageHandler(new SmokeMessageParser(), writer);

        assertFalse(handler.handle("{\"ppm\":1000}"));
        assertTrue(writer.count == 0);
    }

    @Test
    public void reportsDatabaseFailureWithoutThrowing() {
        CountingWriter writer = new CountingWriter(true);
        SmokeDataMessageHandler handler = new SmokeDataMessageHandler(new SmokeMessageParser(), writer);

        assertFalse(handler.handle("{\"ppm\":86.5}"));
        assertTrue(writer.count == 1);
    }

    private static class CountingWriter implements SmokeDataWriter {

        private final boolean fail;
        private int count;

        private CountingWriter(boolean fail) {
            this.fail = fail;
        }

        @Override
        public void save(SmokeDataMessage message) throws SQLException {
            count++;
            if (fail) {
                throw new SQLException("test failure");
            }
        }
    }
}

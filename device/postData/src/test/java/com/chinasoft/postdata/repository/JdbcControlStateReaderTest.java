package com.chinasoft.postdata.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JdbcControlStateReaderTest {

    @Test
    void queryOnlyReadsExpectedControlRows() {
        String sql = JdbcControlStateReader.SELECT_SQL.toLowerCase();
        assertTrue(sql.startsWith("select control_type, status from device_control"));
        assertTrue(sql.contains("device_id = ?"));
        assertTrue(sql.contains("'switch'"));
        assertTrue(sql.contains("'buzzer'"));
        assertTrue(sql.contains("'alarm_light'"));
        assertFalse(sql.contains("insert"));
        assertFalse(sql.contains("update"));
        assertFalse(sql.contains("delete"));
    }
}

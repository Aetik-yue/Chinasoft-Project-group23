package com.chinasoft.postdata.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JdbcControlStateReaderTest {

    @Test
    void queryOnlyReadsSwitchAndWarningThreshold() {
        String sql = JdbcControlStateReader.SELECT_SQL.toLowerCase();
        assertTrue(sql.contains("from device_control"));
        assertTrue(sql.contains("device_id = ?"));
        assertTrue(sql.contains("control_type = 'switch'"));
        assertTrue(sql.contains("from system_setting"));
        assertTrue(sql.contains("setting_key = 'warning_threshold'"));
        assertFalse(sql.contains("buzzer"));
        assertFalse(sql.contains("alarm_light"));
        assertFalse(sql.contains("insert"));
        assertFalse(sql.contains("update"));
        assertFalse(sql.contains("delete"));
    }
}

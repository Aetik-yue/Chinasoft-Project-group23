package com.chinasoft.getdata.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class JdbcSmokeDataRepositoryTest {

    @Test
    public void sqlOnlyInsertsIntoSmokeData() {
        String normalized = JdbcSmokeDataRepository.INSERT_SQL
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();

        assertEquals(
                "insert into smoke_data (device_id, smoke_value, risk_level, record_time, source) "
                        + "values ('smk-001', ?, ?, now(), 'sensor')",
                normalized);
        assertFalse(normalized.contains("update "));
        assertFalse(normalized.contains("delete "));
        assertFalse(normalized.contains("create "));
        assertFalse(normalized.contains("drop "));
    }
}

package com.chinasoft.getdata.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class JdbcSensorDataRepositoryTest {

    @Test
    public void sqlOnlyInsertsIntoThreeTargetTables() {
        assertEquals(
                "insert into smoke_data (device_id, smoke_value, risk_level, record_time, source) "
                        + "values ('smk-001', ?, ?, now(), 'sensor')",
                normalize(JdbcSensorDataRepository.INSERT_SMOKE_SQL));
        assertEquals(
                "insert into temperature_data (device_id, temperature_value, record_time, source) "
                        + "values ('smk-001', ?, now(), 'simulate')",
                normalize(JdbcSensorDataRepository.INSERT_TEMPERATURE_SQL));
        assertEquals(
                "insert into humidity_data (device_id, humidity_value, record_time, source) "
                        + "values ('smk-001', ?, now(), 'simulate')",
                normalize(JdbcSensorDataRepository.INSERT_HUMIDITY_SQL));

        String allSql = normalize(JdbcSensorDataRepository.INSERT_SMOKE_SQL + " "
                + JdbcSensorDataRepository.INSERT_TEMPERATURE_SQL + " "
                + JdbcSensorDataRepository.INSERT_HUMIDITY_SQL);
        assertFalse(allSql.contains("update "));
        assertFalse(allSql.contains("delete "));
        assertFalse(allSql.contains("create "));
        assertFalse(allSql.contains("drop "));
        assertFalse(allSql.contains("alter "));
    }

    private String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim().toLowerCase();
    }
}

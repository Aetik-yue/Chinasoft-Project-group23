package com.chinasoft.postdata.repository;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcControlStateReader implements ControlStateReader {

    static final String SELECT_SQL =
            "SELECT 'switch' AS state_key, status AS state_value FROM device_control "
                    + "WHERE device_id = ? AND control_type = 'switch' "
                    + "UNION ALL "
                    + "SELECT 'warning_threshold' AS state_key, setting_value AS state_value "
                    + "FROM system_setting WHERE setting_key = 'warning_threshold'";

    private final JdbcTemplate jdbcTemplate;

    public JdbcControlStateReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Map<String, String> readStates(String deviceId) {
        Map<String, String> states = new LinkedHashMap<>();
        jdbcTemplate.query(SELECT_SQL, new Object[]{deviceId}, (RowCallbackHandler) resultSet ->
                states.put(resultSet.getString("state_key"), resultSet.getString("state_value")));
        return states;
    }
}

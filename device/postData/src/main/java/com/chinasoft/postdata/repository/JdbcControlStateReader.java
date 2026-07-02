package com.chinasoft.postdata.repository;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcControlStateReader implements ControlStateReader {

    static final String SELECT_SQL =
            "SELECT control_type, status FROM device_control "
                    + "WHERE device_id = ? "
                    + "AND control_type IN ('switch', 'buzzer', 'alarm_light')";

    private final JdbcTemplate jdbcTemplate;

    public JdbcControlStateReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Map<String, String> readStates(String deviceId) {
        Map<String, String> states = new LinkedHashMap<>();
        jdbcTemplate.query(SELECT_SQL, new Object[]{deviceId}, (RowCallbackHandler) resultSet ->
                states.put(resultSet.getString("control_type"), resultSet.getString("status")));
        return states;
    }
}

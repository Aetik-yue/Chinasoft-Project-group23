package com.chinasoft.getdata.repository;

import com.chinasoft.getdata.config.DatabaseProperties;
import com.chinasoft.getdata.model.SensorDataMessage;
import com.chinasoft.getdata.model.SensorDataType;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSensorDataRepository implements SensorDataWriter {

    static final String INSERT_SMOKE_SQL =
            "INSERT INTO smoke_data "
                    + "(device_id, smoke_value, risk_level, record_time, source) "
                    + "VALUES ('SMK-001', ?, ?, NOW(), 'sensor')";
    static final String INSERT_TEMPERATURE_SQL =
            "INSERT INTO temperature_data "
                    + "(device_id, temperature_value, record_time, source) "
                    + "VALUES ('SMK-001', ?, NOW(), 'simulate')";
    static final String INSERT_HUMIDITY_SQL =
            "INSERT INTO humidity_data "
                    + "(device_id, humidity_value, record_time, source) "
                    + "VALUES ('SMK-001', ?, NOW(), 'simulate')";

    private final DatabaseProperties properties;

    public JdbcSensorDataRepository(DatabaseProperties properties) {
        this.properties = properties;
    }

    @Override
    public void save(SensorDataMessage message) throws SQLException {
        String sql = sqlFor(message.getType());
        try (Connection connection = DriverManager.getConnection(
                properties.getUrl(), properties.getUsername(), properties.getPassword());
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDouble(1, message.getValue());
            if (message.getType() == SensorDataType.SMOKE) {
                statement.setString(2, message.getRiskLevel());
            }

            int affectedRows = statement.executeUpdate();
            if (affectedRows != 1) {
                throw new SQLException("Expected one sensor data row to be inserted, got " + affectedRows);
            }
        }
    }

    private String sqlFor(SensorDataType type) {
        switch (type) {
            case SMOKE:
                return INSERT_SMOKE_SQL;
            case TEMPERATURE:
                return INSERT_TEMPERATURE_SQL;
            case HUMIDITY:
                return INSERT_HUMIDITY_SQL;
            default:
                throw new IllegalArgumentException("Unsupported sensor data type: " + type);
        }
    }
}

package com.chinasoft.getdata.repository;

import com.chinasoft.getdata.config.DatabaseProperties;
import com.chinasoft.getdata.model.SmokeDataMessage;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSmokeDataRepository implements SmokeDataWriter {

    static final String INSERT_SQL =
            "INSERT INTO smoke_data "
                    + "(device_id, smoke_value, risk_level, record_time, source) "
                    + "VALUES ('SMK-001', ?, ?, NOW(), 'sensor')";

    private final DatabaseProperties properties;

    public JdbcSmokeDataRepository(DatabaseProperties properties) {
        this.properties = properties;
    }

    @Override
    public void save(SmokeDataMessage message) throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                properties.getUrl(), properties.getUsername(), properties.getPassword());
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setDouble(1, message.getPpm());
            statement.setString(2, message.getRiskLevel());

            int affectedRows = statement.executeUpdate();
            if (affectedRows != 1) {
                throw new SQLException("Expected one smoke_data row to be inserted, got " + affectedRows);
            }
        }
    }
}

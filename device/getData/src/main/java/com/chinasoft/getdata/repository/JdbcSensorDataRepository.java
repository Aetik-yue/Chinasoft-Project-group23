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

    /**
     * 烟雾告警阈值（ppm）。达到该值即视为触发告警，与后端 system_setting 中
     * warning_threshold 的默认档位保持一致（RiskLevelCalculator 以 200 作为
     * low/medium 边界）。仅用于回写 smoke_device.current_alarm_status，不影响
     * smoke_data 本身的 risk_level（后者由 RiskLevelCalculator 计算）。
     */
    static final int WARNING_THRESHOLD = 200;

    static final String ALARM_STATUS_ALARM = "alarm";
    static final String ALARM_STATUS_SAFE = "safe";

    static final String INSERT_SMOKE_SQL =
            "INSERT INTO smoke_data "
                    + "(device_id, smoke_value, risk_level, record_time, source) "
                    + "VALUES (?, ?, ?, NOW(), 'sensor')";
    static final String INSERT_TEMPERATURE_SQL =
            "INSERT INTO temperature_data "
                    + "(device_id, temperature_value, record_time, source) "
                    + "VALUES (?, ?, NOW(), 'simulate')";
    static final String INSERT_HUMIDITY_SQL =
            "INSERT INTO humidity_data "
                    + "(device_id, humidity_value, record_time, source) "
                    + "VALUES (?, ?, NOW(), 'simulate')";

    /**
     * 烟雾数据上报后回写 smoke_device：刷新心跳、置为在线，并更新当前烟雾值/
     * 风险等级/告警状态。否则后端 /api/smoke/realtime 会因 last_heartbeat 超时
     * （默认 60s）把设备判为离线，前端拿到的 smokeValue 即为 null。
     */
    static final String UPDATE_DEVICE_ON_SMOKE_SQL =
            "UPDATE smoke_device SET last_heartbeat = NOW(), online = 1, "
                    + "current_smoke_value = ?, current_risk_level = ?, current_alarm_status = ? "
                    + "WHERE device_id = ?";

    /**
     * 温湿度数据上报后仅刷新心跳并置为在线（后端实时接口不读温湿度字段，故不回写
     * current_*，但心跳仍需刷新以保持设备在线）。
     */
    static final String UPDATE_DEVICE_HEARTBEAT_SQL =
            "UPDATE smoke_device SET last_heartbeat = NOW(), online = 1 WHERE device_id = ?";

    private final DatabaseProperties properties;

    public JdbcSensorDataRepository(DatabaseProperties properties) {
        this.properties = properties;
    }

    @Override
    public void save(SensorDataMessage message) throws SQLException {
        String deviceId = properties.getDeviceId();
        String insertSql = sqlFor(message.getType());
        try (Connection connection = DriverManager.getConnection(
                properties.getUrl(), properties.getUsername(), properties.getPassword())) {
            try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
                statement.setString(1, deviceId);
                statement.setDouble(2, message.getValue());
                if (message.getType() == SensorDataType.SMOKE) {
                    statement.setString(3, message.getRiskLevel());
                }

                int affectedRows = statement.executeUpdate();
                if (affectedRows != 1) {
                    throw new SQLException("Expected one sensor data row to be inserted, got " + affectedRows);
                }
            }

            // 回写 smoke_device 状态，打通“硬件 MQTT → 后端在线 → 前端实时显示”
            updateDeviceStatus(connection, message, deviceId);
        }
    }

    private void updateDeviceStatus(Connection connection, SensorDataMessage message, String deviceId)
            throws SQLException {
        if (message.getType() == SensorDataType.SMOKE) {
            int ppm = (int) Math.round(message.getValue());
            String alarmStatus = ppm >= WARNING_THRESHOLD ? ALARM_STATUS_ALARM : ALARM_STATUS_SAFE;
            try (PreparedStatement statement = connection.prepareStatement(UPDATE_DEVICE_ON_SMOKE_SQL)) {
                statement.setInt(1, ppm);
                statement.setString(2, message.getRiskLevel());
                statement.setString(3, alarmStatus);
                statement.setString(4, deviceId);
                statement.executeUpdate();
            }
        } else {
            try (PreparedStatement statement = connection.prepareStatement(UPDATE_DEVICE_HEARTBEAT_SQL)) {
                statement.setString(1, deviceId);
                statement.executeUpdate();
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

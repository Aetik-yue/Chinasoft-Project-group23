package com.chinasoft.getdata.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.chinasoft.getdata.config.DatabaseProperties;
import com.chinasoft.getdata.config.MqttProperties;
import com.chinasoft.getdata.mqtt.MqttSmokeDataSubscriber;
import com.chinasoft.getdata.repository.JdbcSmokeDataRepository;
import com.chinasoft.getdata.service.SmokeDataMessageHandler;
import com.chinasoft.getdata.service.SmokeMessageParser;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.Test;

/**
 * 远端联调测试。普通 mvn test 不会自动运行 *IT；必须通过 -Dtest 显式执行。
 */
public class RemoteMqttIntegrationIT {

    private static final String[] NON_TARGET_TABLES = {
            "alarm_record", "alarm_timeline", "device_control", "smoke_device",
            "sys_user", "system_setting", "vision_check"
    };

    @Test
    public void mqttPpmOnlyWritesSmokeDataWithServerMetadata() throws Exception {
        String mqttHost = env("MQTT_HOST_URL", "tcp://localhost:1883");
        String mqttTopic = env("MQTT_DATA_TOPIC", "group23");
        String mysqlUrl = env("MYSQL_URL",
                "jdbc:mysql://localhost:3306/dream28?useUnicode=true&characterEncoding=utf8&useSSL=false");
        String mysqlUsername = env("MYSQL_USERNAME", "root");
        String mysqlPassword = env("MYSQL_PASSWORD", "");

        DatabaseProperties database = new DatabaseProperties();
        database.setUrl(mysqlUrl);
        database.setUsername(mysqlUsername);
        database.setPassword(mysqlPassword);

        MqttProperties mqtt = new MqttProperties();
        mqtt.setHostUrl(mqttHost);
        mqtt.setClientId("smoke-data-it-subscriber");
        mqtt.setTopic(mqttTopic);
        mqtt.setQos(1);
        mqtt.setCleanSession(true);
        mqtt.setAutomaticReconnect(true);
        mqtt.setConnectionTimeout(10);
        mqtt.setKeepAlive(30);

        SmokeDataMessageHandler handler = new SmokeDataMessageHandler(
                new SmokeMessageParser(), new JdbcSmokeDataRepository(database));
        MqttSmokeDataSubscriber subscriber = new MqttSmokeDataSubscriber(mqtt, handler);

        Map<String, Long> countsBefore = readCounts(
                mysqlUrl, mysqlUsername, mysqlPassword, NON_TARGET_TABLES);
        long maxSmokeDataId = readMaxSmokeDataId(mysqlUrl, mysqlUsername, mysqlPassword);
        LocalDateTime databaseTimeBefore = readDatabaseTime(mysqlUrl, mysqlUsername, mysqlPassword);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String payload = "{\"ppm\":86.5}";

        MqttClient publisher = null;
        try {
            subscriber.afterPropertiesSet();
            Thread.sleep(500L);

            publisher = new MqttClient(
                    mqttHost, "smoke-data-it-publisher-" + suffix, new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            publisher.connect(options);

            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(1);
            message.setRetained(false);
            publisher.publish(mqttTopic, message);

            assertTrue("Timed out waiting for smoke_data insert",
                    waitForInsertedRow(mysqlUrl, mysqlUsername, mysqlPassword,
                            maxSmokeDataId, databaseTimeBefore));

            Map<String, Long> countsAfter = readCounts(
                    mysqlUrl, mysqlUsername, mysqlPassword, NON_TARGET_TABLES);
            assertEquals("The seven non-target tables must remain unchanged", countsBefore, countsAfter);
        } finally {
            if (publisher != null) {
                if (publisher.isConnected()) {
                    publisher.disconnect();
                }
                publisher.close();
            }
            subscriber.destroy();
        }
    }

    private boolean waitForInsertedRow(String url, String username, String password,
                                       long previousMaxId, LocalDateTime databaseTimeBefore) throws Exception {
        String sql = "SELECT COUNT(*) FROM smoke_data "
                + "WHERE id > ? AND device_id = 'SMK-001' "
                + "AND ABS(smoke_value - 86.5) < 0.001 AND risk_level = 'normal' "
                + "AND record_time >= ? AND source = 'sensor'";
        for (int attempt = 0; attempt < 40; attempt++) {
            try (Connection connection = DriverManager.getConnection(url, username, password);
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, previousMaxId);
                statement.setTimestamp(2, Timestamp.valueOf(databaseTimeBefore));
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    if (resultSet.getLong(1) == 1L) {
                        return true;
                    }
                }
            }
            Thread.sleep(500L);
        }
        return false;
    }

    private long readMaxSmokeDataId(String url, String username, String password) throws Exception {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COALESCE(MAX(id), 0) FROM smoke_data")) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private LocalDateTime readDatabaseTime(String url, String username, String password) throws Exception {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT NOW()")) {
            resultSet.next();
            return resultSet.getTimestamp(1).toLocalDateTime();
        }
    }

    private Map<String, Long> readCounts(String url, String username, String password,
                                         String[] tables) throws Exception {
        Map<String, Long> counts = new LinkedHashMap<String, Long>();
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            for (String table : tables) {
                try (Statement statement = connection.createStatement();
                     ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    resultSet.next();
                    counts.put(table, resultSet.getLong(1));
                }
            }
        }
        return counts;
    }

    private String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }
}

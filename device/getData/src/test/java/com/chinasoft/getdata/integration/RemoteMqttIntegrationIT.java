package com.chinasoft.getdata.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.chinasoft.getdata.config.DatabaseProperties;
import com.chinasoft.getdata.config.MqttProperties;
import com.chinasoft.getdata.mqtt.MqttSensorDataSubscriber;
import com.chinasoft.getdata.repository.JdbcSensorDataRepository;
import com.chinasoft.getdata.service.SensorDataMessageHandler;
import com.chinasoft.getdata.service.SensorMessageParser;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.Test;

/**
 * 远端联调测试。普通 mvn test 不会自动执行 *IT，必须通过 -Dtest 显式执行。
 */
public class RemoteMqttIntegrationIT {

    private static final String[] NON_TARGET_TABLES = {
            "alarm_record", "alarm_timeline", "device_control", "smoke_device",
            "sys_user", "system_setting", "vision_check"
    };

    @Test
    public void mqttWritesEachSensorMessageOnlyToItsTargetTable() throws Exception {
        String mqttHost = env("MQTT_HOST_URL", "tcp://47.108.58.107:1883");
        String mqttTopic = env("MQTT_DATA_TOPIC", "group23");
        String mysqlUrl = env("MYSQL_URL",
                "jdbc:mysql://47.108.58.107:3306/dream28?useUnicode=true&characterEncoding=utf8&useSSL=false");
        String mysqlUsername = env("MYSQL_USERNAME", "root");
        String mysqlPassword = env("MYSQL_PASSWORD", "c0765083cd3f57ab");

        DatabaseProperties database = new DatabaseProperties();
        database.setUrl(mysqlUrl);
        database.setUsername(mysqlUsername);
        database.setPassword(mysqlPassword);

        MqttProperties mqtt = new MqttProperties();
        mqtt.setHostUrl(mqttHost);
        mqtt.setClientId("sensor-data-it-subscriber");
        mqtt.setTopic(mqttTopic);
        mqtt.setQos(1);
        mqtt.setCleanSession(true);
        mqtt.setAutomaticReconnect(true);
        mqtt.setConnectionTimeout(10);
        mqtt.setKeepAlive(30);

        SensorDataMessageHandler handler = new SensorDataMessageHandler(
                new SensorMessageParser(), new JdbcSensorDataRepository(database));
        MqttSensorDataSubscriber subscriber = new MqttSensorDataSubscriber(mqtt, handler);

        Map<String, Long> countsBefore = readCounts(
                mysqlUrl, mysqlUsername, mysqlPassword, NON_TARGET_TABLES);
        long smokeId = readMaxId(mysqlUrl, mysqlUsername, mysqlPassword, "smoke_data");
        long temperatureId = readMaxId(mysqlUrl, mysqlUsername, mysqlPassword, "temperature_data");
        long humidityId = readMaxId(mysqlUrl, mysqlUsername, mysqlPassword, "humidity_data");

        MqttClient publisher = null;
        try {
            subscriber.afterPropertiesSet();
            Thread.sleep(500L);

            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            publisher = new MqttClient(
                    mqttHost, "sensor-data-it-publisher-" + suffix, new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            publisher.connect(options);

            publish(publisher, mqttTopic, "{\"ppm\":86.5}");
            publish(publisher, mqttTopic, "{\"℃\":25.3}");
            publish(publisher, mqttTopic, "{\"%RH\":50.7}");

            assertTrue("Timed out waiting for smoke_data insert",
                    waitForValue(mysqlUrl, mysqlUsername, mysqlPassword,
                            "smoke_data", "smoke_value", smokeId, 86.5, "sensor"));
            assertTrue("Timed out waiting for temperature_data insert",
                    waitForValue(mysqlUrl, mysqlUsername, mysqlPassword,
                            "temperature_data", "temperature_value", temperatureId, 25.3, "simulate"));
            assertTrue("Timed out waiting for humidity_data insert",
                    waitForValue(mysqlUrl, mysqlUsername, mysqlPassword,
                            "humidity_data", "humidity_value", humidityId, 50.7, "simulate"));

            assertEquals(countsBefore,
                    readCounts(mysqlUrl, mysqlUsername, mysqlPassword, NON_TARGET_TABLES));
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

    private void publish(MqttClient publisher, String topic, String payload) throws Exception {
        MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
        message.setQos(1);
        message.setRetained(false);
        publisher.publish(topic, message);
    }

    private boolean waitForValue(String url, String username, String password,
                                 String table, String valueColumn, long previousMaxId,
                                 double expectedValue, String source) throws Exception {
        String sql = "SELECT COUNT(*) FROM " + table
                + " WHERE id > ? AND device_id = 'SMK-001' AND ABS(" + valueColumn
                + " - ?) < 0.001 AND source = ?";
        for (int attempt = 0; attempt < 40; attempt++) {
            try (Connection connection = DriverManager.getConnection(url, username, password);
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, previousMaxId);
                statement.setDouble(2, expectedValue);
                statement.setString(3, source);
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

    private long readMaxId(String url, String username, String password, String table) throws Exception {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COALESCE(MAX(id), 0) FROM " + table)) {
            resultSet.next();
            return resultSet.getLong(1);
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

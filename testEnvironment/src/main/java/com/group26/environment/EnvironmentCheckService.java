package com.group26.environment;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class EnvironmentCheckService {

    static final String DETECTOR_CLASS = "cn.smartjavaai.objectdetection.model.DetectorModel";
    static final String ONNX_ENVIRONMENT_CLASS = "ai.onnxruntime.OrtEnvironment";

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final String mysqlPassword;
    private final String expectedDatabase;
    private final String mqttBroker;
    private final String maxKbUrl;
    private final String dataEaseUrl;
    private final int timeoutSeconds;
    private final HttpClient httpClient;

    public EnvironmentCheckService(
            JdbcTemplate jdbcTemplate,
            StringRedisTemplate redisTemplate,
            @Value("${spring.datasource.password:}") String mysqlPassword,
            @Value("${environment-check.expected-database}") String expectedDatabase,
            @Value("${environment-check.mqtt-broker}") String mqttBroker,
            @Value("${environment-check.maxkb-url}") String maxKbUrl,
            @Value("${environment-check.dataease-url}") String dataEaseUrl,
            @Value("${environment-check.timeout-seconds:10}") int timeoutSeconds) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
        this.mysqlPassword = mysqlPassword;
        this.expectedDatabase = expectedDatabase;
        this.mqttBroker = mqttBroker;
        this.maxKbUrl = maxKbUrl;
        this.dataEaseUrl = dataEaseUrl;
        this.timeoutSeconds = timeoutSeconds;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    public EnvironmentReport checkAll() {
        List<CheckResult> checks = new ArrayList<>();
        checks.add(runCheck("java-spring", this::checkJavaAndSpring));
        checks.add(runCheck("mysql", this::checkMySql));
        checks.add(runCheck("redis", this::checkRedis));
        checks.add(runCheck("mqtt", this::checkMqtt));
        checks.add(runCheck("maxkb", () -> checkHttp(maxKbUrl)));
        checks.add(runCheck("dataease", () -> checkHttp(dataEaseUrl)));
        checks.add(runCheck("smartjavaai", () -> checkSmartJavaAi(DETECTOR_CLASS, ONNX_ENVIRONMENT_CLASS)));
        return EnvironmentReport.from(checks);
    }

    String checkJavaAndSpring() {
        return "Java " + Runtime.version().feature()
                + ", Spring Boot " + org.springframework.boot.SpringBootVersion.getVersion();
    }

    String checkMySql() {
        if (mysqlPassword == null || mysqlPassword.isBlank()) {
            throw new IllegalStateException("MYSQL_PASSWORD is not set");
        }
        Map<String, Object> result = jdbcTemplate.queryForMap(
                "SELECT DATABASE() AS current_database, VERSION() AS database_version");
        String database = String.valueOf(result.get("current_database"));
        if (!expectedDatabase.equals(database)) {
            throw new IllegalStateException("Connected to unexpected database: " + database);
        }
        return "database=" + database + ", version=" + result.get("database_version");
    }

    String checkRedis() {
        String key = "environment-check:" + UUID.randomUUID();
        String value = "redis-ok";
        try {
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(30));
            String actual = redisTemplate.opsForValue().get(key);
            if (!value.equals(actual)) {
                throw new IllegalStateException("Redis read-back value did not match");
            }
            return "temporary key write/read/delete succeeded";
        } finally {
            redisTemplate.delete(key);
        }
    }

    String checkMqtt() throws Exception {
        String id = UUID.randomUUID().toString();
        String topic = "smart-smoke/group26/environment-check/" + id;
        String payload = "mqtt-ok-" + id;
        CountDownLatch received = new CountDownLatch(1);
        AtomicReference<String> receivedPayload = new AtomicReference<>();

        MqttClient client = new MqttClient(mqttBroker, "env-check-" + id, new MemoryPersistence());
        try {
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    // The result is reported by the surrounding check.
                }

                @Override
                public void messageArrived(String incomingTopic, MqttMessage message) {
                    if (topic.equals(incomingTopic)) {
                        receivedPayload.set(new String(message.getPayload(), StandardCharsets.UTF_8));
                        received.countDown();
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // No action required.
                }
            });

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(timeoutSeconds);
            options.setKeepAliveInterval(20);
            client.connect(options);
            client.subscribe(topic, 1);
            client.publish(topic, payload.getBytes(StandardCharsets.UTF_8), 1, false);

            if (!received.await(timeoutSeconds, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for MQTT message");
            }
            if (!payload.equals(receivedPayload.get())) {
                throw new IllegalStateException("MQTT payload did not match");
            }
            return "QoS 1 publish/subscribe succeeded on temporary topic";
        } finally {
            if (client.isConnected()) {
                client.disconnect(2000);
            }
            client.close();
        }
    }

    String checkHttp(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .GET()
                .build();
        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() < 200 || response.statusCode() >= 400) {
            throw new IllegalStateException("HTTP status " + response.statusCode());
        }
        return "HTTP " + response.statusCode() + " from " + URI.create(url).getAuthority();
    }

    String checkSmartJavaAi(String detectorClassName, String runtimeClassName) throws Exception {
        Class<?> detectorClass = Class.forName(detectorClassName);
        Class<?> runtimeClass = Class.forName(runtimeClassName);
        Method getEnvironment = runtimeClass.getMethod("getEnvironment");
        Object environment = getEnvironment.invoke(null);
        if (environment == null) {
            throw new IllegalStateException("ONNX Runtime environment was null");
        }
        Method getVersion = runtimeClass.getMethod("getVersion");
        Object version = getVersion.invoke(environment);
        return "loaded " + detectorClass.getSimpleName() + ", ONNX Runtime " + version;
    }

    private CheckResult runCheck(String name, CheckedSupplier supplier) {
        long started = System.nanoTime();
        try {
            String message = supplier.get();
            return new CheckResult(name, "UP", sanitize(message, mysqlPassword), elapsedMs(started));
        } catch (Exception | LinkageError error) {
            return new CheckResult(name, "DOWN", sanitize(rootMessage(error), mysqlPassword), elapsedMs(started));
        }
    }

    static String sanitize(String message, String secret) {
        String safe = message == null ? "Unknown error" : message.replaceAll("[\\r\\n]+", " ");
        if (secret != null && !secret.isBlank()) {
            safe = safe.replace(secret, "***");
        }
        return safe;
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return current.getClass().getSimpleName() + (message == null ? "" : ": " + message);
    }

    private static long elapsedMs(long started) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
    }

    @FunctionalInterface
    private interface CheckedSupplier {
        String get() throws Exception;
    }
}

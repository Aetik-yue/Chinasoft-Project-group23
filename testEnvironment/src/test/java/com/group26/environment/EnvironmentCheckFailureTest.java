package com.group26.environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

class EnvironmentCheckFailureTest {

    @Test
    void missingMysqlPasswordHasAnExplicitSafeError() {
        EnvironmentCheckService service = serviceWith("", "tcp://127.0.0.1:1", 1);

        assertThatThrownBy(service::checkMySql)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("MYSQL_PASSWORD is not set");
    }

    @Test
    void unreachableMqttBrokerTimesOutWithoutHidingTheCause() {
        EnvironmentCheckService service = serviceWith("not-used", "tcp://127.0.0.1:1", 1);

        assertThatThrownBy(service::checkMqtt).isInstanceOf(Exception.class);
    }

    @Test
    void missingSmartJavaAiClassIsReported() {
        EnvironmentCheckService service = serviceWith("not-used", "tcp://127.0.0.1:1", 1);

        assertThatThrownBy(() -> service.checkSmartJavaAi("missing.Detector", "missing.Runtime"))
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void secretsAreRemovedFromMessages() {
        assertThat(EnvironmentCheckService.sanitize("failure: secret-value", "secret-value"))
                .isEqualTo("failure: ***");
    }

    private EnvironmentCheckService serviceWith(String password, String mqttBroker, int timeoutSeconds) {
        return new EnvironmentCheckService(
                new JdbcTemplate(),
                new StringRedisTemplate(),
                password,
                "dream26",
                mqttBroker,
                "http://127.0.0.1:1",
                "http://127.0.0.1:1",
                timeoutSeconds);
    }
}

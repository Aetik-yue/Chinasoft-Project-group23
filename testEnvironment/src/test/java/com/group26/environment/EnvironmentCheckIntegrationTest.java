package com.group26.environment;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EnvironmentCheckIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void allConfiguredTechnologiesAreAvailableThroughTheHttpEndpoint() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/environment/check"))
                .timeout(Duration.ofSeconds(60))
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
        EnvironmentReport report = objectMapper.readValue(response.body(), EnvironmentReport.class);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(report.checks()).extracting(CheckResult::name)
                .containsExactly("java-spring", "mysql", "redis", "mqtt", "maxkb", "dataease", "smartjavaai");
        assertThat(report.overall())
                .withFailMessage("Environment report contained failures: %s", report.checks())
                .isEqualTo("UP");
    }
}

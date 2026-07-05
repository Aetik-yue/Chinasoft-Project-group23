package com.chinasoft.smokesensor.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

class SensorDataRepositoryQueryTest {

    @Test
    void latestQueriesShouldOnlyAcceptRealDataAndOrderByCreatedAt() throws NoSuchMethodException {
        String byDeviceQuery = queryOf("findLatestRealDataByDeviceId", String.class);
        String globalQuery = queryOf("findLatestRealData");

        assertRealDataFilter(byDeviceQuery);
        assertRealDataFilter(globalQuery);
        assertThat(byDeviceQuery).contains("order by created_at desc");
        assertThat(globalQuery).contains("order by created_at desc");
    }

    @Test
    void onlineCountShouldUseCreatedAtAndTheSameRealDataFilter() throws NoSuchMethodException {
        String countQuery = queryOf("countOnlineDevices", LocalDateTime.class);

        assertRealDataFilter(countQuery);
        assertThat(countQuery).contains("sd.created_at >= :thresholdtime");
        assertThat(countQuery).contains("inner join smoke_device");
    }

    private String queryOf(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        return SensorDataRepository.class.getMethod(methodName, parameterTypes)
                .getAnnotation(Query.class)
                .value()
                .toLowerCase()
                .replaceAll("\\s+", " ");
    }

    private void assertRealDataFilter(String query) {
        assertThat(query)
                .contains("source = 'sensor'")
                .contains("source is null")
                .doesNotContain("source = 'simulate'")
                .doesNotContain("source = 'mqtt_test'");
    }
}

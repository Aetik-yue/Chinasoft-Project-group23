package com.chinasoft.smokesensor;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SmokeSensorApplicationTest {
    private final TimeZone originalTimeZone = TimeZone.getDefault();

    @AfterEach
    void restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test
    void configuresShanghaiAsTheBusinessTimeZoneBeforeSpringStarts() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        SmokeSensorApplication.configureDefaultTimeZone();

        assertThat(ZoneId.systemDefault()).isEqualTo(ZoneId.of("Asia/Shanghai"));
    }
}

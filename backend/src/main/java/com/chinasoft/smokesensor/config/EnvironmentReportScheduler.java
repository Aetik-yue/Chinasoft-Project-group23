package com.chinasoft.smokesensor.config;

import com.chinasoft.smokesensor.service.EnvironmentReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 环境小时报表定时任务：每小时第 5 分钟聚合上一个完整小时的环境采样。
 *
 * <p>可通过 `app.environment-report.schedule-enabled=false` 关闭（例如单元测试时）。
 */
@Slf4j
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.environment-report.schedule-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class EnvironmentReportScheduler {

    private final EnvironmentReportService environmentReportService;

    /** 每小时第 5 分钟执行，聚合上一个完整小时。 */
    @Scheduled(cron = "0 5 * * * *")
    public void aggregatePreviousHour() {
        try {
            environmentReportService.aggregatePreviousHourForAll();
        } catch (Exception e) {
            log.error("[report-hourly] 定时聚合失败：{}", e.getMessage(), e);
        }
    }
}

package com.chinasoft.smokesensor;

import java.time.ZoneId;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SmokeSensorApplication {
    /**
     * 宠物档案业务以北京时间记录 LocalDateTime。
     *
     * <p>必须在 Spring 上下文创建前设置：Bean Validation 的 @PastOrPresent、
     * LocalDateTime.now() 与 JPA 都会读取 JVM 默认时区。这样可以避免浏览器
     * 发送的北京时间被 UTC 服务器误判为未来时间。</p>
     */
    static final ZoneId BUSINESS_TIME_ZONE = ZoneId.of("Asia/Shanghai");

    static void configureDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(BUSINESS_TIME_ZONE));
    }

    public static void main(String[] args) {
        configureDefaultTimeZone();
        SpringApplication.run(SmokeSensorApplication.class, args);
    }
}

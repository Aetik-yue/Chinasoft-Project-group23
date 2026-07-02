package com.chinasoft.postdata;

import com.chinasoft.postdata.config.MqttProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(MqttProperties.class)
public class PostDataApplication {

    public static void main(String[] args) {
        SpringApplication.run(PostDataApplication.class, args);
    }
}

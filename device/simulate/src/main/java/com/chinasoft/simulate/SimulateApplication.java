package com.chinasoft.simulate;

import com.chinasoft.simulate.config.MqttProperties;
import com.chinasoft.simulate.config.SimulationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties({MqttProperties.class, SimulationProperties.class})
public class SimulateApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimulateApplication.class, args);
    }
}

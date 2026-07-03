package com.chinasoft.simulate.mqtt;

import com.chinasoft.simulate.config.MqttProperties;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class PahoSimulationPublisher
        implements SimulationPublisher, InitializingBean, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(PahoSimulationPublisher.class);

    private final MqttProperties properties;
    private MqttClient client;
    private MqttConnectOptions connectOptions;

    public PahoSimulationPublisher(MqttProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        client = new MqttClient(properties.getHostUrl(),
                properties.getClientId() + "-" + suffix, new MemoryPersistence());
        connectOptions = new MqttConnectOptions();
        connectOptions.setCleanSession(properties.isCleanSession());
        connectOptions.setAutomaticReconnect(properties.isAutomaticReconnect());
        connectOptions.setConnectionTimeout(properties.getConnectionTimeout());
        connectOptions.setKeepAliveInterval(properties.getKeepAlive());
        if (properties.getUsername() != null && !properties.getUsername().trim().isEmpty()) {
            connectOptions.setUserName(properties.getUsername());
        }
        if (properties.getPassword() != null && !properties.getPassword().isEmpty()) {
            connectOptions.setPassword(properties.getPassword().toCharArray());
        }
    }

    @Override
    public synchronized void publish(String payload) throws Exception {
        ensureConnected();
        client.publish(properties.getTopic(), createMessage(payload));
        logger.info("模拟数据已发送: topic={}, payload={}", properties.getTopic(), payload);
    }

    MqttMessage createMessage(String payload) {
        MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
        message.setQos(properties.getQos());
        message.setRetained(properties.isRetained());
        return message;
    }

    private void ensureConnected() throws Exception {
        if (!client.isConnected()) {
            logger.info("正在连接 MQTT Broker: {}", properties.getHostUrl());
            client.connect(connectOptions);
            logger.info("MQTT 连接成功，模拟数据主题: {}", properties.getTopic());
        }
    }

    @Override
    public void destroy() {
        if (client == null) {
            return;
        }
        try {
            if (client.isConnected()) {
                client.disconnect();
            }
            client.close();
        } catch (Exception exception) {
            logger.warn("关闭 MQTT 模拟客户端时发生异常", exception);
        }
    }
}

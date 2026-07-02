package com.chinasoft.postdata.mqtt;

import com.chinasoft.postdata.config.MqttProperties;
import com.chinasoft.postdata.model.ControlSignal;
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
public class PahoControlSignalPublisher implements ControlSignalPublisher, InitializingBean, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(PahoControlSignalPublisher.class);

    private final MqttProperties properties;
    private MqttClient client;
    private MqttConnectOptions connectOptions;

    public PahoControlSignalPublisher(MqttProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        String clientId = properties.getClientId() + "-" + UUID.randomUUID().toString().substring(0, 8);
        client = new MqttClient(properties.getHostUrl(), clientId, new MemoryPersistence());
        connectOptions = new MqttConnectOptions();
        connectOptions.setAutomaticReconnect(properties.isAutomaticReconnect());
        connectOptions.setCleanSession(properties.isCleanSession());
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
    public synchronized void publish(ControlSignal signal) throws Exception {
        ensureConnected();
        MqttMessage message = new MqttMessage(signal.toJson().getBytes(StandardCharsets.UTF_8));
        message.setQos(properties.getQos());
        message.setRetained(properties.isRetained());
        client.publish(properties.getTopic(), message);
        logger.info("控制信号已发送: topic={}, payload={}", properties.getTopic(), signal.toJson());
    }

    private void ensureConnected() throws Exception {
        if (!client.isConnected()) {
            logger.info("正在连接 MQTT Broker: {}", properties.getHostUrl());
            client.connect(connectOptions);
            logger.info("MQTT 连接成功，控制主题: {}", properties.getTopic());
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
            logger.warn("关闭 MQTT 客户端时发生异常", exception);
        }
    }
}

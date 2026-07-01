package com.chinasoft.getdata.mqtt;

import com.chinasoft.getdata.config.MqttProperties;
import com.chinasoft.getdata.service.SmokeDataMessageHandler;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class MqttSmokeDataSubscriber
        implements MqttCallbackExtended, InitializingBean, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(MqttSmokeDataSubscriber.class);

    private final MqttProperties properties;
    private final SmokeDataMessageHandler handler;
    private MqttClient client;

    public MqttSmokeDataSubscriber(MqttProperties properties, SmokeDataMessageHandler handler) {
        this.properties = properties;
        this.handler = handler;
    }

    @Override
    public void afterPropertiesSet() throws MqttException {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String clientId = properties.getClientId() + "-" + suffix;
        client = new MqttClient(properties.getHostUrl(), clientId, new MemoryPersistence());
        client.setCallback(this);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(properties.isCleanSession());
        options.setAutomaticReconnect(properties.isAutomaticReconnect());
        options.setConnectionTimeout(properties.getConnectionTimeout());
        options.setKeepAliveInterval(properties.getKeepAlive());
        if (properties.getUsername() != null && !properties.getUsername().trim().isEmpty()) {
            options.setUserName(properties.getUsername());
        }
        if (properties.getPassword() != null && !properties.getPassword().isEmpty()) {
            options.setPassword(properties.getPassword().toCharArray());
        }

        logger.info("正在连接 MQTT Broker: {}", properties.getHostUrl());
        client.connect(options);
    }

    @Override
    public void connectComplete(boolean reconnect, String serverUri) {
        try {
            client.subscribe(properties.getTopic(), properties.getQos());
            logger.info("MQTT{}成功，已订阅主题 {}，QoS={}",
                    reconnect ? "重连" : "连接", properties.getTopic(), properties.getQos());
        } catch (MqttException exception) {
            logger.error("订阅 MQTT 主题失败: " + properties.getTopic(), exception);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        logger.warn("MQTT 连接中断，等待自动重连", cause);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        logger.info("收到 MQTT 消息: topic={}, qos={}", topic, message.getQos());
        handler.handle(payload);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // 此服务只负责订阅设备数据。
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
        } catch (MqttException exception) {
            logger.warn("关闭 MQTT 客户端时发生异常", exception);
        }
    }
}

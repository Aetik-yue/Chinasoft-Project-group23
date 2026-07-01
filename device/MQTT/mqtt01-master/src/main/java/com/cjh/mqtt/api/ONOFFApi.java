package com.cjh.mqtt.api;


import com.cjh.mqtt.util.MqttSendClient;
import com.cjh.mqtt.util.MqttProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@CrossOrigin
@RestController
public class ONOFFApi {

    @Autowired
    private MqttSendClient MqttSendClient;

    @Autowired
    private MqttProperties mqttProperties;

    @PostMapping("/on")
    public Map on(){
        MqttSendClient.publish(false, mqttProperties.getControlTopic(), "1");
        return  null;

    }

    @PostMapping("/off")
    public Map off(){
        MqttSendClient.publish(false, mqttProperties.getControlTopic(), "0");
        return  null;

    }
}

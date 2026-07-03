package com.chinasoft.simulate.mqtt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.chinasoft.simulate.config.MqttProperties;
import java.nio.charset.StandardCharsets;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.Test;

public class PahoSimulationPublisherTest {

    @Test
    public void createsQosOneNonRetainedUtf8Message() {
        MqttProperties properties = new MqttProperties();
        properties.setQos(1);
        properties.setRetained(false);
        PahoSimulationPublisher publisher = new PahoSimulationPublisher(properties);

        MqttMessage message = publisher.createMessage("{\"℃\":25.0}");

        assertEquals(1, message.getQos());
        assertFalse(message.isRetained());
        assertEquals("{\"℃\":25.0}", new String(message.getPayload(), StandardCharsets.UTF_8));
    }
}

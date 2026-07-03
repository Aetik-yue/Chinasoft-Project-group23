package com.chinasoft.simulate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mqtt")
public class MqttProperties {

    private String hostUrl;
    private String username;
    private String password;
    private String clientId;
    private String topic;
    private int qos = 1;
    private boolean retained;
    private boolean cleanSession = true;
    private boolean automaticReconnect = true;
    private int connectionTimeout = 10;
    private int keepAlive = 30;

    public String getHostUrl() { return hostUrl; }
    public void setHostUrl(String hostUrl) { this.hostUrl = hostUrl; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public int getQos() { return qos; }
    public void setQos(int qos) { this.qos = qos; }
    public boolean isRetained() { return retained; }
    public void setRetained(boolean retained) { this.retained = retained; }
    public boolean isCleanSession() { return cleanSession; }
    public void setCleanSession(boolean cleanSession) { this.cleanSession = cleanSession; }
    public boolean isAutomaticReconnect() { return automaticReconnect; }
    public void setAutomaticReconnect(boolean automaticReconnect) { this.automaticReconnect = automaticReconnect; }
    public int getConnectionTimeout() { return connectionTimeout; }
    public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }
    public int getKeepAlive() { return keepAlive; }
    public void setKeepAlive(int keepAlive) { this.keepAlive = keepAlive; }
}

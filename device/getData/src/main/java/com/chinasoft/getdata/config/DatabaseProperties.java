package com.chinasoft.getdata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "database")
public class DatabaseProperties {

    private String url;
    private String username;
    private String password;

    /**
     * 当前设备端服务代表的 device_id，用于回写 smoke_device 心跳与实时状态。
     * 默认 device-001，与前端 mockDashboard 中鹦鹉绑定的守护设备一致。
     */
    private String deviceId = "device-001";

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}

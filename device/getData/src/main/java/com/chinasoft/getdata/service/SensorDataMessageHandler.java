package com.chinasoft.getdata.service;

import com.chinasoft.getdata.model.SensorDataMessage;
import com.chinasoft.getdata.repository.SensorDataWriter;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SensorDataMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(SensorDataMessageHandler.class);

    private final SensorMessageParser parser;
    private final SensorDataWriter writer;

    public SensorDataMessageHandler(SensorMessageParser parser, SensorDataWriter writer) {
        this.parser = parser;
        this.writer = writer;
    }

    public boolean handle(String payload) {
        try {
            SensorDataMessage message = parser.parse(payload);
            writer.save(message);
            logger.info("传感数据已写入: deviceId=SMK-001, type={}, value={}",
                    message.getType(), message.getValue());
            return true;
        } catch (IllegalArgumentException exception) {
            logger.warn("忽略不合法的传感 MQTT 消息: {}", exception.getMessage());
            return false;
        } catch (SQLException exception) {
            logger.error("写入传感数据失败", exception);
            return false;
        }
    }
}

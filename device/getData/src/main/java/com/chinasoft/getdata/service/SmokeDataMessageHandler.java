package com.chinasoft.getdata.service;

import com.chinasoft.getdata.model.SmokeDataMessage;
import com.chinasoft.getdata.repository.SmokeDataWriter;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SmokeDataMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(SmokeDataMessageHandler.class);

    private final SmokeMessageParser parser;
    private final SmokeDataWriter writer;

    public SmokeDataMessageHandler(SmokeMessageParser parser, SmokeDataWriter writer) {
        this.parser = parser;
        this.writer = writer;
    }

    public boolean handle(String payload) {
        try {
            SmokeDataMessage message = parser.parse(payload);
            writer.save(message);
            logger.info("烟感数据已写入 smoke_data: deviceId=SMK-001, ppm={}, riskLevel={}, source=sensor",
                    message.getPpm(), message.getRiskLevel());
            return true;
        } catch (IllegalArgumentException exception) {
            logger.warn("忽略不合法的烟感 MQTT 消息: {}", exception.getMessage());
            return false;
        } catch (SQLException exception) {
            logger.error("写入 smoke_data 失败", exception);
            return false;
        }
    }
}

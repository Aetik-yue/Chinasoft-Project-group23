package com.chinasoft.getdata.repository;

import com.chinasoft.getdata.model.SensorDataMessage;
import java.sql.SQLException;

public interface SensorDataWriter {

    void save(SensorDataMessage message) throws SQLException;
}

package com.chinasoft.getdata.repository;

import com.chinasoft.getdata.model.SmokeDataMessage;
import java.sql.SQLException;

public interface SmokeDataWriter {

    void save(SmokeDataMessage message) throws SQLException;
}

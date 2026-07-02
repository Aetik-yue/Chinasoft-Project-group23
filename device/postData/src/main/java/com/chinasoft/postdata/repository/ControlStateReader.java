package com.chinasoft.postdata.repository;

import java.util.Map;

public interface ControlStateReader {

    Map<String, String> readStates(String deviceId);
}

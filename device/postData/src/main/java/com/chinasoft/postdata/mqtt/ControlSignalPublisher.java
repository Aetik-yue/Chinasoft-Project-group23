package com.chinasoft.postdata.mqtt;

import com.chinasoft.postdata.model.ControlSignal;

public interface ControlSignalPublisher {

    void publish(ControlSignal signal) throws Exception;
}

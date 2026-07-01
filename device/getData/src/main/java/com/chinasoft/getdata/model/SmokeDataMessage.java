package com.chinasoft.getdata.model;

public class SmokeDataMessage {

    private final double ppm;
    private final String riskLevel;

    public SmokeDataMessage(double ppm, String riskLevel) {
        this.ppm = ppm;
        this.riskLevel = riskLevel;
    }

    public double getPpm() {
        return ppm;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

}

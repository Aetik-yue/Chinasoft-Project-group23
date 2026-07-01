package com.chinasoft.getdata.service;

public final class RiskLevelCalculator {

    private RiskLevelCalculator() {
    }

    public static String calculate(double ppm) {
        if (ppm < 100) {
            return "normal";
        }
        if (ppm < 200) {
            return "low";
        }
        if (ppm < 400) {
            return "medium";
        }
        return "high";
    }
}

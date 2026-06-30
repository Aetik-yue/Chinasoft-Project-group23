package com.group26.environment;

public record CheckResult(String name, String status, String message, long durationMs) {

    public boolean isUp() {
        return "UP".equals(status);
    }
}

package com.group26.environment;

import java.time.Instant;
import java.util.List;

public record EnvironmentReport(
        String overall,
        Instant checkedAt,
        int passed,
        int failed,
        List<CheckResult> checks) {

    public static EnvironmentReport from(List<CheckResult> checks) {
        int passed = (int) checks.stream().filter(CheckResult::isUp).count();
        return new EnvironmentReport(
                passed == checks.size() ? "UP" : "DOWN",
                Instant.now(),
                passed,
                checks.size() - passed,
                List.copyOf(checks));
    }
}

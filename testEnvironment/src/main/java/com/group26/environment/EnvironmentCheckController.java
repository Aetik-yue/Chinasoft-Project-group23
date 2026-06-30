package com.group26.environment;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/environment")
public class EnvironmentCheckController {

    private final EnvironmentCheckService checkService;

    public EnvironmentCheckController(EnvironmentCheckService checkService) {
        this.checkService = checkService;
    }

    @GetMapping("/check")
    public EnvironmentReport check() {
        return checkService.checkAll();
    }
}

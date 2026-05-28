package org.example.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class FrontendConfigController {

    private final boolean aiBotEnabled;

    public FrontendConfigController(@Value("${bot.ai.enabled:false}") boolean aiBotEnabled) {
        this.aiBotEnabled = aiBotEnabled;
    }

    @GetMapping("/api/config")
    public Map<String, Object> getConfig() {
        return Map.of("aiBotEnabled", aiBotEnabled);
    }
}

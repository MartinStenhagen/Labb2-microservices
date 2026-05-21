package org.example.botservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bot.ai")
public record BotAiProperties(
        boolean enabled,
        String baseUrl,
        String apiKey,
        String model,
        double temperature
) {
}

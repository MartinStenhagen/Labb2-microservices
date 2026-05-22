package org.example.botservice.service;

import org.example.botservice.config.BotAiProperties;
import org.example.event.MessagePublishedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;

public class OpenRouterBotResponseGenerator implements BotResponseGenerator {
    private static final Logger logger = LoggerFactory.getLogger(OpenRouterBotResponseGenerator.class);

    private static final String SYSTEM_PROMPT = """
            Du är en vänlig och naturlig svensk chattbot.
            Svara som en vanlig chattkompis: hälsa tillbaka, svara enkelt på småprat och ställ gärna en kort följdfråga.
            Håll svaren ganska korta och be aldrig om lösenord, API-nycklar eller annan känslig information.
            """;

    private final RestClient restClient;
    private final BotAiProperties botAiProperties;
    private final RuleBasedBotReplyGenerator fallbackGenerator;

    public OpenRouterBotResponseGenerator(
            RestClient.Builder restClientBuilder,
            BotAiProperties botAiProperties,
            RuleBasedBotReplyGenerator fallbackGenerator
    ) {
        if (!StringUtils.hasText(botAiProperties.apiKey())) {
            throw new IllegalStateException("bot.ai.api-key must be configured when bot.ai.enabled=true");
        }

        this.restClient = restClientBuilder
                .clone()
                .baseUrl(botAiProperties.baseUrl())
                .build();
        this.botAiProperties = botAiProperties;
        this.fallbackGenerator = fallbackGenerator;
    }

    @Override
    public String generateReply(MessagePublishedEvent event) {
        try {
            OpenRouterResponse response = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + botAiProperties.apiKey())
                    .header("HTTP-Referer", "http://localhost:8080")
                    .header("X-Title", "Labb2 Microservices Chat")
                    .body(request(event))
                    .retrieve()
                    .body(OpenRouterResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                logger.warn("OpenRouter returned an empty response. Falling back to local bot response.");
                return fallbackGenerator.generateReply(event);
            }

            String content = response.choices().getFirst().message().content();
            if (!StringUtils.hasText(content)) {
                logger.warn("OpenRouter returned an empty message. Falling back to local bot response.");
                return fallbackGenerator.generateReply(event);
            }

            return content.trim();
        } catch (Exception exception) {
            logger.warn("Could not generate AI bot response. Falling back to local bot response: {}",
                    exception.getMessage());
            return fallbackGenerator.generateReply(event);
        }
    }

    private OpenRouterRequest request(MessagePublishedEvent event) {
        return new OpenRouterRequest(
                botAiProperties.model(),
                List.of(
                        new OpenRouterMessage("system", SYSTEM_PROMPT),
                        new OpenRouterMessage("user", userPrompt(event))
                ),
                botAiProperties.temperature()
        );
    }

    private String userPrompt(MessagePublishedEvent event) {
        String username = StringUtils.hasText(event.senderUsername()) ? event.senderUsername() : "okänd användare";
        return username + " skrev i rummet " + event.room() + ": " + normalizeMessage(event.content());
    }

    private String normalizeMessage(String content) {
        if (content == null) {
            return "";
        }

        return content
                .replaceAll("(?i)@bot", "")
                .replaceAll("\\s+([,.!?])", "$1")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    public record OpenRouterRequest(
            String model,
            List<OpenRouterMessage> messages,
            double temperature
    ) {
    }

    public record OpenRouterMessage(
            String role,
            String content
    ) {
    }

    public record OpenRouterResponse(
            List<Choice> choices
    ) {
    }

    public record Choice(
            Message message
    ) {
    }

    public record Message(
            String role,
            String content
    ) {
    }
}

package org.example.botservice.service;

import org.example.botservice.config.BotAiProperties;
import org.example.event.MessagePublishedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class OpenRouterBotResponseGenerator implements BotResponseGenerator {
    private static final Logger logger = LoggerFactory.getLogger(OpenRouterBotResponseGenerator.class);
    private static final int HISTORY_LIMIT = 10;

    private final RestClient restClient;
    private final RestClient messageServiceRestClient;
    private final BotAiProperties botAiProperties;
    private final RuleBasedBotReplyGenerator fallbackGenerator;
    private final BotPersonalityPrompts botPersonalityPrompts;

    public OpenRouterBotResponseGenerator(
            RestClient.Builder restClientBuilder,
            RestClient messageServiceRestClient,
            BotAiProperties botAiProperties,
            RuleBasedBotReplyGenerator fallbackGenerator,
            BotPersonalityPrompts botPersonalityPrompts
    ) {
        if (!StringUtils.hasText(botAiProperties.apiKey())) {
            throw new IllegalStateException("bot.ai.api-key must be configured when bot.ai.enabled=true");
        }

        this.restClient = restClientBuilder
                .clone()
                .baseUrl(botAiProperties.baseUrl())
                .build();
        this.messageServiceRestClient = messageServiceRestClient;
        this.botAiProperties = botAiProperties;
        this.fallbackGenerator = fallbackGenerator;
        this.botPersonalityPrompts = botPersonalityPrompts;
    }

    @Override
    public String generateReply(MessagePublishedEvent event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + botAiProperties.apiKey())
                    .header("HTTP-Referer", "http://localhost:8080")
                    .header("X-Title", "Labb2 Microservices Chat")
                    .body(request(event))
                    .retrieve()
                    .body(Map.class);

            String content = extractAssistantContent(response);
            if (!StringUtils.hasText(content)) {
                logger.warn("OpenRouter returned an empty response. Falling back to local bot response.");
                return fallbackGenerator.generateReply(event);
            }

            return content.trim();
        } catch (Exception exception) {
            logger.warn("Could not generate AI bot response. Falling back to local bot response: {}",
                    exception.getMessage());
            return fallbackGenerator.generateReply(event);
        }
    }

    private Map<String, Object> request(MessagePublishedEvent event) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", botPersonalityPrompts.systemPromptFor(event.botPersonality())));
        messages.addAll(chatHistory(event));
        messages.add(Map.of("role", "user", "content", userPrompt(event)));

        return Map.of(
                "model", botAiProperties.model(),
                "messages", messages,
                "temperature", botAiProperties.temperature()
        );
    }

    private List<Map<String, String>> chatHistory(MessagePublishedEvent event) {
        try {
            Map<?, ?>[] response = messageServiceRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/messages")
                            .queryParam("room", normalizeRoom(event.room()))
                            .build())
                    .retrieve()
                    .body(Map[].class);

            if (response == null || response.length == 0) {
                return List.of();
            }

            List<Map<?, ?>> messages = Arrays.asList(response);
            int startIndex = Math.max(0, messages.size() - HISTORY_LIMIT);
            List<Map<String, String>> history = new ArrayList<>();

            for (Map<?, ?> message : messages.subList(startIndex, messages.size())) {
                if (Objects.equals(asLong(message.get("id")), event.messageId())) {
                    continue;
                }

                String content = normalizeMessage(asString(message.get("content")));
                if (!StringUtils.hasText(content)) {
                    continue;
                }

                String senderUsername = asString(message.get("senderUsername"));
                if ("bot".equalsIgnoreCase(senderUsername)) {
                    history.add(Map.of("role", "assistant", "content", content));
                } else {
                    String username = StringUtils.hasText(senderUsername) ? senderUsername : "användare";
                    history.add(Map.of("role", "user", "content", username + ": " + content));
                }
            }

            return history;
        } catch (Exception exception) {
            logger.warn("Could not load chat history for AI bot response: {}", exception.getMessage());
            return List.of();
        }
    }

    private String extractAssistantContent(Map<String, Object> response) {
        if (response == null) {
            return null;
        }

        Object choicesObject = response.get("choices");
        if (!(choicesObject instanceof List<?> choices) || choices.isEmpty()) {
            return null;
        }

        Object firstChoice = choices.get(0);
        if (!(firstChoice instanceof Map<?, ?> choice)) {
            return null;
        }

        Object messageObject = choice.get("message");
        if (!(messageObject instanceof Map<?, ?> message)) {
            return null;
        }

        Object contentObject = message.get("content");
        if (contentObject instanceof String content) {
            return content;
        }

        return null;
    }

    private String userPrompt(MessagePublishedEvent event) {
        String username = StringUtils.hasText(event.senderUsername()) ? event.senderUsername() : "okänd användare";
        return username + " skrev i rummet " + normalizeRoom(event.room()) + ": " + normalizeMessage(event.content());
    }

    private String normalizeRoom(String room) {
        if (!StringUtils.hasText(room)) {
            return "general";
        }

        return room.trim().toLowerCase();
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

    private String asString(Object value) {
        return value instanceof String text ? text : null;
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }
}

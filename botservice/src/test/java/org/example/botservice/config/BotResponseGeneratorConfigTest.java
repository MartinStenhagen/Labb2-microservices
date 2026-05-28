package org.example.botservice.config;

import org.example.botservice.service.BotPersonalityPrompts;
import org.example.botservice.service.OpenRouterBotResponseGenerator;
import org.example.botservice.service.RuleBasedBotReplyGenerator;
import org.example.event.MessagePublishedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BotResponseGeneratorConfigTest {

    private final BotResponseGeneratorConfig config = new BotResponseGeneratorConfig();
    private final RuleBasedBotReplyGenerator ruleBasedGenerator = new RuleBasedBotReplyGenerator();
    private final BotPersonalityPrompts botPersonalityPrompts = new BotPersonalityPrompts();

    @Test
    void botResponseGeneratorUsesRuleBasedGeneratorWhenAiIsDisabled() {
        var generator = config.botResponseGenerator(
                RestClient.builder(),
                RestClient.builder().baseUrl("http://messageservice").build(),
                new BotAiProperties(false, "http://openrouter.test", "", "test-model", 0.2),
                ruleBasedGenerator,
                botPersonalityPrompts
        );

        assertThat(generator.generateReply(event("@bot hjälp mig")))
                .contains("Jag kan hjälpa till med frågor");
    }

    @Test
    void botResponseGeneratorCreatesOpenRouterGeneratorWhenAiIsEnabled() {
        var generator = config.botResponseGenerator(
                RestClient.builder(),
                RestClient.builder().baseUrl("http://messageservice").build(),
                new BotAiProperties(true, "http://openrouter.test", "test-key", "test-model", 0.2),
                ruleBasedGenerator,
                botPersonalityPrompts
        );

        assertThat(generator).isInstanceOf(OpenRouterBotResponseGenerator.class);
    }

    private MessagePublishedEvent event(String content) {
        return new MessagePublishedEvent(
                UUID.randomUUID(),
                1L,
                1L,
                "martin",
                "general",
                "neutral",
                content,
                LocalDateTime.now()
        );
    }
}

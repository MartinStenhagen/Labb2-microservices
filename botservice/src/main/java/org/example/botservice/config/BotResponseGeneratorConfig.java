package org.example.botservice.config;

import org.example.botservice.service.BotResponseGenerator;
import org.example.botservice.service.OpenRouterBotResponseGenerator;
import org.example.botservice.service.BotPersonalityPrompts;
import org.example.botservice.service.RuleBasedBotReplyGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class BotResponseGeneratorConfig {

    @Bean
    RuleBasedBotReplyGenerator ruleBasedBotReplyGenerator() {
        return new RuleBasedBotReplyGenerator();
    }

    @Bean
    BotPersonalityPrompts botPersonalityPrompts() {
        return new BotPersonalityPrompts();
    }

    @Bean
    @ConditionalOnProperty(prefix = "bot.ai", name = "enabled", havingValue = "true")
    BotResponseGenerator openRouterBotResponseGenerator(
            RestClient.Builder restClientBuilder,
            RestClient messageServiceRestClient,
            BotAiProperties botAiProperties,
            RuleBasedBotReplyGenerator ruleBasedBotReplyGenerator,
            BotPersonalityPrompts botPersonalityPrompts
    ) {
        return new OpenRouterBotResponseGenerator(
                restClientBuilder,
                messageServiceRestClient,
                botAiProperties,
                ruleBasedBotReplyGenerator,
                botPersonalityPrompts
        );
    }

    @Bean
    @ConditionalOnProperty(prefix = "bot.ai", name = "enabled", havingValue = "false", matchIfMissing = true)
    BotResponseGenerator localBotResponseGenerator(RuleBasedBotReplyGenerator ruleBasedBotReplyGenerator) {
        return ruleBasedBotReplyGenerator::generateReply;
    }
}

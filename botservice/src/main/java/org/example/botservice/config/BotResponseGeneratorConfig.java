package org.example.botservice.config;

import org.example.botservice.service.BotResponseGenerator;
import org.example.botservice.service.OpenRouterBotResponseGenerator;
import org.example.botservice.service.BotPersonalityPrompts;
import org.example.botservice.service.RuleBasedBotReplyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class BotResponseGeneratorConfig {
    private static final Logger logger = LoggerFactory.getLogger(BotResponseGeneratorConfig.class);

    @Bean
    RuleBasedBotReplyGenerator ruleBasedBotReplyGenerator() {
        return new RuleBasedBotReplyGenerator();
    }

    @Bean
    BotPersonalityPrompts botPersonalityPrompts() {
        return new BotPersonalityPrompts();
    }

    @Bean
    BotResponseGenerator botResponseGenerator(
            RestClient.Builder restClientBuilder,
            RestClient messageServiceRestClient,
            BotAiProperties botAiProperties,
            RuleBasedBotReplyGenerator ruleBasedBotReplyGenerator,
            BotPersonalityPrompts botPersonalityPrompts
    ) {
        if (!botAiProperties.enabled()) {
            logger.info("AI bot response generation is disabled. Using rule-based bot replies.");
            return ruleBasedBotReplyGenerator::generateReply;
        }

        logger.info(
                "AI bot response generation is enabled. Using OpenRouter model {}.",
                botAiProperties.model()
        );
        return new OpenRouterBotResponseGenerator(
                restClientBuilder,
                messageServiceRestClient,
                botAiProperties,
                ruleBasedBotReplyGenerator,
                botPersonalityPrompts
        );
    }
}

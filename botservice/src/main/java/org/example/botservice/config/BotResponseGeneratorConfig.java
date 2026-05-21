package org.example.botservice.config;

import org.example.botservice.service.BotResponseGenerator;
import org.example.botservice.service.OpenRouterBotResponseGenerator;
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
    @ConditionalOnProperty(prefix = "bot.ai", name = "enabled", havingValue = "true")
    BotResponseGenerator openRouterBotResponseGenerator(
            RestClient.Builder restClientBuilder,
            BotAiProperties botAiProperties,
            RuleBasedBotReplyGenerator ruleBasedBotReplyGenerator
    ) {
        return new OpenRouterBotResponseGenerator(restClientBuilder, botAiProperties, ruleBasedBotReplyGenerator);
    }

    @Bean
    @ConditionalOnProperty(prefix = "bot.ai", name = "enabled", havingValue = "false", matchIfMissing = true)
    BotResponseGenerator localBotResponseGenerator(RuleBasedBotReplyGenerator ruleBasedBotReplyGenerator) {
        return ruleBasedBotReplyGenerator::generateReply;
    }
}

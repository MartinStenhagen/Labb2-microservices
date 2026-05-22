package org.example.botservice.service;

import org.example.botservice.config.BotAiProperties;
import org.example.event.MessagePublishedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenRouterBotResponseGeneratorTest {

    @Test
    void generateReplyCallsOpenRouterChatCompletions() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenRouterBotResponseGenerator generator = new OpenRouterBotResponseGenerator(
                builder,
                properties(),
                new RuleBasedBotReplyGenerator()
        );

        server.expect(requestTo("http://openrouter.test/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("test-model"))
                .andExpect(jsonPath("$.temperature").value(0.2))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andExpect(jsonPath("$.messages[1].content").value("martin skrev i rummet support: Hej, hjälp mig"))
                .andRespond(withSuccess("""
                        {
                          "choices": [
                            { "message": { "role": "assistant", "content": "Absolut, här är ett svar." } }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        String reply = generator.generateReply(event("Hej @bot, hjälp mig"));

        assertThat(reply).isEqualTo("Absolut, här är ett svar.");
        server.verify();
    }

    @Test
    void generateReplyFallsBackWhenOpenRouterFails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenRouterBotResponseGenerator generator = new OpenRouterBotResponseGenerator(
                builder,
                properties(),
                new RuleBasedBotReplyGenerator()
        );

        server.expect(requestTo("http://openrouter.test/chat/completions"))
                .andRespond(withServerError());

        String reply = generator.generateReply(event("Hej @bot, hjälp mig"));

        assertThat(reply).contains("Jag kan hjälpa till med frågor");
        server.verify();
    }

    private BotAiProperties properties() {
        return new BotAiProperties(true, "http://openrouter.test", "test-key", "test-model", 0.2);
    }

    private MessagePublishedEvent event(String content) {
        return new MessagePublishedEvent(
                UUID.randomUUID(),
                1L,
                1L,
                "martin",
                "support",
                content,
                LocalDateTime.now()
        );
    }
}

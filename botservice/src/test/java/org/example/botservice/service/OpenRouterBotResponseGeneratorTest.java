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
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenRouterBotResponseGeneratorTest {

    @Test
    void generateReplyCallsOpenRouterChatCompletionsWithRecentRoomHistory() {
        RestClient.Builder openRouterBuilder = RestClient.builder();
        MockRestServiceServer openRouterServer = MockRestServiceServer.bindTo(openRouterBuilder).build();
        RestClient.Builder messageServiceBuilder = RestClient.builder().baseUrl("http://messageservice");
        MockRestServiceServer messageServiceServer = MockRestServiceServer.bindTo(messageServiceBuilder).build();
        OpenRouterBotResponseGenerator generator = new OpenRouterBotResponseGenerator(
                openRouterBuilder,
                messageServiceBuilder.build(),
                properties(),
                new RuleBasedBotReplyGenerator(),
                new BotPersonalityPrompts()
        );

        messageServiceServer.expect(requestTo("http://messageservice/messages?room=support"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          { "id": 10, "senderUsername": "sara", "content": "Vad pratade vi om?" },
                          { "id": 11, "senderUsername": "bot", "content": "Vi pratade om mikrotjänster." },
                          { "id": 1, "senderUsername": "martin", "content": "Hej @bot, hjälp mig" }
                        ]
                        """, MediaType.APPLICATION_JSON));

        openRouterServer.expect(requestTo("http://openrouter.test/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("test-model"))
                .andExpect(jsonPath("$.temperature").value(0.2))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andExpect(jsonPath("$.messages[1].content").value("sara: Vad pratade vi om?"))
                .andExpect(jsonPath("$.messages[2].role").value("assistant"))
                .andExpect(jsonPath("$.messages[2].content").value("Vi pratade om mikrotjänster."))
                .andExpect(jsonPath("$.messages[3].role").value("user"))
                .andExpect(jsonPath("$.messages[3].content").value("martin skrev i rummet support: Hej, hjälp mig"))
                .andRespond(withSuccess("""
                        {
                          "choices": [
                            { "message": { "role": "assistant", "content": "Absolut, här är ett svar." } }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        String reply = generator.generateReply(event("Hej @bot, hjälp mig"));

        assertThat(reply).isEqualTo("Absolut, här är ett svar.");
        messageServiceServer.verify();
        openRouterServer.verify();
    }

    @Test
    void generateReplyFallsBackWhenOpenRouterFails() {
        RestClient.Builder openRouterBuilder = RestClient.builder();
        MockRestServiceServer openRouterServer = MockRestServiceServer.bindTo(openRouterBuilder).build();
        RestClient.Builder messageServiceBuilder = RestClient.builder().baseUrl("http://messageservice");
        MockRestServiceServer messageServiceServer = MockRestServiceServer.bindTo(messageServiceBuilder).build();
        OpenRouterBotResponseGenerator generator = new OpenRouterBotResponseGenerator(
                openRouterBuilder,
                messageServiceBuilder.build(),
                properties(),
                new RuleBasedBotReplyGenerator(),
                new BotPersonalityPrompts()
        );

        messageServiceServer.expect(requestTo("http://messageservice/messages?room=support"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        openRouterServer.expect(requestTo("http://openrouter.test/chat/completions"))
                .andRespond(withServerError());

        String reply = generator.generateReply(event("Hej @bot, hjälp mig"));

        assertThat(reply).contains("Jag kan hjälpa till med frågor");
        messageServiceServer.verify();
        openRouterServer.verify();
    }

    @Test
    void generateReplyUsesPiratePromptWhenRequested() {
        RestClient.Builder openRouterBuilder = RestClient.builder();
        MockRestServiceServer openRouterServer = MockRestServiceServer.bindTo(openRouterBuilder).build();
        RestClient.Builder messageServiceBuilder = RestClient.builder().baseUrl("http://messageservice");
        MockRestServiceServer messageServiceServer = MockRestServiceServer.bindTo(messageServiceBuilder).build();
        OpenRouterBotResponseGenerator generator = new OpenRouterBotResponseGenerator(
                openRouterBuilder,
                messageServiceBuilder.build(),
                properties(),
                new RuleBasedBotReplyGenerator(),
                new BotPersonalityPrompts()
        );

        messageServiceServer.expect(requestTo("http://messageservice/messages?room=support"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        openRouterServer.expect(requestTo("http://openrouter.test/chat/completions"))
                .andExpect(jsonPath("$.messages[0].content").value(containsString("piratstil")))
                .andRespond(withSuccess("""
                        {
                          "choices": [
                            { "message": { "role": "assistant", "content": "Aj aj, det ordnar vi." } }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        String reply = generator.generateReply(event("Hej @bot", "pirate"));

        assertThat(reply).isEqualTo("Aj aj, det ordnar vi.");
        messageServiceServer.verify();
        openRouterServer.verify();
    }

    private BotAiProperties properties() {
        return new BotAiProperties(true, "http://openrouter.test", "test-key", "test-model", 0.2);
    }

    private MessagePublishedEvent event(String content) {
        return event(content, "neutral");
    }

    private MessagePublishedEvent event(String content, String botPersonality) {
        return new MessagePublishedEvent(
                UUID.randomUUID(),
                1L,
                1L,
                "martin",
                "support",
                botPersonality,
                content,
                LocalDateTime.now()
        );
    }
}

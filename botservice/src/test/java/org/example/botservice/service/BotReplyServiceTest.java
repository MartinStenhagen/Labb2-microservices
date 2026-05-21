package org.example.botservice.service;

import org.example.event.MessagePublishedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BotReplyServiceTest {

    @Test
    void replyIfMentionedPostsBotMessageToMessageService() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://messageservice");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        BotReplyService botReplyService = new BotReplyService(builder.build(), 0L, "bot");

        server.expect(requestTo("http://messageservice/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.senderUserId").value(0))
                .andExpect(jsonPath("$.senderUsername").value("bot"))
                .andExpect(jsonPath("$.content").value("Bot reply: I saw your message, martin."))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        botReplyService.replyIfMentioned(event("martin", "Hej @bot"));

        server.verify();
    }

    @Test
    void replyIfMentionedIgnoresMessagesWithoutBotMention() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://messageservice");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        BotReplyService botReplyService = new BotReplyService(builder.build(), 0L, "bot");

        botReplyService.replyIfMentioned(event("martin", "Hej alla"));

        server.verify();
    }

    @Test
    void replyIfMentionedIgnoresBotMessagesToAvoidLoop() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://messageservice");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        BotReplyService botReplyService = new BotReplyService(builder.build(), 0L, "bot");

        botReplyService.replyIfMentioned(event("bot", "Hej @bot"));

        server.verify();
    }

    private MessagePublishedEvent event(String senderUsername, String content) {
        return new MessagePublishedEvent(
                UUID.randomUUID(),
                1L,
                1L,
                senderUsername,
                content,
                LocalDateTime.now()
        );
    }
}

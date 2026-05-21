package org.example.botservice.service;

import org.example.botservice.dto.CreateBotMessageRequest;
import org.example.event.MessagePublishedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class BotReplyService {
    private static final Logger logger = LoggerFactory.getLogger(BotReplyService.class);

    private final RestClient messageServiceRestClient;
    private final long botUserId;
    private final String botUsername;

    public BotReplyService(
            RestClient messageServiceRestClient,
            @Value("${bot.user-id}") long botUserId,
            @Value("${bot.username}") String botUsername
    ) {
        this.messageServiceRestClient = messageServiceRestClient;
        this.botUserId = botUserId;
        this.botUsername = botUsername;
    }

    public void replyIfMentioned(MessagePublishedEvent event) {
        if (botUsername.equalsIgnoreCase(event.senderUsername())) {
            logger.debug("Ignoring bot message {} to avoid reply loop", event.messageId());
            return;
        }

        if (event.content() == null || !event.content().toLowerCase().contains("@bot")) {
            logger.debug("Message {} did not mention @bot", event.messageId());
            return;
        }

        String reply = "Bot reply: I saw your message, " + event.senderUsername() + ".";

        CreateBotMessageRequest request = new CreateBotMessageRequest(
                botUserId,
                botUsername,
                reply
        );

        messageServiceRestClient.post()
                .uri("/messages")
                .body(request)
                .retrieve()
                .toBodilessEntity();

        logger.info("Published bot reply for message {}", event.messageId());
    }
}

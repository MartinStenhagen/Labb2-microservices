package org.example.botservice.service;

import org.example.botservice.dto.CreateBotMessageRequest;
import org.example.event.MessagePublishedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class BotReplyService {
    private static final Logger logger = LoggerFactory.getLogger(BotReplyService.class);
    private static final long BOT_USER_ID = 0L;
    private static final String BOT_USERNAME = "bot";

    private final RestClient messageServiceRestClient;

    public BotReplyService(RestClient messageServiceRestClient) {
        this.messageServiceRestClient = messageServiceRestClient;
    }

    public void replyIfMentioned(MessagePublishedEvent event) {
        if (BOT_USERNAME.equalsIgnoreCase(event.senderUsername())) {
            logger.debug("Ignoring bot message {} to avoid reply loop", event.messageId());
            return;
        }

        if (event.content() == null || !event.content().toLowerCase().contains("@bot")) {
            logger.debug("Message {} did not mention @bot", event.messageId());
            return;
        }

        String reply = "Bot reply: I saw your message, " + event.senderUsername() + ".";

        CreateBotMessageRequest request = new CreateBotMessageRequest(
                BOT_USER_ID,
                BOT_USERNAME,
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

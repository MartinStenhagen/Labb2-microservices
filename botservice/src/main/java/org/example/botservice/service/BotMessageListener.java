package org.example.botservice.service;

import org.example.botservice.config.RabbitConfig;
import org.example.event.MessagePublishedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class BotMessageListener {
    private static final Logger logger = LoggerFactory.getLogger(BotMessageListener.class);

    private final BotReplyService botReplyService;

    public BotMessageListener(BotReplyService botReplyService) {
        this.botReplyService = botReplyService;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_NAME)
    public void handleMessagePublished(MessagePublishedEvent event) {
        logger.info("Received message-published event: messageId={}, sender={}", event.messageId(), event.senderUsername());
        botReplyService.replyIfMentioned(event);
    }
}

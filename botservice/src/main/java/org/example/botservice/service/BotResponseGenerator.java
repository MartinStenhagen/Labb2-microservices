package org.example.botservice.service;

import org.example.event.MessagePublishedEvent;

public interface BotResponseGenerator {
    String generateReply(MessagePublishedEvent event);
}

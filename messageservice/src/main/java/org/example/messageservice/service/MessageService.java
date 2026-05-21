package org.example.messageservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.event.MessagePublishedEvent;
import org.example.messageservice.client.UserProfileClient;
import org.example.messageservice.model.ChatMessage;
import org.example.messageservice.model.OutboxEvent;
import org.example.messageservice.repository.MessageRepository;
import org.example.messageservice.repository.OutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final UserProfileClient userProfileClient;
    private final Long botUserId;
    private final String botUsername;

    public MessageService(
            MessageRepository messageRepository,
            OutboxRepository outboxRepository,
            ObjectMapper objectMapper,
            UserProfileClient userProfileClient,
            @Value("${bot.user-id}") Long botUserId,
            @Value("${bot.username}") String botUsername
    ) {
        this.messageRepository = messageRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.userProfileClient = userProfileClient;
        this.botUserId = botUserId;
        this.botUsername = botUsername;
    }

    @Transactional
    public ChatMessage publishMessage(ChatMessage message) throws JsonProcessingException {
        enrichSenderProfile(message);
        ChatMessage savedMessage = messageRepository.save(message);

        MessagePublishedEvent event = new MessagePublishedEvent(
                UUID.randomUUID(),
                savedMessage.getId(),
                savedMessage.getSenderUserId(),
                savedMessage.getSenderUsername(),
                savedMessage.getContent(),
                savedMessage.getCreatedAt()
        );

        String payload = objectMapper.writeValueAsString(event);

        OutboxEvent outboxEvent = new OutboxEvent(
                event.eventId(),
                "MESSAGE",
                savedMessage.getId(),
                "MESSAGE_PUBLISHED",
                payload
        );

        outboxRepository.save(outboxEvent);

        return savedMessage;
    }

    private void enrichSenderProfile(ChatMessage message) {
        if (message.getSenderUserId() == null) {
            throw new IllegalArgumentException("senderUserId is required");
        }

        if (botUserId.equals(message.getSenderUserId()) && botUsername.equalsIgnoreCase(message.getSenderUsername())) {
            return;
        }

        var userProfile = userProfileClient.getUserProfile(message.getSenderUserId());
        message.setSenderUsername(userProfile.username());
    }

    public ChatMessage getMessage(Long id) {
        return messageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found"));
    }

    public List<ChatMessage> getMessages() {
        return messageRepository.findAll();
    }
}

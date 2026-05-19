package org.example.messageservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.event.MessagePublishedEvent;
import org.example.messageservice.model.ChatMessage;
import org.example.messageservice.model.OutboxEvent;
import org.example.messageservice.repository.MessageRepository;
import org.example.messageservice.repository.OutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public MessageService(
            MessageRepository messageRepository,
            OutboxRepository outboxRepository,
            ObjectMapper objectMapper
    ) {
        this.messageRepository = messageRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ChatMessage publishMessage(ChatMessage message) throws JsonProcessingException {
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

    public ChatMessage getMessage(Long id) {
        return messageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found"));
    }

    public List<ChatMessage> getMessages() {
        return messageRepository.findAll();
    }
}
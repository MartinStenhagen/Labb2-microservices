package org.example.messageservice.service;

import org.example.event.MessagePublishedEvent;
import org.example.messageservice.config.RabbitConfig;
import org.example.messageservice.model.OutboxEvent;
import org.example.messageservice.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OutboxRelay {
    private static final Logger logger = LoggerFactory.getLogger(OutboxRelay.class);
    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public OutboxRelay(OutboxRepository outboxRepository, RabbitTemplate rabbitTemplate, com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        
        this.rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack && correlationData != null) {
                Long id = Long.valueOf(correlationData.getId());
                updateStatus(id, OutboxEvent.OutboxStatus.PROCESSED);
                logger.info("Message {} successfully published and acked", id);
            } else if (correlationData != null) {
                Long id = Long.valueOf(correlationData.getId());
                logger.error("Message {} failed to publish: {}", id, cause);
                // Optionally retry or mark as FAILED
            }
        });
    }

    private void updateStatus(Long id, OutboxEvent.OutboxStatus status) {
        outboxRepository.findById(id).ifPresent(event -> {
            event.setStatus(status);
            outboxRepository.save(event);
        });
    }

    @Scheduled(fixedDelay = 5000)
    public void relayEvents() {
        List<OutboxEvent> pendingEvents = outboxRepository.findByStatus(OutboxEvent.OutboxStatus.PENDING);
        for (OutboxEvent event : pendingEvents) {
            try {
                logger.info("Outbox Recovery: Relaying pending event {} (Aggregate ID: {})", event.getEventId(), event.getAggregateId());
                logger.debug("Relaying event: {}", event.getEventId());
                CorrelationData correlationData = new CorrelationData(event.getId().toString());
                MessagePublishedEvent messagePayload = objectMapper.readValue(event.getPayload(), MessagePublishedEvent.class);

                rabbitTemplate.convertAndSend(
                    RabbitConfig.EXCHANGE_NAME,
                    RabbitConfig.ROUTING_KEY,
                    messagePayload,
                    correlationData
                );
            } catch (Exception e) {
                logger.error("Error relaying event {}: {}", event.getId(), e.getMessage());
            }
        }
    }
}

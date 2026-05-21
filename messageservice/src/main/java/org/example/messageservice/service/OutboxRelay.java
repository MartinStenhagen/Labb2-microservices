package org.example.messageservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.event.MessagePublishedEvent;
import org.example.messageservice.config.RabbitConfig;
import org.example.messageservice.model.OutboxEvent;
import org.example.messageservice.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OutboxRelay {
    private static final Logger logger = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final int maxAttempts;
    private final long publishingTimeoutMs;

    public OutboxRelay(
            OutboxRepository outboxRepository,
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            @Value("${outbox.max-attempts:5}") int maxAttempts,
            @Value("${outbox.publishing-timeout-ms:30000}") long publishingTimeoutMs
    ) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.maxAttempts = maxAttempts;
        this.publishingTimeoutMs = publishingTimeoutMs;

        this.rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            Long id = parseCorrelationId(correlationData);
            if (id == null) {
                return;
            }

            if (ack) {
                markProcessed(id);
                logger.info("Outbox event {} successfully published and acked", id);
            } else {
                String error = "RabbitMQ negative acknowledgement: " + cause;
                markRetryableFailure(id, error);
                logger.error("Outbox event {} failed to publish: {}", id, cause);
            }
        });
    }

    @Scheduled(fixedDelayString = "${outbox.relay-delay-ms:5000}")
    public void relayEvents() {
        recoverStalePublishingEvents();

        List<OutboxEvent> pendingEvents = outboxRepository.findByStatusOrderByCreatedAtAsc(
                OutboxEvent.OutboxStatus.PENDING
        );

        for (OutboxEvent event : pendingEvents) {
            relayEvent(event);
        }
    }

    private void relayEvent(OutboxEvent event) {
        event.markPublishingAttempt();
        outboxRepository.save(event);

        try {
            logger.info(
                    "Outbox recovery: relaying event {} attempt {} of {} (aggregate id {})",
                    event.getEventId(),
                    event.getPublishAttempts(),
                    maxAttempts,
                    event.getAggregateId()
            );

            CorrelationData correlationData = new CorrelationData(event.getId().toString());
            MessagePublishedEvent messagePayload = objectMapper.readValue(event.getPayload(), MessagePublishedEvent.class);

            rabbitTemplate.convertAndSend(
                    RabbitConfig.EXCHANGE_NAME,
                    RabbitConfig.ROUTING_KEY,
                    messagePayload,
                    correlationData
            );
        } catch (Exception exception) {
            markRetryableFailure(event, exception.getMessage());
            logger.error("Error relaying outbox event {}: {}", event.getId(), exception.getMessage());
        }
    }

    private void recoverStalePublishingEvents() {
        LocalDateTime cutoff = LocalDateTime.now().minusNanos(publishingTimeoutMs * 1_000_000);
        List<OutboxEvent> staleEvents = outboxRepository.findByStatusAndLastAttemptAtBeforeOrderByCreatedAtAsc(
                OutboxEvent.OutboxStatus.PUBLISHING,
                cutoff
        );

        for (OutboxEvent event : staleEvents) {
            markRetryableFailure(event, "Publishing confirmation timed out");
        }
    }

    private void markProcessed(Long id) {
        outboxRepository.findById(id).ifPresent(event -> {
            event.markProcessed();
            outboxRepository.save(event);
        });
    }

    private void markRetryableFailure(Long id, String error) {
        outboxRepository.findById(id).ifPresent(event -> markRetryableFailure(event, error));
    }

    private void markRetryableFailure(OutboxEvent event, String error) {
        if (event.hasReachedMaxAttempts(maxAttempts)) {
            event.markFailed(error);
            logger.error("Outbox event {} marked FAILED after {} attempts", event.getId(), event.getPublishAttempts());
        } else {
            event.markPending(error);
            logger.warn("Outbox event {} will be retried after failure: {}", event.getId(), error);
        }

        outboxRepository.save(event);
    }

    private Long parseCorrelationId(CorrelationData correlationData) {
        if (correlationData == null || correlationData.getId() == null) {
            logger.warn("RabbitMQ confirm callback had no correlation id");
            return null;
        }

        try {
            return Long.valueOf(correlationData.getId());
        } catch (NumberFormatException exception) {
            logger.error("Invalid RabbitMQ correlation id: {}", correlationData.getId());
            return null;
        }
    }
}

package org.example.messageservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.event.MessagePublishedEvent;
import org.example.messageservice.config.RabbitConfig;
import org.example.messageservice.model.OutboxEvent;
import org.example.messageservice.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private ObjectMapper objectMapper;
    private OutboxRelay outboxRelay;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        outboxRelay = new OutboxRelay(outboxRepository, rabbitTemplate, objectMapper, 2, 30000);
    }

    @Test
    void relayEventsMarksEventPublishingAndPublishesToRabbitMq() throws Exception {
        OutboxEvent event = outboxEvent();
        when(outboxRepository.findByStatusAndLastAttemptAtBeforeOrderByCreatedAtAsc(
                eq(OutboxEvent.OutboxStatus.PUBLISHING),
                any(LocalDateTime.class)
        )).thenReturn(List.of());
        when(outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxEvent.OutboxStatus.PENDING))
                .thenReturn(List.of(event));

        outboxRelay.relayEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.PUBLISHING);
        assertThat(event.getPublishAttempts()).isEqualTo(1);
        assertThat(event.getLastAttemptAt()).isNotNull();
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConfig.EXCHANGE_NAME),
                eq(RabbitConfig.ROUTING_KEY),
                any(MessagePublishedEvent.class),
                any(CorrelationData.class)
        );
    }

    @Test
    void confirmAckMarksEventProcessed() throws Exception {
        OutboxEvent event = outboxEvent();
        event.markPublishingAttempt();
        when(outboxRepository.findById(42L)).thenReturn(Optional.of(event));

        RabbitTemplate.ConfirmCallback callback = confirmCallback();
        callback.confirm(new CorrelationData("42"), true, null);

        assertThat(event.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.PROCESSED);
        assertThat(event.getProcessedAt()).isNotNull();
        assertThat(event.getLastError()).isNull();
        verify(outboxRepository, atLeastOnce()).save(event);
    }

    @Test
    void confirmNackMarksFailedAfterMaxAttempts() throws Exception {
        OutboxEvent event = outboxEvent();
        event.markPublishingAttempt();
        event.markPending("first failure");
        event.markPublishingAttempt();
        when(outboxRepository.findById(42L)).thenReturn(Optional.of(event));

        RabbitTemplate.ConfirmCallback callback = confirmCallback();
        callback.confirm(new CorrelationData("42"), false, "broker refused message");

        assertThat(event.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.FAILED);
        assertThat(event.getFailedAt()).isNotNull();
        assertThat(event.getLastError()).contains("broker refused message");
        verify(outboxRepository, atLeastOnce()).save(event);
    }

    @Test
    void synchronousPublishFailureIsRetriedUntilMaxAttempts() throws Exception {
        OutboxEvent event = outboxEvent();
        event.markPublishingAttempt();
        event.markPending("first failure");

        when(outboxRepository.findByStatusAndLastAttemptAtBeforeOrderByCreatedAtAsc(
                eq(OutboxEvent.OutboxStatus.PUBLISHING),
                any(LocalDateTime.class)
        )).thenReturn(List.of());
        when(outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxEvent.OutboxStatus.PENDING))
                .thenReturn(List.of(event));
        doThrow(new RuntimeException("RabbitMQ unavailable"))
                .when(rabbitTemplate)
                .convertAndSend(any(String.class), any(String.class), any(Object.class), any(CorrelationData.class));

        outboxRelay.relayEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.FAILED);
        assertThat(event.getPublishAttempts()).isEqualTo(2);
        assertThat(event.getLastError()).isEqualTo("RabbitMQ unavailable");
        verify(outboxRepository, atLeastOnce()).save(event);
    }

    private RabbitTemplate.ConfirmCallback confirmCallback() {
        ArgumentCaptor<RabbitTemplate.ConfirmCallback> captor = ArgumentCaptor.forClass(RabbitTemplate.ConfirmCallback.class);
        verify(rabbitTemplate).setConfirmCallback(captor.capture());
        return captor.getValue();
    }

    private OutboxEvent outboxEvent() throws Exception {
        MessagePublishedEvent payload = new MessagePublishedEvent(
                UUID.randomUUID(),
                10L,
                1L,
                "martin",
                "general",
                "neutral",
                "Hej @bot",
                LocalDateTime.now()
        );

        OutboxEvent event = new OutboxEvent(
                payload.eventId(),
                "MESSAGE",
                payload.messageId(),
                "MESSAGE_PUBLISHED",
                objectMapper.writeValueAsString(payload)
        );
        ReflectionTestUtils.setField(event, "id", 42L);
        return event;
    }
}

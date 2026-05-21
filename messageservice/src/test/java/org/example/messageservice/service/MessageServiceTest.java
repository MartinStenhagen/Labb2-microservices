package org.example.messageservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.event.MessagePublishedEvent;
import org.example.messageservice.client.UserProfileClient;
import org.example.messageservice.model.ChatMessage;
import org.example.messageservice.model.OutboxEvent;
import org.example.messageservice.repository.MessageRepository;
import org.example.messageservice.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private UserProfileClient userProfileClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private MessageService messageService;

    @BeforeEach
    void setUp() {
        messageService = new MessageService(
                messageRepository,
                outboxRepository,
                objectMapper,
                userProfileClient,
                0L,
                "bot"
        );
    }

    @Test
    void publishMessageEnrichesSenderAndCreatesOutboxEvent() throws Exception {
        ChatMessage message = new ChatMessage();
        message.setSenderUserId(1L);
        message.setContent("Hej @bot");

        when(userProfileClient.getUserProfile(1L))
                .thenReturn(new UserProfileClient.UserProfile(1L, "martin", "Martin Stenhagen"));
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage savedMessage = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedMessage, "id", 42L);
            return savedMessage;
        });

        ChatMessage savedMessage = messageService.publishMessage(message);

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(outboxCaptor.capture());

        OutboxEvent outboxEvent = outboxCaptor.getValue();
        MessagePublishedEvent event = objectMapper.readValue(outboxEvent.getPayload(), MessagePublishedEvent.class);

        assertThat(savedMessage.getId()).isEqualTo(42L);
        assertThat(savedMessage.getSenderUsername()).isEqualTo("martin");
        assertThat(outboxEvent.getAggregateType()).isEqualTo("MESSAGE");
        assertThat(outboxEvent.getAggregateId()).isEqualTo(42L);
        assertThat(outboxEvent.getType()).isEqualTo("MESSAGE_PUBLISHED");
        assertThat(outboxEvent.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.PENDING);
        assertThat(event.messageId()).isEqualTo(42L);
        assertThat(event.senderUserId()).isEqualTo(1L);
        assertThat(event.senderUsername()).isEqualTo("martin");
        assertThat(event.content()).isEqualTo("Hej @bot");
    }

    @Test
    void publishMessageRejectsMissingSenderUserId() {
        ChatMessage message = new ChatMessage();
        message.setContent("Saknar avsandare");

        assertThatThrownBy(() -> messageService.publishMessage(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("senderUserId is required");

        verify(messageRepository, never()).save(any());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void publishMessageAllowsBotSystemUserWithoutGrpcLookup() throws Exception {
        ChatMessage message = new ChatMessage();
        message.setSenderUserId(0L);
        message.setSenderUsername("bot");
        message.setContent("Bot reply");
        message.setSourceEventId(UUID.randomUUID());

        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage savedMessage = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedMessage, "id", 99L);
            return savedMessage;
        });

        ChatMessage savedMessage = messageService.publishMessage(message);

        assertThat(savedMessage.getSenderUsername()).isEqualTo("bot");
        verifyNoInteractions(userProfileClient);
        verify(outboxRepository).save(any(OutboxEvent.class));
    }

    @Test
    void publishMessageReturnsExistingMessageForDuplicateSourceEventId() throws Exception {
        UUID sourceEventId = UUID.randomUUID();
        ChatMessage existingMessage = new ChatMessage();
        existingMessage.setSenderUserId(0L);
        existingMessage.setSenderUsername("bot");
        existingMessage.setContent("Existing bot reply");
        existingMessage.setSourceEventId(sourceEventId);
        ReflectionTestUtils.setField(existingMessage, "id", 123L);

        ChatMessage duplicateMessage = new ChatMessage();
        duplicateMessage.setSenderUserId(0L);
        duplicateMessage.setSenderUsername("bot");
        duplicateMessage.setContent("Duplicate bot reply");
        duplicateMessage.setSourceEventId(sourceEventId);

        when(messageRepository.findBySourceEventId(sourceEventId)).thenReturn(Optional.of(existingMessage));

        ChatMessage savedMessage = messageService.publishMessage(duplicateMessage);

        assertThat(savedMessage.getId()).isEqualTo(123L);
        assertThat(savedMessage.getContent()).isEqualTo("Existing bot reply");
        verify(messageRepository, never()).save(any());
        verify(outboxRepository, never()).save(any());
        verifyNoInteractions(userProfileClient);
    }
}

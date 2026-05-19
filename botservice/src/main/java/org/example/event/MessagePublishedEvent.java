package org.example.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessagePublishedEvent(
        UUID eventId,
        Long messageId,
        Long senderUserId,
        String senderUsername,
        String content,
        LocalDateTime publishedAt
) {
}

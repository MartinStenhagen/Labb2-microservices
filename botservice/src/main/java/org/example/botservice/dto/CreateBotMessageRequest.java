package org.example.botservice.dto;

import java.util.UUID;

public record CreateBotMessageRequest(
        Long senderUserId,
        String senderUsername,
        String room,
        String content,
        UUID sourceEventId
) {
}

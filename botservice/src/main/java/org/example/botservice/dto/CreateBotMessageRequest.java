package org.example.botservice.dto;

public record CreateBotMessageRequest(
        Long senderUserId,
        String senderUsername,
        String content
) {
}

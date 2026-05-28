package org.example.bff.dto;

public record CreateMessageRequest(
        String content,
        String room,
        String botPersonality
) {
}

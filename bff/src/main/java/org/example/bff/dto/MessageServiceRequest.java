package org.example.bff.dto;

public record MessageServiceRequest(
        Long senderUserId,
        String content
) {
}

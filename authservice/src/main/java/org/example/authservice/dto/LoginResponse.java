package org.example.authservice.dto;

import java.time.Instant;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Long userId,
        String username,
        Instant expiresAt
) {
}

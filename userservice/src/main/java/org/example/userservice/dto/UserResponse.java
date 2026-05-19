package org.example.userservice.dto;

public record UserResponse(
        Long id,
        String username,
        String displayName
) {
}

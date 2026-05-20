package org.example.userservice.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserRequest(
        @NotBlank String username,
        @NotBlank String displayName
) {
}

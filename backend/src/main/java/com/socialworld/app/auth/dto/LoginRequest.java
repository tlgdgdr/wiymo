package com.socialworld.app.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        /** Username or email. */
        @NotBlank String identifier,
        @NotBlank String password
) {
}

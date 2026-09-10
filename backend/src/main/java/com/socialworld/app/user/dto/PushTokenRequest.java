package com.socialworld.app.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PushTokenRequest(
        @NotBlank @Size(max = 100) String token
) {
}

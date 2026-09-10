package com.socialworld.app.user.dto;

import com.socialworld.app.intention.Intention;
import jakarta.validation.constraints.NotNull;

public record UpdateIntentionRequest(
        @NotNull Intention intention
) {
}

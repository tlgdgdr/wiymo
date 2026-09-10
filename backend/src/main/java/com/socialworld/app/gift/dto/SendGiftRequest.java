package com.socialworld.app.gift.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SendGiftRequest(
        @NotNull UUID receiverId,
        @NotNull UUID giftId
) {
}

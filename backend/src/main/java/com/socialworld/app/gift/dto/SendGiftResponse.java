package com.socialworld.app.gift.dto;

import com.socialworld.app.chat.dto.MessageResponse;

import java.util.UUID;

public record SendGiftResponse(
        UUID transactionId,
        UUID giftId,
        String giftName,
        int coinAmount,
        long newBalance,
        MessageResponse message
) {
}

package com.socialworld.app.gift.dto;

import com.socialworld.app.gift.Gift;
import com.socialworld.app.gift.GiftCategory;

import java.util.UUID;

public record GiftResponse(
        UUID id,
        String name,
        String iconUrl,
        String animationUrl,
        int coinPrice,
        GiftCategory category
) {
    public static GiftResponse from(Gift gift) {
        return new GiftResponse(
                gift.getId(),
                gift.getName(),
                gift.getIconUrl(),
                gift.getAnimationUrl(),
                gift.getCoinPrice(),
                gift.getCategory());
    }
}

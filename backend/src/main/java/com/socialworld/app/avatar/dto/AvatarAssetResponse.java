package com.socialworld.app.avatar.dto;

import com.socialworld.app.avatar.AvatarAsset;
import com.socialworld.app.avatar.AvatarCategory;

import java.util.UUID;

public record AvatarAssetResponse(
        UUID id,
        AvatarCategory category,
        String assetKey,
        String displayName,
        String imageUrl,
        boolean premium,
        int coinPrice,
        int sortOrder
) {
    public static AvatarAssetResponse from(AvatarAsset asset) {
        return new AvatarAssetResponse(
                asset.getId(),
                asset.getCategory(),
                asset.getAssetKey(),
                asset.getDisplayName(),
                asset.getImageUrl(),
                asset.isPremium(),
                asset.getCoinPrice(),
                asset.getSortOrder());
    }
}

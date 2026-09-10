package com.socialworld.app.avatar.dto;

import com.socialworld.app.avatar.Avatar;

/** Asset keys per layer, e.g. {"bodyId": "body_01", ...}. Null layers are unset. */
public record AvatarResponse(
        String bodyId,
        String faceId,
        String eyesId,
        String hairId,
        String topId,
        String bottomId,
        String shoesId,
        String accessoryId
) {
    public static AvatarResponse from(Avatar avatar) {
        return new AvatarResponse(
                avatar.getBodyKey(),
                avatar.getFaceKey(),
                avatar.getEyesKey(),
                avatar.getHairKey(),
                avatar.getTopKey(),
                avatar.getBottomKey(),
                avatar.getShoesKey(),
                avatar.getAccessoryKey());
    }
}

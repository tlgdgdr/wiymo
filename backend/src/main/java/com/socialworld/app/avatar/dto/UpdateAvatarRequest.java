package com.socialworld.app.avatar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Full replacement of the avatar. Body is required; a null layer clears it.
 * Values are asset keys from GET /api/avatar/assets.
 */
public record UpdateAvatarRequest(
        @NotBlank @Size(max = 50) String bodyId,
        @Size(max = 50) String faceId,
        @Size(max = 50) String eyesId,
        @Size(max = 50) String hairId,
        @Size(max = 50) String topId,
        @Size(max = 50) String bottomId,
        @Size(max = 50) String shoesId,
        @Size(max = 50) String accessoryId
) {
}

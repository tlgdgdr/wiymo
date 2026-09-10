package com.socialworld.app.room.dto;

import com.socialworld.app.avatar.dto.AvatarResponse;
import com.socialworld.app.intention.Intention;

import java.util.UUID;

/** An occupant of a room: enough to render the avatar at its slot. */
public record RoomUserResponse(
        UUID userId,
        String username,
        int slotIndex,
        Intention currentIntention,
        AvatarResponse avatar
) {
}

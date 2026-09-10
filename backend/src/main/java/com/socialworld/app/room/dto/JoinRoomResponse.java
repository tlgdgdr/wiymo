package com.socialworld.app.room.dto;

import java.util.UUID;

public record JoinRoomResponse(
        UUID roomId,
        int slotIndex
) {
}

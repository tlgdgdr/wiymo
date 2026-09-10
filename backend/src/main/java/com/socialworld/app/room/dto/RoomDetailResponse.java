package com.socialworld.app.room.dto;

import java.util.List;

public record RoomDetailResponse(
        RoomResponse room,
        List<RoomSlotResponse> slots
) {
}

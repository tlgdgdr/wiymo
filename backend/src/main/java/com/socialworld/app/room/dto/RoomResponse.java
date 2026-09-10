package com.socialworld.app.room.dto;

import com.socialworld.app.intention.Intention;
import com.socialworld.app.room.Room;
import com.socialworld.app.room.RoomTheme;

import java.util.UUID;

public record RoomResponse(
        UUID id,
        String name,
        String description,
        RoomTheme theme,
        Intention intention,
        String backgroundImageUrl,
        int maxUsers,
        long population
) {
    public static RoomResponse from(Room room, long population) {
        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getDescription(),
                room.getTheme(),
                room.getIntention(),
                room.getBackgroundImageUrl(),
                room.getMaxUsers(),
                population);
    }
}

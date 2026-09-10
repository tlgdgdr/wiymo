package com.socialworld.app.room.dto;

import com.socialworld.app.room.RoomSlot;

import java.math.BigDecimal;

public record RoomSlotResponse(
        int slotIndex,
        BigDecimal xPercent,
        BigDecimal yPercent,
        BigDecimal scale
) {
    public static RoomSlotResponse from(RoomSlot slot) {
        return new RoomSlotResponse(
                slot.getSlotIndex(),
                slot.getXPercent(),
                slot.getYPercent(),
                slot.getScale());
    }
}

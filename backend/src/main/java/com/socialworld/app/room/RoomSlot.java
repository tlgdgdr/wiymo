package com.socialworld.app.room;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A predefined avatar position inside a room, as percentages of the room
 * artwork so coordinates survive any screen size. Scale > 1 near the bottom
 * of the scene gives a cheap depth effect.
 */
@Entity
@Table(name = "room_slots")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomSlot {

    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "slot_index", nullable = false)
    private int slotIndex;

    @Column(name = "x_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal xPercent;

    @Column(name = "y_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal yPercent;

    @Column(nullable = false, precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal scale = BigDecimal.ONE;
}

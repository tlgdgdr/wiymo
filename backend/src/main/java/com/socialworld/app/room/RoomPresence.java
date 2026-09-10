package com.socialworld.app.room;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Who sits where. user_id is the primary key, so a user can only ever be in
 * one room — the invariant is enforced by the schema, not just the service.
 */
@Entity
@Table(name = "room_presence")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomPresence {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "slot_index", nullable = false)
    private int slotIndex;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @PrePersist
    void onCreate() {
        joinedAt = Instant.now();
    }
}

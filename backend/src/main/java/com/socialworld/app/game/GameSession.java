package com.socialworld.app.game;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * One game between two players. {@code state} is owned by the engine for
 * {@code gameType} and opaque everywhere else, so a new game is a new engine
 * rather than a new table.
 */
@Entity
@Table(name = "game_sessions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameSession {

    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Enumerated(EnumType.STRING)
    @Column(name = "game_type", nullable = false, length = 30)
    private GameType gameType;

    @Column(name = "room_id")
    private UUID roomId;

    /** Invited the other player, and always moves first. */
    @Column(name = "challenger_id", nullable = false)
    private UUID challengerId;

    @Column(name = "opponent_id", nullable = false)
    private UUID opponentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GameStatus status;

    @Column(name = "turn_user_id")
    private UUID turnUserId;

    /** Null on a draw as well as on an unfinished game — read {@code status}. */
    @Column(name = "winner_id")
    private UUID winnerId;

    @Column(nullable = false, length = 1000)
    private String state;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean hasPlayer(UUID userId) {
        return challengerId.equals(userId) || opponentId.equals(userId);
    }

    public UUID otherPlayer(UUID userId) {
        return challengerId.equals(userId) ? opponentId : challengerId;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}

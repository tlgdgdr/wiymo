package com.socialworld.app.game;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameSessionRepository extends JpaRepository<GameSession, UUID> {

    /**
     * Row-locked so two simultaneous moves cannot both read the same board and
     * write conflicting states.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM GameSession g WHERE g.id = :id")
    Optional<GameSession> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            SELECT g FROM GameSession g
            WHERE g.status IN (com.socialworld.app.game.GameStatus.PENDING,
                               com.socialworld.app.game.GameStatus.ACTIVE)
              AND (g.challengerId = :userId OR g.opponentId = :userId)
            ORDER BY g.createdAt DESC
            """)
    List<GameSession> findLiveGames(@Param("userId") UUID userId);
}

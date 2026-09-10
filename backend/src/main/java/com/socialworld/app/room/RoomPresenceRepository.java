package com.socialworld.app.room;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomPresenceRepository extends JpaRepository<RoomPresence, UUID> {

    List<RoomPresence> findByRoomIdOrderBySlotIndex(UUID roomId);

    Optional<RoomPresence> findByUserId(UUID userId);

    long countByRoomId(UUID roomId);
}

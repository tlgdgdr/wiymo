package com.socialworld.app.room;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RoomSlotRepository extends JpaRepository<RoomSlot, UUID> {

    List<RoomSlot> findByRoomIdOrderBySlotIndex(UUID roomId);

    int countByRoomId(UUID roomId);
}

package com.socialworld.app.room;

import com.socialworld.app.intention.Intention;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID> {

    List<Room> findByActiveTrueOrderByName();

    List<Room> findByActiveTrueAndIntentionOrderByName(Intention intention);

    Optional<Room> findByIdAndActiveTrue(UUID id);
}

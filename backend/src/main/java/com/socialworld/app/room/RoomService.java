package com.socialworld.app.room;

import com.socialworld.app.avatar.AvatarService;
import com.socialworld.app.common.exception.ApiException;
import com.socialworld.app.common.exception.ErrorCode;
import com.socialworld.app.intention.Intention;
import com.socialworld.app.moderation.BlockService;
import com.socialworld.app.room.dto.JoinRoomResponse;
import com.socialworld.app.room.dto.RoomDetailResponse;
import com.socialworld.app.room.dto.RoomResponse;
import com.socialworld.app.room.dto.RoomSlotResponse;
import com.socialworld.app.room.dto.RoomUserResponse;
import com.socialworld.app.user.User;
import com.socialworld.app.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomSlotRepository roomSlotRepository;
    private final RoomPresenceRepository roomPresenceRepository;
    private final UserRepository userRepository;
    private final AvatarService avatarService;
    private final BlockService blockService;

    @Transactional(readOnly = true)
    public List<RoomResponse> listRooms(Intention intention) {
        List<Room> rooms = intention == null
                ? roomRepository.findByActiveTrueOrderByName()
                : roomRepository.findByActiveTrueAndIntentionOrderByName(intention);
        return rooms.stream()
                .map(room -> RoomResponse.from(room, roomPresenceRepository.countByRoomId(room.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public RoomDetailResponse getRoom(UUID roomId) {
        Room room = requireRoom(roomId);
        List<RoomSlotResponse> slots = roomSlotRepository.findByRoomIdOrderBySlotIndex(roomId).stream()
                .map(RoomSlotResponse::from)
                .toList();
        return new RoomDetailResponse(
                RoomResponse.from(room, roomPresenceRepository.countByRoomId(roomId)), slots);
    }

    @Transactional
    public JoinRoomResponse join(UUID userId, UUID roomId) {
        Room room = requireRoom(roomId);

        RoomPresence existing = roomPresenceRepository.findByUserId(userId).orElse(null);
        if (existing != null) {
            if (existing.getRoomId().equals(roomId)) {
                return new JoinRoomResponse(roomId, existing.getSlotIndex());
            }
            // One room at a time: switching rooms leaves the old one.
            roomPresenceRepository.delete(existing);
            roomPresenceRepository.flush();
        }

        List<RoomPresence> occupants = roomPresenceRepository.findByRoomIdOrderBySlotIndex(roomId);
        int slotCount = roomSlotRepository.countByRoomId(roomId);
        int capacity = Math.min(room.getMaxUsers(), slotCount);
        if (occupants.size() >= capacity) {
            throw new ApiException(ErrorCode.ROOM_FULL);
        }

        // Deterministic seating: lowest free slot, so every client renders
        // the same layout without coordination.
        Set<Integer> taken = occupants.stream()
                .map(RoomPresence::getSlotIndex)
                .collect(Collectors.toSet());
        int slotIndex = 0;
        while (taken.contains(slotIndex)) {
            slotIndex++;
        }

        roomPresenceRepository.save(RoomPresence.builder()
                .userId(userId)
                .roomId(roomId)
                .slotIndex(slotIndex)
                .build());

        userRepository.findById(userId).ifPresent(user -> {
            user.setOnline(true);
            user.setLastSeenAt(Instant.now());
        });

        return new JoinRoomResponse(roomId, slotIndex);
    }

    @Transactional
    public void leave(UUID userId, UUID roomId) {
        RoomPresence presence = roomPresenceRepository.findByUserId(userId)
                .filter(p -> p.getRoomId().equals(roomId))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_IN_ROOM));
        roomPresenceRepository.delete(presence);

        userRepository.findById(userId).ifPresent(user -> user.setLastSeenAt(Instant.now()));
    }

    @Transactional(readOnly = true)
    public List<RoomUserResponse> listRoomUsers(UUID viewerId, UUID roomId) {
        requireRoom(roomId);
        Set<UUID> blocked = blockService.blockedPartnerIds(viewerId);
        List<RoomPresence> presences = roomPresenceRepository.findByRoomIdOrderBySlotIndex(roomId);
        if (presences.isEmpty()) {
            return List.of();
        }
        Map<UUID, User> users = userRepository
                .findAllById(presences.stream().map(RoomPresence::getUserId).toList()).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return presences.stream()
                .map(presence -> {
                    User user = users.get(presence.getUserId());
                    if (user == null || blocked.contains(presence.getUserId())) {
                        return null;
                    }
                    return new RoomUserResponse(
                            user.getId(),
                            user.getUsername(),
                            presence.getSlotIndex(),
                            user.getCurrentIntention(),
                            avatarService.getForUser(user.getId()));
                })
                .filter(u -> u != null)
                .toList();
    }

    private Room requireRoom(UUID roomId) {
        return roomRepository.findByIdAndActiveTrue(roomId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Room not found."));
    }
}

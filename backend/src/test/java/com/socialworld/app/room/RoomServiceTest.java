package com.socialworld.app.room;

import com.socialworld.app.avatar.AvatarService;
import com.socialworld.app.common.exception.ApiException;
import com.socialworld.app.common.exception.ErrorCode;
import com.socialworld.app.room.dto.JoinRoomResponse;
import com.socialworld.app.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;
    @Mock
    private RoomSlotRepository roomSlotRepository;
    @Mock
    private RoomPresenceRepository roomPresenceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AvatarService avatarService;

    @InjectMocks
    private RoomService roomService;

    private final UUID userId = UUID.randomUUID();
    private Room room;

    @BeforeEach
    void setUp() {
        room = Room.builder()
                .name("Midnight Cafe")
                .theme(RoomTheme.NIGHT)
                .backgroundImageUrl("https://example.com/bg.png")
                .maxUsers(3)
                .build();
        lenient().when(roomRepository.findByIdAndActiveTrue(room.getId())).thenReturn(Optional.of(room));
        lenient().when(userRepository.findById(userId)).thenReturn(Optional.empty());
    }

    private RoomPresence presence(UUID uid, UUID roomId, int slot) {
        return RoomPresence.builder().userId(uid).roomId(roomId).slotIndex(slot).build();
    }

    @Test
    void join_assignsLowestFreeSlot() {
        when(roomPresenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
        // slots 0 and 2 taken -> expect slot 1
        when(roomPresenceRepository.findByRoomIdOrderBySlotIndex(room.getId())).thenReturn(List.of(
                presence(UUID.randomUUID(), room.getId(), 0),
                presence(UUID.randomUUID(), room.getId(), 2)));
        when(roomSlotRepository.countByRoomId(room.getId())).thenReturn(12);

        JoinRoomResponse response = roomService.join(userId, room.getId());

        assertThat(response.slotIndex()).isEqualTo(1);
    }

    @Test
    void join_rejectsWhenRoomFull() {
        when(roomPresenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(roomPresenceRepository.findByRoomIdOrderBySlotIndex(room.getId())).thenReturn(
                IntStream.range(0, 3)
                        .mapToObj(i -> presence(UUID.randomUUID(), room.getId(), i))
                        .toList());
        when(roomSlotRepository.countByRoomId(room.getId())).thenReturn(12);

        // maxUsers is 3 -> full
        assertThatThrownBy(() -> roomService.join(userId, room.getId()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.ROOM_FULL);
        verify(roomPresenceRepository, never()).save(any());
    }

    @Test
    void join_capacityIsLimitedBySlotCountToo() {
        when(roomPresenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(roomPresenceRepository.findByRoomIdOrderBySlotIndex(room.getId())).thenReturn(List.of(
                presence(UUID.randomUUID(), room.getId(), 0),
                presence(UUID.randomUUID(), room.getId(), 1)));
        when(roomSlotRepository.countByRoomId(room.getId())).thenReturn(2);

        // maxUsers 3 but only 2 physical slots -> full
        assertThatThrownBy(() -> roomService.join(userId, room.getId()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.ROOM_FULL);
    }

    @Test
    void join_switchingRoomsLeavesOldRoom() {
        RoomPresence oldPresence = presence(userId, UUID.randomUUID(), 4);
        when(roomPresenceRepository.findByUserId(userId)).thenReturn(Optional.of(oldPresence));
        when(roomPresenceRepository.findByRoomIdOrderBySlotIndex(room.getId())).thenReturn(List.of());
        when(roomSlotRepository.countByRoomId(room.getId())).thenReturn(12);

        JoinRoomResponse response = roomService.join(userId, room.getId());

        verify(roomPresenceRepository).delete(oldPresence);
        assertThat(response.slotIndex()).isEqualTo(0);
    }

    @Test
    void join_isIdempotentWithinSameRoom() {
        when(roomPresenceRepository.findByUserId(userId))
                .thenReturn(Optional.of(presence(userId, room.getId(), 5)));

        JoinRoomResponse response = roomService.join(userId, room.getId());

        assertThat(response.slotIndex()).isEqualTo(5);
        verify(roomPresenceRepository, never()).save(any());
        verify(roomPresenceRepository, never()).delete(any());
    }

    @Test
    void leave_removesPresence() {
        RoomPresence presence = presence(userId, room.getId(), 2);
        when(roomPresenceRepository.findByUserId(userId)).thenReturn(Optional.of(presence));

        roomService.leave(userId, room.getId());

        verify(roomPresenceRepository).delete(presence);
    }

    @Test
    void leave_rejectsWhenNotInThatRoom() {
        when(roomPresenceRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.leave(userId, room.getId()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.NOT_IN_ROOM);
    }

    @Test
    void getRoom_unknownRoomIsNotFound() {
        UUID missing = UUID.randomUUID();
        when(roomRepository.findByIdAndActiveTrue(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.getRoom(missing))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }
}

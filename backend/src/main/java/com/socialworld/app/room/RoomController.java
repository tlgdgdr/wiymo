package com.socialworld.app.room;

import com.socialworld.app.auth.AuthenticatedUser;
import com.socialworld.app.intention.Intention;
import com.socialworld.app.room.dto.JoinRoomResponse;
import com.socialworld.app.room.dto.RoomDetailResponse;
import com.socialworld.app.room.dto.RoomResponse;
import com.socialworld.app.room.dto.RoomUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Tag(name = "Rooms")
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    @Operation(summary = "Active rooms, optionally filtered by intention")
    public List<RoomResponse> listRooms(@RequestParam(required = false) Intention intention) {
        return roomService.listRooms(intention);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Room details including seat slots")
    public RoomDetailResponse getRoom(@PathVariable UUID id) {
        return roomService.getRoom(id);
    }

    @PostMapping("/{id}/join")
    @Operation(summary = "Join a room (leaves any previous room; assigns lowest free slot)")
    public JoinRoomResponse join(@AuthenticationPrincipal AuthenticatedUser user,
                                 @PathVariable UUID id) {
        return roomService.join(user.id(), id);
    }

    @PostMapping("/{id}/leave")
    @Operation(summary = "Leave the room")
    public ResponseEntity<Void> leave(@AuthenticationPrincipal AuthenticatedUser user,
                                      @PathVariable UUID id) {
        roomService.leave(user.id(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/users")
    @Operation(summary = "Current occupants with their slots and avatars")
    public List<RoomUserResponse> roomUsers(@AuthenticationPrincipal AuthenticatedUser user,
                                            @PathVariable UUID id) {
        return roomService.listRoomUsers(user.id(), id);
    }
}

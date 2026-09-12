package com.socialworld.app.game;

import com.socialworld.app.auth.AuthenticatedUser;
import com.socialworld.app.game.dto.GameStateResponse;
import com.socialworld.app.game.dto.InviteGameRequest;
import com.socialworld.app.game.dto.MoveRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST is the authority for every game action; the WebSocket only carries the
 * resulting state out, plus a shortcut for moves so play feels instant.
 */
@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
@Tag(name = "Games")
public class GameController {

    private final GameService gameService;

    @GetMapping
    @Operation(summary = "My pending invites and games in progress")
    public List<GameStateResponse> myGames(@AuthenticationPrincipal AuthenticatedUser me) {
        return gameService.myLiveGames(me.id());
    }

    @PostMapping
    @Operation(summary = "Invite another user to a game")
    public GameStateResponse invite(@AuthenticationPrincipal AuthenticatedUser me,
                                    @Valid @RequestBody InviteGameRequest request) {
        return gameService.invite(me.id(), request.opponentId(), request.gameType(), request.roomId());
    }

    @PostMapping("/{id}/accept")
    @Operation(summary = "Accept an invite")
    public GameStateResponse accept(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable UUID id) {
        return gameService.respondToInvite(me.id(), id, true);
    }

    @PostMapping("/{id}/decline")
    @Operation(summary = "Decline an invite")
    public GameStateResponse decline(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable UUID id) {
        return gameService.respondToInvite(me.id(), id, false);
    }

    @PostMapping("/{id}/moves")
    @Operation(summary = "Play a move")
    public GameStateResponse play(@AuthenticationPrincipal AuthenticatedUser me,
                                  @PathVariable UUID id,
                                  @Valid @RequestBody MoveRequest request) {
        return gameService.play(me.id(), id, request.cell());
    }

    @PostMapping("/{id}/forfeit")
    @Operation(summary = "Quit a game (or cancel an invite you sent)")
    public GameStateResponse forfeit(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable UUID id) {
        return gameService.forfeit(me.id(), id);
    }
}

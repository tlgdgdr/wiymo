package com.socialworld.app.game.dto;

import com.socialworld.app.game.GameType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InviteGameRequest(
        @NotNull UUID opponentId,
        @NotNull GameType gameType,
        /** The room the invite was sent from, for context only. */
        UUID roomId) {
}

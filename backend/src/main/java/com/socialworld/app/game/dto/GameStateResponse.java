package com.socialworld.app.game.dto;

import com.socialworld.app.game.GameSession;
import com.socialworld.app.game.GameStatus;
import com.socialworld.app.game.GameType;

import java.util.UUID;

/**
 * The whole game as the client needs it. Both players get the identical
 * payload — the client works out "my turn" by comparing ids, so the server
 * never has to render two views of one board.
 */
public record GameStateResponse(
        UUID id,
        GameType gameType,
        GameStatus status,
        UUID challengerId,
        String challengerUsername,
        UUID opponentId,
        String opponentUsername,
        UUID turnUserId,
        UUID winnerId,
        boolean draw,
        String board) {

    public static GameStateResponse from(GameSession game, String challengerUsername, String opponentUsername) {
        return new GameStateResponse(
                game.getId(),
                game.getGameType(),
                game.getStatus(),
                game.getChallengerId(),
                challengerUsername,
                game.getOpponentId(),
                opponentUsername,
                game.getTurnUserId(),
                game.getWinnerId(),
                game.getStatus() == GameStatus.FINISHED && game.getWinnerId() == null,
                game.getState());
    }
}

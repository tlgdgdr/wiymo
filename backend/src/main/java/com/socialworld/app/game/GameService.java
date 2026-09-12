package com.socialworld.app.game;

import com.socialworld.app.chat.ChatService;
import com.socialworld.app.chat.ws.ChatSessionRegistry;
import com.socialworld.app.common.exception.ApiException;
import com.socialworld.app.common.exception.ErrorCode;
import com.socialworld.app.game.dto.GameStateResponse;
import com.socialworld.app.moderation.BlockService;
import com.socialworld.app.user.User;
import com.socialworld.app.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Server-authoritative game play. Clients send intents ("I play cell 4") and
 * never state: every rule, turn check and win condition is decided here, so a
 * tampered client can only ever get an error back.
 */
@Service
@RequiredArgsConstructor
public class GameService {

    private final GameSessionRepository gameSessionRepository;
    private final UserRepository userRepository;
    private final BlockService blockService;
    private final ChatSessionRegistry sessionRegistry;

    /** Invite someone to a game. The challenger always moves first. */
    @Transactional
    public GameStateResponse invite(UUID challengerId, UUID opponentId, GameType gameType, UUID roomId) {
        if (challengerId.equals(opponentId)) {
            throw new ApiException(ErrorCode.SELF_ACTION_NOT_ALLOWED, "You cannot play against yourself.");
        }
        blockService.assertInteractionAllowed(challengerId, opponentId);
        userRepository.findById(opponentId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found."));

        requireNoLiveGame(challengerId, "You already have a game in progress.");
        requireNoLiveGame(opponentId, "That player is already in a game.");

        GameSession game = gameSessionRepository.save(GameSession.builder()
                .gameType(gameType)
                .roomId(roomId)
                .challengerId(challengerId)
                .opponentId(opponentId)
                .status(GameStatus.PENDING)
                .turnUserId(challengerId)
                .state(TicTacToe.emptyBoard())
                .build());

        return publish(game);
    }

    @Transactional
    public GameStateResponse respondToInvite(UUID userId, UUID gameId, boolean accept) {
        GameSession game = requireLockedGame(gameId, userId);
        if (game.getStatus() != GameStatus.PENDING) {
            throw new ApiException(ErrorCode.GAME_NOT_PLAYABLE, "This invite is no longer open.");
        }
        // Only the invited player answers the invite; the challenger cancels
        // by forfeiting instead.
        if (!game.getOpponentId().equals(userId)) {
            throw new ApiException(ErrorCode.GAME_NOT_PLAYABLE, "This invite is not yours to answer.");
        }
        game.setStatus(accept ? GameStatus.ACTIVE : GameStatus.DECLINED);
        return publish(game);
    }

    @Transactional
    public GameStateResponse play(UUID userId, UUID gameId, int cell) {
        GameSession game = requireLockedGame(gameId, userId);
        if (game.getStatus() != GameStatus.ACTIVE) {
            throw new ApiException(ErrorCode.GAME_NOT_PLAYABLE, "This game is not in play.");
        }
        if (!userId.equals(game.getTurnUserId())) {
            throw new ApiException(ErrorCode.NOT_YOUR_TURN);
        }
        if (!TicTacToe.canPlay(game.getState(), cell)) {
            throw new ApiException(ErrorCode.INVALID_MOVE);
        }

        char mark = game.getChallengerId().equals(userId) ? TicTacToe.CHALLENGER : TicTacToe.OPPONENT;
        game.setState(TicTacToe.play(game.getState(), cell, mark));

        char winningMark = TicTacToe.winner(game.getState());
        if (winningMark != TicTacToe.EMPTY) {
            finish(game, userId);
        } else if (TicTacToe.isFull(game.getState())) {
            finish(game, null); // draw
        } else {
            game.setTurnUserId(game.otherPlayer(userId));
        }
        return publish(game);
    }

    /** Quitting hands the win to the other player, whether deliberate or a dropped connection. */
    @Transactional
    public GameStateResponse forfeit(UUID userId, UUID gameId) {
        GameSession game = requireLockedGame(gameId, userId);
        if (!game.getStatus().isLive()) {
            return publish(game);
        }
        // Walking away from an unanswered invite is a cancellation, not a loss.
        game.setWinnerId(game.getStatus() == GameStatus.PENDING ? null : game.otherPlayer(userId));
        game.setStatus(GameStatus.ABANDONED);
        game.setTurnUserId(null);
        return publish(game);
    }

    /**
     * Ends every live game a user is in. Called when their socket drops, so a
     * player who closes the app never leaves an opponent waiting on a turn
     * that will not come.
     */
    @Transactional
    public void forfeitAll(UUID userId) {
        for (GameSession game : gameSessionRepository.findLiveGames(userId)) {
            forfeit(userId, game.getId());
        }
    }

    @Transactional(readOnly = true)
    public List<GameStateResponse> myLiveGames(UUID userId) {
        return gameSessionRepository.findLiveGames(userId).stream().map(this::toResponse).toList();
    }

    private void finish(GameSession game, UUID winnerId) {
        game.setStatus(GameStatus.FINISHED);
        game.setWinnerId(winnerId);
        game.setTurnUserId(null);
    }

    private void requireNoLiveGame(UUID userId, String message) {
        if (!gameSessionRepository.findLiveGames(userId).isEmpty()) {
            throw new ApiException(ErrorCode.GAME_ALREADY_IN_PROGRESS, message);
        }
    }

    private GameSession requireLockedGame(UUID gameId, UUID userId) {
        GameSession game = gameSessionRepository.findByIdForUpdate(gameId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Game not found."));
        // Same 404 for "not yours" as for "does not exist": game ids should not
        // be probeable.
        if (!game.hasPlayer(userId)) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Game not found.");
        }
        return game;
    }

    /** Save, then push the identical state to both players. */
    private GameStateResponse publish(GameSession game) {
        GameSession saved = gameSessionRepository.saveAndFlush(game);
        GameStateResponse response = toResponse(saved);
        ChatService.WsEvent<GameStateResponse> event = new ChatService.WsEvent<>("game.state", response);
        sessionRegistry.sendToUser(saved.getChallengerId(), event);
        sessionRegistry.sendToUser(saved.getOpponentId(), event);
        return response;
    }

    private GameStateResponse toResponse(GameSession game) {
        return GameStateResponse.from(game, username(game.getChallengerId()), username(game.getOpponentId()));
    }

    private String username(UUID userId) {
        return userRepository.findById(userId).map(User::getUsername).orElse("Unknown");
    }
}

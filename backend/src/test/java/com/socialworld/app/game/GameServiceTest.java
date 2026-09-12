package com.socialworld.app.game;

import com.socialworld.app.chat.ws.ChatSessionRegistry;
import com.socialworld.app.common.exception.ApiException;
import com.socialworld.app.common.exception.ErrorCode;
import com.socialworld.app.game.dto.GameStateResponse;
import com.socialworld.app.moderation.BlockService;
import com.socialworld.app.user.User;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameSessionRepository gameSessionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BlockService blockService;
    @Mock
    private ChatSessionRegistry sessionRegistry;

    @InjectMocks
    private GameService gameService;

    private final UUID challenger = UUID.randomUUID();
    private final UUID opponent = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(userRepository.findById(any())).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            User user = new User();
            user.setId(id);
            user.setUsername(id.equals(challenger) ? "mira" : "leo");
            return Optional.of(user);
        });
        lenient().when(gameSessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(gameSessionRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(gameSessionRepository.findLiveGames(any())).thenReturn(List.of());
    }

    private GameSession activeGame(String board, UUID turn) {
        GameSession game = GameSession.builder()
                .gameType(GameType.TIC_TAC_TOE)
                .challengerId(challenger)
                .opponentId(opponent)
                .status(GameStatus.ACTIVE)
                .turnUserId(turn)
                .state(board)
                .build();
        when(gameSessionRepository.findByIdForUpdate(game.getId())).thenReturn(Optional.of(game));
        return game;
    }

    @Test
    void inviteStartsPendingWithTheChallengerToMove() {
        GameStateResponse response = gameService.invite(challenger, opponent, GameType.TIC_TAC_TOE, null);

        assertThat(response.status()).isEqualTo(GameStatus.PENDING);
        assertThat(response.turnUserId()).isEqualTo(challenger);
        assertThat(response.board()).isEqualTo(".........");
        verify(blockService).assertInteractionAllowed(challenger, opponent);
        // Both players see the invite live.
        verify(sessionRegistry).sendToUser(eq(challenger), any());
        verify(sessionRegistry).sendToUser(eq(opponent), any());
    }

    @Test
    void cannotInviteYourself() {
        assertThatThrownBy(() -> gameService.invite(challenger, challenger, GameType.TIC_TAC_TOE, null))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo(ErrorCode.SELF_ACTION_NOT_ALLOWED);
    }

    @Test
    void cannotInviteWhileEitherPlayerIsAlreadyPlaying() {
        when(gameSessionRepository.findLiveGames(challenger))
                .thenReturn(List.of(GameSession.builder().build()));

        assertThatThrownBy(() -> gameService.invite(challenger, opponent, GameType.TIC_TAC_TOE, null))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo(ErrorCode.GAME_ALREADY_IN_PROGRESS);
    }

    @Test
    void onlyTheInvitedPlayerAnswersTheInvite() {
        GameSession game = activeGame(".........", challenger);
        game.setStatus(GameStatus.PENDING);

        assertThatThrownBy(() -> gameService.respondToInvite(challenger, game.getId(), true))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo(ErrorCode.GAME_NOT_PLAYABLE);

        assertThat(gameService.respondToInvite(opponent, game.getId(), true).status())
                .isEqualTo(GameStatus.ACTIVE);
    }

    @Test
    void playingOutOfTurnIsRejected() {
        GameSession game = activeGame(".........", challenger);

        assertThatThrownBy(() -> gameService.play(opponent, game.getId(), 0))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo(ErrorCode.NOT_YOUR_TURN);
        assertThat(game.getState()).isEqualTo(".........");
    }

    @Test
    void playingAnOccupiedOrOutOfRangeCellIsRejected() {
        GameSession game = activeGame("X........", challenger);

        assertThatThrownBy(() -> gameService.play(challenger, game.getId(), 0))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo(ErrorCode.INVALID_MOVE);
        assertThatThrownBy(() -> gameService.play(challenger, game.getId(), 99))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo(ErrorCode.INVALID_MOVE);
    }

    @Test
    void aLegalMovePassesTheTurn() {
        GameSession game = activeGame(".........", challenger);

        GameStateResponse response = gameService.play(challenger, game.getId(), 4);

        assertThat(response.board()).isEqualTo("....X....");
        assertThat(response.turnUserId()).isEqualTo(opponent);
        assertThat(response.status()).isEqualTo(GameStatus.ACTIVE);
    }

    @Test
    void completingALineWinsTheGame() {
        GameSession game = activeGame("XX.OO....", challenger);

        GameStateResponse response = gameService.play(challenger, game.getId(), 2);

        assertThat(response.status()).isEqualTo(GameStatus.FINISHED);
        assertThat(response.winnerId()).isEqualTo(challenger);
        assertThat(response.draw()).isFalse();
        assertThat(response.turnUserId()).isNull();
    }

    @Test
    void fillingTheLastCellWithoutALineIsADraw() {
        GameSession game = activeGame("XXOOOXXO.", opponent);

        GameStateResponse response = gameService.play(opponent, game.getId(), 8);

        assertThat(response.status()).isEqualTo(GameStatus.FINISHED);
        assertThat(response.winnerId()).isNull();
        assertThat(response.draw()).isTrue();
    }

    @Test
    void aFinishedGameCannotBePlayedOn() {
        GameSession game = activeGame("XXX......", challenger);
        game.setStatus(GameStatus.FINISHED);

        assertThatThrownBy(() -> gameService.play(challenger, game.getId(), 3))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo(ErrorCode.GAME_NOT_PLAYABLE);
    }

    @Test
    void quittingMidGameHandsTheWinToTheOtherPlayer() {
        GameSession game = activeGame("X........", opponent);

        GameStateResponse response = gameService.forfeit(challenger, game.getId());

        assertThat(response.status()).isEqualTo(GameStatus.ABANDONED);
        assertThat(response.winnerId()).isEqualTo(opponent);
    }

    @Test
    void walkingAwayFromAnUnansweredInviteIsACancellationNotALoss() {
        GameSession game = activeGame(".........", challenger);
        game.setStatus(GameStatus.PENDING);

        GameStateResponse response = gameService.forfeit(challenger, game.getId());

        assertThat(response.status()).isEqualTo(GameStatus.ABANDONED);
        assertThat(response.winnerId()).isNull();
    }

    @Test
    void aDroppedConnectionEndsEveryLiveGame() {
        GameSession game = activeGame("X........", opponent);
        when(gameSessionRepository.findLiveGames(challenger)).thenReturn(List.of(game));

        gameService.forfeitAll(challenger);

        assertThat(game.getStatus()).isEqualTo(GameStatus.ABANDONED);
        assertThat(game.getWinnerId()).isEqualTo(opponent);
    }

    @Test
    void gamesYouAreNotInLookLikeTheyDoNotExist() {
        GameSession game = activeGame(".........", challenger);
        UUID stranger = UUID.randomUUID();

        assertThatThrownBy(() -> gameService.play(stranger, game.getId(), 0))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void aSecondForfeitIsANoOpRatherThanAnError() {
        GameSession game = activeGame("X........", opponent);
        gameService.forfeit(challenger, game.getId());

        GameStateResponse response = gameService.forfeit(challenger, game.getId());

        assertThat(response.status()).isEqualTo(GameStatus.ABANDONED);
        assertThat(response.winnerId()).isEqualTo(opponent);
        verify(sessionRegistry, times(4)).sendToUser(any(), any());
    }
}

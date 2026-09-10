package com.socialworld.app.moderation;

import com.socialworld.app.common.exception.ApiException;
import com.socialworld.app.common.exception.ErrorCode;
import com.socialworld.app.user.User;
import com.socialworld.app.user.UserRepository;
import com.socialworld.app.user.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlockServiceTest {

    @Mock
    private UserBlockRepository userBlockRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BlockService blockService;

    private final UUID blockerId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();

    private void targetExists() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(User.builder()
                .username("bob").email("b@example.com").passwordHash("h")
                .birthDate(LocalDate.now().minusYears(25)).status(UserStatus.ACTIVE).build()));
    }

    @Test
    void block_savesOnce() {
        targetExists();
        when(userBlockRepository.findByBlockerIdAndBlockedUserId(blockerId, targetId))
                .thenReturn(Optional.empty());

        blockService.block(blockerId, targetId);

        verify(userBlockRepository, times(1)).save(any(UserBlock.class));
    }

    @Test
    void block_isIdempotent() {
        targetExists();
        when(userBlockRepository.findByBlockerIdAndBlockedUserId(blockerId, targetId))
                .thenReturn(Optional.of(UserBlock.builder()
                        .blockerId(blockerId).blockedUserId(targetId).build()));

        blockService.block(blockerId, targetId);

        verify(userBlockRepository, never()).save(any());
    }

    @Test
    void block_rejectsSelf() {
        assertThatThrownBy(() -> blockService.block(blockerId, blockerId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.SELF_ACTION_NOT_ALLOWED);
    }

    @Test
    void unblock_removesOwnBlockOnly() {
        UserBlock block = UserBlock.builder().blockerId(blockerId).blockedUserId(targetId).build();
        when(userBlockRepository.findByBlockerIdAndBlockedUserId(blockerId, targetId))
                .thenReturn(Optional.of(block));

        blockService.unblock(blockerId, targetId);

        verify(userBlockRepository).delete(block);
    }

    @Test
    void assertInteractionAllowed_throwsNeutralErrorWhenBlockedEitherWay() {
        when(userBlockRepository.existsBetween(blockerId, targetId)).thenReturn(true);

        assertThatThrownBy(() -> blockService.assertInteractionAllowed(blockerId, targetId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.USER_UNAVAILABLE);
    }

    @Test
    void assertInteractionAllowed_passesWhenNoBlock() {
        when(userBlockRepository.existsBetween(blockerId, targetId)).thenReturn(false);
        assertThatCode(() -> blockService.assertInteractionAllowed(blockerId, targetId))
                .doesNotThrowAnyException();
    }
}

package com.socialworld.app.friendship;

import com.socialworld.app.avatar.AvatarService;
import com.socialworld.app.common.exception.ApiException;
import com.socialworld.app.common.exception.ErrorCode;
import com.socialworld.app.friendship.dto.ConnectionResponse;
import com.socialworld.app.user.User;
import com.socialworld.app.user.UserRepository;
import com.socialworld.app.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConnectionServiceTest {

    @Mock
    private ConnectionRepository connectionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AvatarService avatarService;
    @Mock
    private com.socialworld.app.moderation.BlockService blockService;

    @InjectMocks
    private ConnectionService connectionService;

    private final UUID requesterId = UUID.randomUUID();
    private User addressee;

    @BeforeEach
    void setUp() {
        addressee = User.builder()
                .username("bob")
                .email("bob@example.com")
                .passwordHash("hash")
                .birthDate(LocalDate.now().minusYears(25))
                .status(UserStatus.ACTIVE)
                .build();
        lenient().when(userRepository.findById(addressee.getId())).thenReturn(Optional.of(addressee));
    }

    @Test
    void request_createsPendingConnection() {
        when(connectionRepository.findActivePair(requesterId, addressee.getId()))
                .thenReturn(Optional.empty());
        when(connectionRepository.save(any(Connection.class))).thenAnswer(inv -> inv.getArgument(0));

        ConnectionResponse response = connectionService.request(requesterId, addressee.getId());

        assertThat(response.status()).isEqualTo(ConnectionStatus.PENDING);
        assertThat(response.partnerUsername()).isEqualTo("bob");
        assertThat(response.incoming()).isFalse();
    }

    @Test
    void request_rejectsSelf() {
        assertThatThrownBy(() -> connectionService.request(requesterId, requesterId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.SELF_ACTION_NOT_ALLOWED);
    }

    @Test
    void request_rejectsDuplicateInEitherDirection() {
        when(connectionRepository.findActivePair(requesterId, addressee.getId()))
                .thenReturn(Optional.of(Connection.builder()
                        .requesterId(addressee.getId())
                        .addresseeId(requesterId)
                        .status(ConnectionStatus.PENDING)
                        .build()));

        assertThatThrownBy(() -> connectionService.request(requesterId, addressee.getId()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.CONNECTION_ALREADY_EXISTS);
        verify(connectionRepository, never()).save(any());
    }

    @Test
    void accept_onlyAddresseeOfPendingMayAccept() {
        Connection pending = Connection.builder()
                .requesterId(requesterId)
                .addresseeId(addressee.getId())
                .status(ConnectionStatus.PENDING)
                .build();
        when(connectionRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
        when(userRepository.findById(requesterId)).thenReturn(Optional.of(addressee));

        // Requester cannot accept their own request.
        assertThatThrownBy(() -> connectionService.accept(requesterId, pending.getId()))
                .isInstanceOf(ApiException.class);

        ConnectionResponse response = connectionService.accept(addressee.getId(), pending.getId());
        assertThat(response.status()).isEqualTo(ConnectionStatus.ACCEPTED);
        assertThat(pending.getStatus()).isEqualTo(ConnectionStatus.ACCEPTED);
    }

    @Test
    void reject_marksRejectedAndAllowsNothingFurther() {
        Connection pending = Connection.builder()
                .requesterId(requesterId)
                .addresseeId(addressee.getId())
                .status(ConnectionStatus.PENDING)
                .build();
        when(connectionRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
        when(userRepository.findById(requesterId)).thenReturn(Optional.of(addressee));

        connectionService.reject(addressee.getId(), pending.getId());
        assertThat(pending.getStatus()).isEqualTo(ConnectionStatus.REJECTED);

        // Already decided: accepting now fails.
        assertThatThrownBy(() -> connectionService.accept(addressee.getId(), pending.getId()))
                .isInstanceOf(ApiException.class);
    }
}

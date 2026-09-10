package com.socialworld.app.auth;

import com.socialworld.app.auth.dto.AuthResponse;
import com.socialworld.app.auth.dto.LoginRequest;
import com.socialworld.app.auth.dto.RefreshRequest;
import com.socialworld.app.auth.dto.RegisterRequest;
import com.socialworld.app.common.exception.ApiException;
import com.socialworld.app.common.exception.ErrorCode;
import com.socialworld.app.config.JwtProperties;
import com.socialworld.app.user.User;
import com.socialworld.app.user.UserRepository;
import com.socialworld.app.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private AuthService authService;

    private final JwtProperties jwtProperties = new JwtProperties(
            "test-secret-test-secret-test-secret-test-secret",
            Duration.ofMinutes(15),
            Duration.ofDays(30));

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                passwordEncoder,
                new JwtService(jwtProperties),
                jwtProperties);
    }

    private RegisterRequest registerRequest(LocalDate birthDate) {
        return new RegisterRequest("alice", "alice@example.com", "password123", birthDate, "TR");
    }

    @Test
    void register_succeedsForAdultAndReturnsTokens() {
        when(userRepository.existsByUsernameIgnoreCase("alice")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);

        AuthResponse response = authService.register(registerRequest(LocalDate.now().minusYears(25)));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.username()).isEqualTo("alice");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPasswordHash()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", saved.getPasswordHash())).isTrue();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void register_rejectsUnderageUser() {
        assertThatThrownBy(() -> authService.register(registerRequest(LocalDate.now().minusYears(17))))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.UNDERAGE);
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_rejectsExactly18Tomorrow() {
        LocalDate almostEighteen = LocalDate.now().minusYears(18).plusDays(1);
        assertThatThrownBy(() -> authService.register(registerRequest(almostEighteen)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.UNDERAGE);
    }

    @Test
    void register_rejectsTakenUsername() {
        when(userRepository.existsByUsernameIgnoreCase("alice")).thenReturn(true);
        assertThatThrownBy(() -> authService.register(registerRequest(LocalDate.now().minusYears(25))))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.USERNAME_TAKEN);
    }

    @Test
    void register_rejectsTakenEmail() {
        when(userRepository.existsByUsernameIgnoreCase("alice")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(true);
        assertThatThrownBy(() -> authService.register(registerRequest(LocalDate.now().minusYears(25))))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.EMAIL_TAKEN);
    }

    private User activeUser(String rawPassword) {
        return User.builder()
                .username("alice")
                .email("alice@example.com")
                .passwordHash(passwordEncoder.encode(rawPassword))
                .birthDate(LocalDate.now().minusYears(25))
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    void login_succeedsWithCorrectPassword() {
        User user = activeUser("password123");
        when(userRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(new LoginRequest("alice", "password123"));

        assertThat(response.userId()).isEqualTo(user.getId());
        assertThat(response.accessToken()).isNotBlank();
    }

    @Test
    void login_rejectsWrongPassword() {
        when(userRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(activeUser("password123")));

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "wrong")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void login_rejectsSuspendedUser() {
        User user = activeUser("password123");
        user.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "password123")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.ACCOUNT_SUSPENDED);
    }

    @Test
    void refresh_rotatesToken() {
        User user = activeUser("password123");
        String rawToken = "some-raw-refresh-token";
        RefreshToken stored = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(AuthService.sha256(rawToken))
                .expiresAt(Instant.now().plus(Duration.ofDays(1)))
                .build();
        when(refreshTokenRepository.findByTokenHash(AuthService.sha256(rawToken))).thenReturn(Optional.of(stored));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        AuthResponse response = authService.refresh(new RefreshRequest(rawToken));

        assertThat(stored.isRevoked()).isTrue();
        assertThat(response.refreshToken()).isNotEqualTo(rawToken);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void refresh_reuseOfRevokedTokenRevokesWholeFamily() {
        UUID userId = UUID.randomUUID();
        String rawToken = "reused-token";
        RefreshToken stored = RefreshToken.builder()
                .userId(userId)
                .tokenHash(AuthService.sha256(rawToken))
                .expiresAt(Instant.now().plus(Duration.ofDays(1)))
                .revoked(true)
                .build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(rawToken)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
        verify(refreshTokenRepository).revokeAllForUser(userId);
    }
}

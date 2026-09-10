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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    public static final int MINIMUM_AGE = 18;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    private final SecureRandom secureRandom = new SecureRandom();

    private volatile String cachedDummyHash;

    private String dummyHash() {
        String hash = cachedDummyHash;
        if (hash == null) {
            hash = passwordEncoder.encode(UUID.randomUUID().toString());
            cachedDummyHash = hash;
        }
        return hash;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.birthDate().isAfter(LocalDate.now().minusYears(MINIMUM_AGE))) {
            throw new ApiException(ErrorCode.UNDERAGE);
        }
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new ApiException(ErrorCode.USERNAME_TAKEN);
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ApiException(ErrorCode.EMAIL_TAKEN);
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .birthDate(request.birthDate())
                .countryCode(request.countryCode())
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameIgnoreCase(request.identifier())
                .or(() -> userRepository.findByEmailIgnoreCase(request.identifier()))
                .orElse(null);

        // Always run one BCrypt comparison so response time does not reveal
        // whether the identifier exists (user enumeration via timing).
        String hashToCheck = user != null ? user.getPasswordHash() : dummyHash();
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);
        if (user == null || !passwordMatches) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
        requireUsable(user);

        user.setLastSeenAt(Instant.now());
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(sha256(request.refreshToken()))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            // Reuse of a revoked token is a red flag: kill the whole session family.
            refreshTokenRepository.revokeAllForUser(stored.getUserId());
            throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REFRESH_TOKEN));
        requireUsable(user);

        stored.setRevoked(true);
        return issueTokens(user);
    }

    private void requireUsable(User user) {
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new ApiException(ErrorCode.ACCOUNT_SUSPENDED);
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername());

        byte[] raw = new byte[48];
        secureRandom.nextBytes(raw);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        refreshTokenRepository.save(RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(sha256(refreshToken))
                .expiresAt(Instant.now().plus(jwtProperties.refreshTokenTtl()))
                .build());

        return new AuthResponse(accessToken, refreshToken, user.getId(), user.getUsername());
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

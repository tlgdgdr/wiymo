package com.socialworld.app.auth;

import com.socialworld.app.config.JwtProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(new JwtProperties(
            "test-secret-test-secret-test-secret-test-secret",
            Duration.ofMinutes(15),
            Duration.ofDays(30)));

    @Test
    void roundTripsUserId() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId, "alice");
        assertThat(jwtService.validateAndGetUserId(token)).contains(userId);
    }

    @Test
    void rejectsGarbageToken() {
        assertThat(jwtService.validateAndGetUserId("not.a.jwt")).isEmpty();
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() {
        JwtService other = new JwtService(new JwtProperties(
                "another-secret-another-secret-another-secret!!",
                Duration.ofMinutes(15),
                Duration.ofDays(30)));
        String token = other.generateAccessToken(UUID.randomUUID(), "alice");
        assertThat(jwtService.validateAndGetUserId(token)).isEmpty();
    }

    @Test
    void rejectsExpiredToken() {
        JwtService expired = new JwtService(new JwtProperties(
                "test-secret-test-secret-test-secret-test-secret",
                Duration.ofMinutes(-5),
                Duration.ofDays(30)));
        String token = expired.generateAccessToken(UUID.randomUUID(), "alice");
        assertThat(jwtService.validateAndGetUserId(token)).isEmpty();
    }
}

package com.socialworld.app.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Refuses to start with the checked-in development secret unless the
 * "local" profile is active, so a misconfigured deployment cannot silently
 * issue forgeable tokens.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtSecretSanityCheck {

    static final String DEV_SECRET = "local-dev-only-secret-change-me-0123456789abcdef";

    private final JwtProperties jwtProperties;
    private final Environment environment;

    @EventListener(ApplicationReadyEvent.class)
    public void verify() {
        if (DEV_SECRET.equals(jwtProperties.secret())) {
            if (Arrays.asList(environment.getActiveProfiles()).contains("local")) {
                log.warn("Running with the built-in development JWT secret (local profile only).");
            } else {
                throw new IllegalStateException(
                        "The default development JWT secret is in use outside the 'local' profile. "
                                + "Set JWT_SECRET (openssl rand -hex 32) before starting.");
            }
        }
    }
}

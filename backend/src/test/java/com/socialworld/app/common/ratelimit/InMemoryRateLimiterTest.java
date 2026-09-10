package com.socialworld.app.common.ratelimit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRateLimiterTest {

    private final InMemoryRateLimiter rateLimiter = new InMemoryRateLimiter();

    @Test
    void allowsUpToLimitThenRejects() {
        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiter.tryAcquire("k", 5, 3600)).isTrue();
        }
        assertThat(rateLimiter.tryAcquire("k", 5, 3600)).isFalse();
    }

    @Test
    void keysAreIndependent() {
        assertThat(rateLimiter.tryAcquire("a", 1, 3600)).isTrue();
        assertThat(rateLimiter.tryAcquire("a", 1, 3600)).isFalse();
        assertThat(rateLimiter.tryAcquire("b", 1, 3600)).isTrue();
    }
}

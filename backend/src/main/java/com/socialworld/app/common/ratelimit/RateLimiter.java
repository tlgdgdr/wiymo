package com.socialworld.app.common.ratelimit;

/**
 * Rate limiting abstraction. The MVP ships an in-memory implementation;
 * a Redis-backed one can replace it later without touching callers.
 */
public interface RateLimiter {

    /**
     * Registers a hit for the given key and reports whether it is still within
     * {@code maxRequests} per {@code windowSeconds}.
     */
    boolean tryAcquire(String key, int maxRequests, int windowSeconds);
}

package com.socialworld.app.common.ratelimit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-window counter per key. Good enough for a single instance;
 * swap for a Redis implementation when scaling out.
 */
@Component
public class InMemoryRateLimiter implements RateLimiter {

    private record Window(long windowStartEpochSeconds, AtomicInteger count) {
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String key, int maxRequests, int windowSeconds) {
        long now = Instant.now().getEpochSecond();
        long windowStart = now - (now % windowSeconds);

        Window window = windows.compute(key, (k, existing) ->
                existing == null || existing.windowStartEpochSeconds() != windowStart
                        ? new Window(windowStart, new AtomicInteger())
                        : existing);

        return window.count().incrementAndGet() <= maxRequests;
    }

    /** Drop stale windows so the map cannot grow without bound. */
    @Scheduled(fixedDelay = 300_000)
    void evictStale() {
        long cutoff = Instant.now().getEpochSecond() - 3600;
        for (Iterator<Map.Entry<String, Window>> it = windows.entrySet().iterator(); it.hasNext(); ) {
            if (it.next().getValue().windowStartEpochSeconds() < cutoff) {
                it.remove();
            }
        }
    }
}

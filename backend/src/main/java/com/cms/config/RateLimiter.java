package com.cms.config;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory rate limiter using ConcurrentHashMap.
 * Replaces Redis sliding-window rate limiting.
 */
@Component
public class RateLimiter {

    private record Window(AtomicLong count, Instant expiresAt) {}
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public boolean isRateLimited(String key, int maxRequests, Duration window) {
        Instant now = Instant.now();
        windows.compute(key, (k, existing) -> {
            if (existing == null || now.isAfter(existing.expiresAt())) {
                return new Window(new AtomicLong(1), now.plus(window));
            }
            existing.count().incrementAndGet();
            return existing;
        });
        Window w = windows.get(key);
        return w != null && w.count().get() > maxRequests;
    }
}

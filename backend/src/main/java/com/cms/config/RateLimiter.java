package com.cms.config;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RateLimiter {

    private final StringRedisTemplate redisTemplate;

    public RateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Check if the action is rate-limited.
     * @return true if the request should be rejected (rate limit exceeded)
     */
    public boolean isRateLimited(String key, int maxRequests, Duration window) {
        String redisKey = "ratelimit:" + key;
        Long current = redisTemplate.opsForValue().increment(redisKey);
        if (current != null && current == 1) {
            redisTemplate.expire(redisKey, window);
        }
        return current != null && current > maxRequests;
    }
}

package com.arsh.workflow.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RedisDistributedLock {

    private final StringRedisTemplate redisTemplate;

    // Reuse compiled script, do NOT rebuild per request
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>();

    static {
        UNLOCK_SCRIPT.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                        "   return redis.call('del', KEYS[1]) " +
                        "else " +
                        "   return 0 " +
                        "end"
        );
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    /**
     * Attempts to acquire a lock with TTL.
     */
    public String tryLock(String key, Duration ttl) {
        String token = UUID.randomUUID().toString();

        Boolean success = redisTemplate
                .opsForValue()
                .setIfAbsent(key, token, ttl);

        return Boolean.TRUE.equals(success) ? token : null;
    }

    /**
     * Blocking lock with retries until waitTimeoutMs expires.
     */
    public String lockBlocking(String key, Duration ttl, long waitTimeoutMs, long retryMillis) {
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start < waitTimeoutMs) {
            String token = tryLock(key, ttl);
            if (token != null) {
                return token;
            }

            try {
                Thread.sleep(retryMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        return null;
    }

    /**
     * Safe unlock using Lua script to avoid deleting another node's lock.
     */
    public boolean releaseLock(String key, String token) {
        Long result = redisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(key),
                token
        );
        return Long.valueOf(1L).equals(result);
    }

    /**
     * Fast idempotency check (GET is faster than HASKEY).
     */
    public boolean isAlreadyExecuted(String key) {
        String val = redisTemplate.opsForValue().get(key);
        return val != null;
    }

    /**
     * Set idempotency flag for completed task.
     */
    public void markExecuted(String key, Duration ttl) {
        redisTemplate.opsForValue().set(key, "1", ttl);
    }
}

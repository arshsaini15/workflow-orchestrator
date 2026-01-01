package com.arsh.workflow.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisDistributedLock {

    private final StringRedisTemplate redisTemplate;

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

        log.info("TRY LOCK | key={} | ttl={} | thread={}",
                key, ttl.toMillis(), Thread.currentThread().getName());

        Boolean success = redisTemplate
                .opsForValue()
                .setIfAbsent(key, token, ttl);

        if (Boolean.TRUE.equals(success)) {
            log.info("LOCK ACQUIRED | key={} | token={}", key, token);
            return token;
        }

        log.info("LOCK FAILED | key={} | another executor holds the lock", key);
        return null;
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
                log.warn("LOCK INTERRUPTED | key={}", key);
                return null;
            }
        }

        log.warn("LOCK TIMEOUT | key={} | waited={}ms", key, waitTimeoutMs);
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

        boolean success = Long.valueOf(1L).equals(result);

        log.info("LOCK RELEASE | key={} | token={} | success={}",
                key, token, success);

        return success;
    }

    /**
     * Fast idempotency check (GET is faster than HASKEY).
     */
    public boolean isAlreadyExecuted(String key) {
        boolean executed = redisTemplate.opsForValue().get(key) != null;

        log.info("IDEMPOTENCY CHECK | key={} | alreadyExecuted={}",
                key, executed);

        return executed;
    }

    /**
     * Set idempotency flag for completed task.
     */
    public void markExecuted(String key, Duration ttl) {
        redisTemplate.opsForValue().set(key, "1", ttl);

        log.info("IDEMPOTENCY MARKED | key={} | ttl={}ms",
                key, ttl.toMillis());
    }
}

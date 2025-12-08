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

    private static final String UNLOCK_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "   return redis.call('del', KEYS[1]) " +
                    "else " +
                    "   return 0 " +
                    "end";

    public String tryLock(String key, Duration ttl) {
        String token = UUID.randomUUID().toString();
        Boolean success = redisTemplate
                .opsForValue()
                .setIfAbsent(key, token, ttl);

        return Boolean.TRUE.equals(success) ? token : null;
    }

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

    public boolean releaseLock(String key, String token) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(UNLOCK_LUA);
        script.setResultType(Long.class);

        Long result = redisTemplate.execute(
                script,
                Collections.singletonList(key),
                token
        );
        return Long.valueOf(1L).equals(result);
    }
}

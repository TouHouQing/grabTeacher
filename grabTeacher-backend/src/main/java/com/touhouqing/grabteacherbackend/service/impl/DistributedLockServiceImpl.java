package com.touhouqing.grabteacherbackend.service.impl;

import com.touhouqing.grabteacherbackend.service.DistributedLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 简单的基于 RedisTemplate 的分布式锁实现
 * - 加锁使用 SET NX PX
 * - 解锁使用 Lua 脚本，保证原子性（校验 token 一致再删除）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockServiceImpl implements DistributedLockService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean tryLock(String key, String token, Duration ttl) {
        try {
            Boolean success = redisTemplate.opsForValue().setIfAbsent(key, token, ttl);
            return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            log.error("获取分布式锁失败 key={}", key, e);
            return false;
        }
    }

    @Override
    public boolean tryLock(String key, String token, Duration ttl, int retryTimes, Duration retryInterval) {
        int attempts = 0;
        while (attempts <= retryTimes) {
            if (tryLock(key, token, ttl)) {
                return true;
            }
            attempts++;
            try {
                Thread.sleep(Math.max(1L, retryInterval.toMillis()));
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean unlock(String key, String token) {
        // Lua 脚本: 比较值后再删除
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                        "return redis.call('del', KEYS[1]) else return 0 end";
        try {
            Object res = redisTemplate.execute((RedisCallback<Object>) connection ->
                connection.scriptingCommands().eval(
                    script.getBytes(StandardCharsets.UTF_8),
                    ReturnType.INTEGER,
                    1,
                    key.getBytes(StandardCharsets.UTF_8),
                    token.getBytes(StandardCharsets.UTF_8)
                )
            );
            if (res instanceof Long) {
                return ((Long) res) > 0;
            }
            // 某些驱动实现可能返回 byte[]
            if (res instanceof byte[]) {
                try {
                    long v = Long.parseLong(new String((byte[]) res, StandardCharsets.UTF_8));
                    return v > 0;
                } catch (NumberFormatException ignore) {}
            }
            return false;
        } catch (DataAccessException e) {
            log.error("释放分布式锁失败 key={}", key, e);
            return false;
        }
    }
}


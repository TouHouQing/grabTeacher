package com.touhouqing.grabteacherbackend.service;

import java.time.Duration;

/**
 * 基于 Redis 的分布式锁服务
 * 适用于高并发下同一教师同一时间段的预约并发控制
 */
public interface DistributedLockService {

    /**
     * 尝试获取锁（不重试）
     * @param key 锁键
     * @param token 唯一令牌（用于安全释放）
     * @param ttl 锁过期时间
     * @return 是否获取成功
     */
    boolean tryLock(String key, String token, Duration ttl);

    /**
     * 尝试获取锁（带重试）
     * @param key 锁键
     * @param token 唯一令牌
     * @param ttl 锁过期时间
     * @param retryTimes 重试次数
     * @param retryInterval 每次重试间隔
     * @return 是否获取成功
     */
    boolean tryLock(String key, String token, Duration ttl, int retryTimes, Duration retryInterval);

    /**
     * 释放锁（仅当 token 匹配时才会释放）
     * @param key 锁键
     * @param token 唯一令牌
     * @return 是否释放成功
     */
    boolean unlock(String key, String token);
}


package com.touhouqing.grabteacherbackend.cache;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地一级缓存（极小容量），专用于 /api/teacher/featured?page=..&size=.. 的预序列化 JSON
 * 目标：降低热门键的 Redis 反序列化与 JSON 重序列化成本
 * 说明：不设置 TTL，依赖服务端写操作的集中失效来保证新鲜度（通过显式 clear 或缓存名失效）
 */
@Component
public class FeaturedTeachersLocalCache {
    private final Map<String, String> jsonCache = new ConcurrentHashMap<>();

    public String get(String key) {
        return jsonCache.get(key);
    }

    public void put(String key, String json) {
        if (key != null && json != null) {
            jsonCache.put(key, json);
        }
    }

    public void clear() {
        jsonCache.clear();
    }
}


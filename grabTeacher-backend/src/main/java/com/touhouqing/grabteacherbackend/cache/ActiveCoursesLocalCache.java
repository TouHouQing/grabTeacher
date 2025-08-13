package com.touhouqing.grabteacherbackend.cache;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地一级缓存（极小容量），专用于 /api/public/courses/active?limit=... 的预序列化 JSON
 * 目标：降低热点键的 Redis 反序列化与 JSON 重序列化成本
 * 注意：不设置 TTL，统一依赖服务端写操作后的集中失效来保证新鲜度
 */
@Component
public class ActiveCoursesLocalCache {
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


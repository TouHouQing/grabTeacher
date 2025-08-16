package com.touhouqing.grabteacherbackend.cache;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地一级缓存，专用于 /api/public/subjects/active 的预序列化 JSON
 */
@Component
public class ActiveSubjectsLocalCache {
    private final Map<String, String> jsonCache = new ConcurrentHashMap<>();

    public String get(String key) { return jsonCache.get(key); }
    public void put(String key, String json) {
        if (key != null && json != null) jsonCache.put(key, json);
    }
    public void clear() { jsonCache.clear(); }
}


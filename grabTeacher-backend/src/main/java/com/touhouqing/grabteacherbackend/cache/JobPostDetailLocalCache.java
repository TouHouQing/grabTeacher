package com.touhouqing.grabteacherbackend.cache;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 教师招聘详情本地一级缓存（极热Key的JSON预序列化）
 * 不设置TTL，依赖写操作后的精准失效来保证数据新鲜度
 */
@Component
public class JobPostDetailLocalCache {
    private final Map<Long, String> jsonCache = new ConcurrentHashMap<>();

    public String get(Long id) {
        return jsonCache.get(id);
    }

    public void put(Long id, String json) {
        if (id != null && json != null) {
            jsonCache.put(id, json);
        }
    }

    public void evict(Long id) {
        if (id != null) jsonCache.remove(id);
    }

    public void clear() {
        jsonCache.clear();
    }
}


package com.touhouqing.grabteacherbackend.cache;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 本地一级缓存：公开端留学项目 listActive 的预序列化 JSON */
@Component
public class AbroadProgramsLocalCache {
    private final Map<String, String> jsonCache = new ConcurrentHashMap<>();
    public String get(String key) { return jsonCache.get(key); }
    public void put(String key, String json) { if (key!=null && json!=null) jsonCache.put(key, json); }
    public void clear() { jsonCache.clear(); }
}


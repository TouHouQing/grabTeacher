package com.touhouqing.grabteacherbackend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存监控服务
 * 提供缓存命中率统计、性能监控等功能
 */
@Slf4j
@Service
public class CacheMonitorService {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 缓存统计数据
    private final Map<String, CacheStats> cacheStatsMap = new ConcurrentHashMap<>();

    /**
     * 缓存统计数据类
     */
    public static class CacheStats {
        private final AtomicLong hitCount = new AtomicLong(0);
        private final AtomicLong missCount = new AtomicLong(0);
        private final AtomicLong putCount = new AtomicLong(0);
        private final AtomicLong evictionCount = new AtomicLong(0);
        private LocalDateTime lastAccessTime = LocalDateTime.now();
        private LocalDateTime createdTime = LocalDateTime.now();

        public void recordHit() {
            hitCount.incrementAndGet();
            lastAccessTime = LocalDateTime.now();
        }

        public void recordMiss() {
            missCount.incrementAndGet();
            lastAccessTime = LocalDateTime.now();
        }

        public void recordPut() {
            putCount.incrementAndGet();
            lastAccessTime = LocalDateTime.now();
        }

        public void recordEviction() {
            evictionCount.incrementAndGet();
        }

        public long getHitCount() { return hitCount.get(); }
        public long getMissCount() { return missCount.get(); }
        public long getPutCount() { return putCount.get(); }
        public long getEvictionCount() { return evictionCount.get(); }
        public LocalDateTime getLastAccessTime() { return lastAccessTime; }
        public LocalDateTime getCreatedTime() { return createdTime; }

        public double getHitRate() {
            long total = hitCount.get() + missCount.get();
            return total == 0 ? 0.0 : (double) hitCount.get() / total;
        }

        public long getTotalRequests() {
            return hitCount.get() + missCount.get();
        }
    }

    /**
     * 记录缓存命中
     */
    public void recordCacheHit(String cacheName) {
        getCacheStats(cacheName).recordHit();
        log.debug("缓存命中: {}", cacheName);
    }

    /**
     * 记录缓存未命中
     */
    public void recordCacheMiss(String cacheName) {
        getCacheStats(cacheName).recordMiss();
        log.debug("缓存未命中: {}", cacheName);
    }

    /**
     * 记录缓存写入
     */
    public void recordCachePut(String cacheName) {
        getCacheStats(cacheName).recordPut();
        log.debug("缓存写入: {}", cacheName);
    }

    /**
     * 记录缓存清除
     */
    public void recordCacheEviction(String cacheName) {
        getCacheStats(cacheName).recordEviction();
        log.debug("缓存清除: {}", cacheName);
    }

    /**
     * 获取缓存统计信息
     */
    private CacheStats getCacheStats(String cacheName) {
        return cacheStatsMap.computeIfAbsent(cacheName, k -> new CacheStats());
    }

    /**
     * 获取所有缓存的统计报告
     */
    public Map<String, Object> getCacheStatsReport() {
        Map<String, Object> report = new HashMap<>();
        
        // 基本统计信息
        Map<String, Object> basicStats = new HashMap<>();
        for (Map.Entry<String, CacheStats> entry : cacheStatsMap.entrySet()) {
            String cacheName = entry.getKey();
            CacheStats stats = entry.getValue();
            
            Map<String, Object> cacheReport = new HashMap<>();
            cacheReport.put("hitCount", stats.getHitCount());
            cacheReport.put("missCount", stats.getMissCount());
            cacheReport.put("putCount", stats.getPutCount());
            cacheReport.put("evictionCount", stats.getEvictionCount());
            cacheReport.put("hitRate", String.format("%.2f%%", stats.getHitRate() * 100));
            cacheReport.put("totalRequests", stats.getTotalRequests());
            cacheReport.put("lastAccessTime", stats.getLastAccessTime());
            cacheReport.put("createdTime", stats.getCreatedTime());
            
            basicStats.put(cacheName, cacheReport);
        }
        report.put("cacheStats", basicStats);
        
        // Redis统计信息
        report.put("redisStats", getRedisStats());
        
        // 缓存大小统计
        report.put("cacheSizes", getCacheSizes());
        
        // 总体统计
        report.put("overallStats", getOverallStats());
        
        report.put("reportTime", LocalDateTime.now());
        
        return report;
    }

    /**
     * 获取Redis统计信息
     */
    private Map<String, Object> getRedisStats() {
        Map<String, Object> redisStats = new HashMap<>();

        try {
            // 获取Redis信息 - 使用新的API
            Properties info = redisTemplate.getConnectionFactory().getConnection().serverCommands().info();

            redisStats.put("connectedClients", info.getProperty("connected_clients"));
            redisStats.put("usedMemory", info.getProperty("used_memory_human"));
            redisStats.put("usedMemoryPeak", info.getProperty("used_memory_peak_human"));
            redisStats.put("keyspaceHits", info.getProperty("keyspace_hits"));
            redisStats.put("keyspaceMisses", info.getProperty("keyspace_misses"));

            // 计算命中率
            String hits = info.getProperty("keyspace_hits");
            String misses = info.getProperty("keyspace_misses");
            if (hits != null && misses != null) {
                long hitCount = Long.parseLong(hits);
                long missCount = Long.parseLong(misses);
                long total = hitCount + missCount;
                double hitRate = total == 0 ? 0.0 : (double) hitCount / total;
                redisStats.put("hitRate", String.format("%.2f%%", hitRate * 100));
            }

        } catch (Exception e) {
            log.error("获取Redis统计信息失败", e);
            redisStats.put("error", "无法获取Redis统计信息: " + e.getMessage());
        }

        return redisStats;
    }

    /**
     * 获取各缓存的大小统计
     */
    private Map<String, Object> getCacheSizes() {
        Map<String, Object> sizes = new HashMap<>();
        
        try {
            // 获取各个缓存的key数量
            String[] cacheNames = {"course", "courseList", "teacherCourses", "activeCourses", "subjects", "grades"};
            
            for (String cacheName : cacheNames) {
                Set<String> keys = redisTemplate.keys("grabTeacher:" + cacheName + ":*");
                sizes.put(cacheName, keys != null ? keys.size() : 0);
            }
            
            // 总key数量
            Set<String> allKeys = redisTemplate.keys("grabTeacher:*");
            sizes.put("total", allKeys != null ? allKeys.size() : 0);
            
        } catch (Exception e) {
            log.error("获取缓存大小统计失败", e);
            sizes.put("error", "无法获取缓存大小统计: " + e.getMessage());
        }
        
        return sizes;
    }

    /**
     * 获取总体统计信息
     */
    private Map<String, Object> getOverallStats() {
        Map<String, Object> overall = new HashMap<>();
        
        long totalHits = 0;
        long totalMisses = 0;
        long totalPuts = 0;
        long totalEvictions = 0;
        
        for (CacheStats stats : cacheStatsMap.values()) {
            totalHits += stats.getHitCount();
            totalMisses += stats.getMissCount();
            totalPuts += stats.getPutCount();
            totalEvictions += stats.getEvictionCount();
        }
        
        overall.put("totalHits", totalHits);
        overall.put("totalMisses", totalMisses);
        overall.put("totalPuts", totalPuts);
        overall.put("totalEvictions", totalEvictions);
        overall.put("totalRequests", totalHits + totalMisses);
        
        double overallHitRate = (totalHits + totalMisses) == 0 ? 0.0 : 
                               (double) totalHits / (totalHits + totalMisses);
        overall.put("overallHitRate", String.format("%.2f%%", overallHitRate * 100));
        
        return overall;
    }

    /**
     * 获取缓存性能报告
     */
    public String getCachePerformanceReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== 缓存性能报告 ===\n");
        report.append("生成时间: ").append(LocalDateTime.now()).append("\n\n");
        
        // 各缓存统计
        for (Map.Entry<String, CacheStats> entry : cacheStatsMap.entrySet()) {
            String cacheName = entry.getKey();
            CacheStats stats = entry.getValue();
            
            report.append("缓存: ").append(cacheName).append("\n");
            report.append("  命中次数: ").append(stats.getHitCount()).append("\n");
            report.append("  未命中次数: ").append(stats.getMissCount()).append("\n");
            report.append("  命中率: ").append(String.format("%.2f%%", stats.getHitRate() * 100)).append("\n");
            report.append("  写入次数: ").append(stats.getPutCount()).append("\n");
            report.append("  清除次数: ").append(stats.getEvictionCount()).append("\n");
            report.append("  最后访问: ").append(stats.getLastAccessTime()).append("\n");
            report.append("\n");
        }
        
        // 总体统计
        Map<String, Object> overall = getOverallStats();
        report.append("=== 总体统计 ===\n");
        report.append("总命中次数: ").append(overall.get("totalHits")).append("\n");
        report.append("总未命中次数: ").append(overall.get("totalMisses")).append("\n");
        report.append("总体命中率: ").append(overall.get("overallHitRate")).append("\n");
        report.append("总写入次数: ").append(overall.get("totalPuts")).append("\n");
        report.append("总清除次数: ").append(overall.get("totalEvictions")).append("\n");
        
        return report.toString();
    }

    /**
     * 重置统计数据
     */
    public void resetStats() {
        cacheStatsMap.clear();
        log.info("缓存统计数据已重置");
    }

    /**
     * 获取指定缓存的统计信息
     */
    public CacheStats getSpecificCacheStats(String cacheName) {
        return cacheStatsMap.get(cacheName);
    }
}

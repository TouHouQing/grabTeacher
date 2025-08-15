package com.touhouqing.grabteacherbackend.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class JobPostCacheManager {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String LIST_PREFIX = "grabTeacher:jobPosts:list:";
    private static final String INDEX_GRADE_PREFIX = "grabTeacher:jobPosts:index:grade:";
    private static final String INDEX_SUBJECT_PREFIX = "grabTeacher:jobPosts:index:subject:";
    private static final String LOCK_PREFIX = "grabTeacher:jobPosts:lock:";

    public String buildListKey(int page, int size, Long gradeId, Long subjectId) {
        String g = gradeId == null ? "ALL" : String.valueOf(gradeId);
        String s = subjectId == null ? "ALL" : String.valueOf(subjectId);
        return LIST_PREFIX + "page_" + page + ":size_" + size + ":grade_" + g + ":subject_" + s;
    }

    public void saveList(String key, Object pageObj, Long gradeId, Long subjectId, Duration ttl) {
        redisTemplate.opsForValue().set(key, pageObj, jitter(ttl));
        // 将缓存key登记到维度索引
        String g = gradeId == null ? "ALL" : String.valueOf(gradeId);
        String s = subjectId == null ? "ALL" : String.valueOf(subjectId);
        stringRedisTemplate.opsForSet().add(INDEX_GRADE_PREFIX + g, key);
        stringRedisTemplate.opsForSet().add(INDEX_SUBJECT_PREFIX + s, key);
    }

    public Object getList(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public boolean tryLock(String key, Duration ttl) {
        String lockKey = LOCK_PREFIX + key;
        Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", ttl);
        return Boolean.TRUE.equals(ok);
    }

    public void unlock(String key) {
        stringRedisTemplate.delete(LOCK_PREFIX + key);
    }

    public void evictByDimensions(Set<Long> gradeIds, Set<Long> subjectIds) {
        // 收集需要删除的缓存键
        Set<String> keys = new HashSet<>();
        // 始终包含 ALL 维度
        keys.addAll(members(INDEX_GRADE_PREFIX + "ALL"));
        keys.addAll(members(INDEX_SUBJECT_PREFIX + "ALL"));
        // 指定维度
        if (gradeIds != null && !gradeIds.isEmpty()) {
            for (Long g : gradeIds) keys.addAll(members(INDEX_GRADE_PREFIX + g));
        }
        if (subjectIds != null && !subjectIds.isEmpty()) {
            for (Long s : subjectIds) keys.addAll(members(INDEX_SUBJECT_PREFIX + s));
        }
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
            // 清理索引集合（ALL + 指定维度）
            Set<String> affectedGradeSets = new HashSet<>();
            affectedGradeSets.add(INDEX_GRADE_PREFIX + "ALL");
            if (gradeIds != null && !gradeIds.isEmpty()) {
                for (Long g : gradeIds) affectedGradeSets.add(INDEX_GRADE_PREFIX + g);
            }
            Set<String> affectedSubjectSets = new HashSet<>();
            affectedSubjectSets.add(INDEX_SUBJECT_PREFIX + "ALL");
            if (subjectIds != null && !subjectIds.isEmpty()) {
                for (Long s : subjectIds) affectedSubjectSets.add(INDEX_SUBJECT_PREFIX + s);
            }
            for (String setKey : affectedGradeSets) {
                if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(setKey))) {
                    stringRedisTemplate.opsForSet().remove(setKey, keys.toArray());
                }
            }
            for (String setKey : affectedSubjectSets) {
                if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(setKey))) {
                    stringRedisTemplate.opsForSet().remove(setKey, keys.toArray());
                }
            }
        }
    }

    private Set<String> members(String setKey) {
        Set<String> m = stringRedisTemplate.opsForSet().members(setKey);
        return m == null ? Collections.emptySet() : m;
    }

    private Duration jitter(Duration base) {
        long max = Math.max(1, base.toMillis() / 10);
        long delta = ThreadLocalRandom.current().nextLong(max + 1);
        return base.plusMillis(delta);
    }
}


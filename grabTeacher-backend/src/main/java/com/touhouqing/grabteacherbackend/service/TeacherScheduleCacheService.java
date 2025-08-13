package com.touhouqing.grabteacherbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;

/**
 * 维护教师每日忙时（占用时段）缓存，粒度 30 分钟 slot。
 * key: grabTeacher:busy:teacher:{teacherId}:{yyyy-MM-dd}
 * value: Redis Set<String>，成员是 "HH:mm-HH:mm"（半小时为基本粒度，可复用项目 TimeSlotUtil）
 */
@Service
@RequiredArgsConstructor
public class TeacherScheduleCacheService {

    private final StringRedisTemplate stringRedisTemplate;

    // 默认 TTL 3 分钟，加 0~30 秒随机抖动，防雪崩
    private static final Duration BASE_TTL = Duration.ofMinutes(3);

    public String busyKey(Long teacherId, LocalDate date) {
        return "grabTeacher:busy:teacher:" + teacherId + ":" + date;
    }

    public void putBusySlots(Long teacherId, LocalDate date, List<String> slots) {
        String key = busyKey(teacherId, date);
        if (slots == null || slots.isEmpty()) {
            // 空集合也设 TTL，表示加载过，防止穿透
            stringRedisTemplate.opsForValue().set(key + ":empty", "1", withJitter(BASE_TTL));
            return;
        }
        stringRedisTemplate.opsForSet().add(key, slots.toArray(new String[0]));
        stringRedisTemplate.expire(key, withJitter(BASE_TTL));
    }

    public List<String> getBusySlots(Long teacherId, LocalDate date) {
        String key = busyKey(teacherId, date);
        Set<String> members = stringRedisTemplate.opsForSet().members(key);
        if (members == null) return new ArrayList<>();
        return new ArrayList<>(members);
    }

    public boolean isCached(Long teacherId, LocalDate date) {
        String key = busyKey(teacherId, date);
        String emptyKey = key + ":empty";
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key)) ||
               Boolean.TRUE.equals(stringRedisTemplate.hasKey(emptyKey));
    }

    public void addBusySlot(Long teacherId, LocalDate date, String slot) {
        String key = busyKey(teacherId, date);
        stringRedisTemplate.opsForSet().add(key, slot);
        stringRedisTemplate.expire(key, withJitter(BASE_TTL));
        // 删除可能存在的 empty 标记
        stringRedisTemplate.delete(key + ":empty");
    }

    public void removeBusySlot(Long teacherId, LocalDate date, String slot) {
        String key = busyKey(teacherId, date);
        stringRedisTemplate.opsForSet().remove(key, slot);
        stringRedisTemplate.expire(key, withJitter(BASE_TTL));
    }

    public void putBusySlotsBatch(Long teacherId, Map<LocalDate, List<String>> daySlots) {
        if (daySlots == null || daySlots.isEmpty()) return;
        stringRedisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            public <K, V> Object execute(org.springframework.data.redis.core.RedisOperations<K, V> operations) {
                for (Map.Entry<LocalDate, List<String>> e : daySlots.entrySet()) {
                    String key = busyKey(teacherId, e.getKey());
                    List<String> slots = e.getValue();
                    if (slots != null && !slots.isEmpty()) {
                        Duration ttl = withJitter(BASE_TTL);
                        operations.opsForSet().add((K) ((Object) key), (V[]) slots.toArray(new String[0]));
                        operations.expire((K) ((Object) key), ttl);
                        // 清理可能存在的 empty 标记
                        operations.delete((K) ((Object) (key + ":empty")));
                    }
                }
                return null;
            }
        });
    }

    private Duration withJitter(Duration base) {
        long jitterMs = (long) (Math.random() * 30000); // 0~30s 抖动
        return base.plusMillis(jitterMs);
    }
}


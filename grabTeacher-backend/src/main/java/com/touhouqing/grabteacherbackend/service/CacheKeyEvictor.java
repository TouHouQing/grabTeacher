package com.touhouqing.grabteacherbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collection;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheKeyEvictor {

    private final StringRedisTemplate stringRedisTemplate;

    // 与 RedisConfiguration 中 computePrefixWith 保持一致
    private static final String PREFIX_TEACHER_SCHEDULE = "grabTeacher:teacherSchedule:";
    private static final String PREFIX_TEACHER_AVAILABILITY = "grabTeacher:teacherAvailability:";

    // 本服务新增的忙时缓存前缀
    private static final String PREFIX_BUSY_DAY = "grabTeacher:busy:teacher:"; // busy:teacher:{id}:{yyyy-MM-dd}

    /**
     * 精确清理某教师相关课表/可用性缓存；同时清理 busy 缓存（可选按日期）
     */
    public void evictTeacherScheduleAndAvailability(Long teacherId, Collection<LocalDate> dates) {
        try {
            // 清 teacherSchedule/teacherAvailability 中与该教师相关的所有 key（忽略日期范围）
            deleteByPattern(PREFIX_TEACHER_SCHEDULE + "*teacher_" + teacherId + ":*");
            deleteByPattern(PREFIX_TEACHER_AVAILABILITY + "*teacher_" + teacherId + ":*");

            // 清 busy:{date}
            if (dates != null) {
                for (LocalDate d : dates) {
                    String key = busyKey(teacherId, d);
                    stringRedisTemplate.delete(key);
                }
            }
            log.info("已精确清理教师{}的课表/可用性缓存，清理日期数: {}", teacherId, dates == null ? 0 : dates.size());
        } catch (Exception e) {
            log.warn("精确清理教师缓存失败 teacherId={}", teacherId, e);
        }
    }

    public void evictTeacherScheduleAndAvailability(Long teacherId) {
        evictTeacherScheduleAndAvailability(teacherId, null);
    }

    private void deleteByPattern(String pattern) {
        stringRedisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Void>) connection -> {
            try (Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().match(pattern).count(1000).build())) {
                while (cursor.hasNext()) {
                    byte[] key = cursor.next();
                    connection.keyCommands().del(key);
                }
            } catch (Exception e) {
                log.warn("SCAN 删除 keys 失败 pattern={}", pattern, e);
            }
            return null;
        });
    }

    public String busyKey(Long teacherId, LocalDate date) {
        return PREFIX_BUSY_DAY + teacherId + ":" + date.toString();
    }
}


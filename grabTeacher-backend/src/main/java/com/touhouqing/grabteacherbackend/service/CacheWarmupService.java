package com.touhouqing.grabteacherbackend.service;

import com.touhouqing.grabteacherbackend.entity.dto.CourseResponse;
import com.touhouqing.grabteacherbackend.mapper.CourseMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * 缓存预热和管理服务
 * 负责缓存的预热、清理和监控
 */
@Slf4j
@Service
public class CacheWarmupService implements ApplicationRunner {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private TeacherCacheWarmupService teacherCacheWarmupService;

    // 留学模块预热所需服务
    @Autowired
    private StudyAbroadCountryService countryService;

    @Autowired
    private StudyAbroadStageService stageService;

    @Autowired
    private StudyAbroadProgramService programService;

    /**
     * 应用启动时执行缓存预热
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("开始执行缓存预热...");
        warmupCaches();
        log.info("缓存预热完成");
    }

    /**
     * 异步执行缓存预热
     */
    @Async
    public CompletableFuture<Void> warmupCaches() {
        try {
            // 预热活跃课程缓存
            warmupActiveCourses();

            // 预热热门课程列表
            warmupPopularCourseLists();

            // 预热热门教师课程
            warmupPopularTeacherCourses();

            // 预热教师相关缓存
            teacherCacheWarmupService.warmupTeacherCaches();

            // 预热留学模块缓存
            warmupAbroadCaches();

            log.info("缓存预热任务完成");
        } catch (Exception e) {
            log.error("缓存预热失败", e);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 预热活跃课程缓存
     */
    private void warmupActiveCourses() {
        try {
            log.info("预热活跃课程缓存...");
            // 预热全量与常用 limit 版本（也会触发 JSON 文本缓存的写入）
            List<CourseResponse> activeCourses = courseService.getActiveCourses();
            List<CourseResponse> activeTop50 = courseService.getActiveCoursesLimited(50);
            log.info("预热活跃课程缓存完成：全量{}门，Top50 {}门",
                    activeCourses.size(), activeTop50.size());
        } catch (Exception e) {
            log.error("预热活跃课程缓存失败", e);
        }
    }

    /**
     * 预热热门课程列表
     */
    private void warmupPopularCourseLists() {
        try {
            log.info("预热热门课程列表缓存...");

            // 预热第一页课程列表（最常访问）
            courseService.getCourseList(1, 10, null, null, null, "active", null, null);

            // 预热不同课程类型的列表
            courseService.getCourseList(1, 10, null, null, null, "active", "one_on_one", null);
            courseService.getCourseList(1, 10, null, null, null, "active", "large_class", null);

            log.info("预热热门课程列表缓存完成");
        } catch (Exception e) {
            log.error("预热热门课程列表缓存失败", e);
        }
    }

    /**
     * 预热热门教师课程
     */
    private void warmupPopularTeacherCourses() {
        try {
            log.info("预热热门教师课程缓存...");

            // 获取有活跃课程的教师ID列表
            List<Long> activeTeacherIds = courseMapper.findActiveTeacherIds();

            // 为前10个活跃教师预热缓存
            activeTeacherIds.stream()
                    .limit(10)
                    .forEach(teacherId -> {
                        try {
                            courseService.getTeacherCourses(teacherId, null, "public");
                        } catch (Exception e) {
                            log.warn("预热教师{}的课程缓存失败", teacherId, e);
                        }
                    });

            log.info("预热热门教师课程缓存完成");
        } catch (Exception e) {
            log.error("预热热门教师课程缓存失败", e);
        }
    }

    /**
     * 定时清理过期缓存
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredCaches() {
        log.info("开始清理过期缓存...");

        try {
            // 清理所有课程相关缓存
            clearAllCourseCaches();

            // 重新预热核心缓存
            warmupCaches();

            log.info("过期缓存清理完成");
        } catch (Exception e) {
            log.error("清理过期缓存失败", e);
        }
    }

    /**
     * 清理所有课程相关缓存
     */
    @CacheEvict(cacheNames = {"course", "courseList", "teacherCourses", "activeCourses"}, allEntries = true)
    public void clearAllCourseCaches() {
        log.info("清理所有课程相关缓存");
    }

    /**
     * 清理特定课程的缓存
     */
    public void clearCourseCache(Long courseId) {
        if (courseId == null) {
            return;
        }

        try {
            // 清理单个课程缓存
            cacheManager.getCache("course").evict(courseId);

            // 清理相关列表缓存
            cacheManager.getCache("courseList").clear();
            cacheManager.getCache("teacherCourses").clear();
            cacheManager.getCache("activeCourses").clear();

            log.info("清理课程{}的相关缓存", courseId);
        } catch (Exception e) {
            log.error("清理课程{}缓存失败", courseId, e);
        }
    }

    /**
     * 预热留学模块缓存（国家/阶段/项目）
     */
    private void warmupAbroadCaches() {
        try {
            log.info("预热留学模块缓存...");
            // 公开端：国家/阶段 active 列表
            countryService.listActive();
            stageService.listActive();
            // 公开端：热门项目列表（limit=20）
            programService.listActive(20, null, null);
            // 管理端：项目第一页列表（常用）
            programService.list(1, 10, null, true, null, null, null, null);
            log.info("预热留学模块缓存完成");
        } catch (Exception e) {
            log.error("预热留学模块缓存失败", e);
        }
    }

    /**
     * 清理特定教师的课程缓存
     */
    public void clearTeacherCoursesCache(Long teacherId) {
        if (teacherId == null) {
            return;
        }

        try {
            // 清理教师课程缓存
            Set<String> keys = redisTemplate.keys("grabTeacher:teacherCourses:*teacher_" + teacherId + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }

            log.info("清理教师{}的课程缓存", teacherId);
        } catch (Exception e) {
            log.error("清理教师{}课程缓存失败", teacherId, e);
        }
    }

    /**
     * 获取缓存统计信息
     */
    public String getCacheStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("缓存统计信息:\n");

        try {
            // 获取Redis中的key数量
            Set<String> courseKeys = redisTemplate.keys("grabTeacher:course:*");
            Set<String> courseListKeys = redisTemplate.keys("grabTeacher:courseList:*");
            Set<String> teacherCoursesKeys = redisTemplate.keys("grabTeacher:teacherCourses:*");
            Set<String> activeCoursesKeys = redisTemplate.keys("grabTeacher:activeCourses:*");

            stats.append("课程缓存数量: ").append(courseKeys != null ? courseKeys.size() : 0).append("\n");
            stats.append("课程列表缓存数量: ").append(courseListKeys != null ? courseListKeys.size() : 0).append("\n");
            stats.append("教师课程缓存数量: ").append(teacherCoursesKeys != null ? teacherCoursesKeys.size() : 0).append("\n");
            stats.append("活跃课程缓存数量: ").append(activeCoursesKeys != null ? activeCoursesKeys.size() : 0).append("\n");
            stats.append("统计时间: ").append(LocalDateTime.now()).append("\n");

        } catch (Exception e) {
            stats.append("获取缓存统计信息失败: ").append(e.getMessage());
            log.error("获取缓存统计信息失败", e);
        }

        return stats.toString();
    }

    /**
     * 手动触发缓存预热
     */
    public void manualWarmup() {
        log.info("手动触发缓存预热");
        warmupCaches();
    }
}

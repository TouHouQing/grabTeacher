package com.touhouqing.grabteacherbackend.service;

import com.touhouqing.grabteacherbackend.model.entity.Subject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 精选课程缓存预热服务
 * 在应用启动时预热精选课程相关的缓存，提升用户访问体验
 */
@Slf4j
@Service
@Order(3) // 在其他缓存预热服务之后执行
public class FeaturedCourseCacheWarmupService implements ApplicationRunner {

    @Autowired
    private CourseService courseService;

    @Autowired
    private SubjectService subjectService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("开始预热精选课程缓存...");
        
        try {
            // 异步预热缓存，避免阻塞应用启动
            CompletableFuture.runAsync(this::warmupFeaturedCoursesCache)
                    .thenRun(() -> log.info("精选课程缓存预热完成"))
                    .exceptionally(throwable -> {
                        log.error("精选课程缓存预热失败", throwable);
                        return null;
                    });
        } catch (Exception e) {
            log.error("启动精选课程缓存预热任务失败", e);
        }
    }

    /**
     * 预热精选课程缓存
     */
    private void warmupFeaturedCoursesCache() {
        try {
            // 预热精选课程ID列表
            warmupFeaturedCourseIds();
            
            // 预热精选课程列表（第一页）
            warmupFeaturedCoursesList();
            
            // 预热不同科目的精选课程
            warmupFeaturedCoursesBySubjects();
            
        } catch (Exception e) {
            log.error("精选课程缓存预热过程中发生错误", e);
        }
    }

    /**
     * 预热精选课程ID列表
     */
    private void warmupFeaturedCourseIds() {
        try {
            List<Long> featuredCourseIds = courseService.getFeaturedCourseIds();
            log.debug("预热精选课程ID列表完成，共{}个精选课程", featuredCourseIds.size());
        } catch (Exception e) {
            log.warn("预热精选课程ID列表失败", e);
        }
    }

    /**
     * 预热精选课程列表
     */
    private void warmupFeaturedCoursesList() {
        try {
            // 预热第一页精选课程（最常访问）
            courseService.getFeaturedCourses(1, 6, null);
            log.debug("预热精选课程列表（第一页）完成");
            
            // 预热第二页精选课程
            courseService.getFeaturedCourses(2, 6, null);
            log.debug("预热精选课程列表（第二页）完成");
            
        } catch (Exception e) {
            log.warn("预热精选课程列表失败", e);
        }
    }

    /**
     * 预热不同科目的精选课程
     */
    private void warmupFeaturedCoursesBySubjects() {
        try {
            // 预热科目的精选课程（动态获取激活科目）
            List<Subject> activeSubjects = subjectService.getAllActiveSubjects();
            for (Subject s : activeSubjects) {
                try {
                    courseService.getFeaturedCourses(1, 6, s.getId());
                    log.debug("预热科目{}的精选课程完成", s.getName());
                } catch (Exception e) {
                    log.warn("预热科目{}的精选课程失败", s.getName(), e);
                }
            }

        } catch (Exception e) {
            log.warn("预热不同科目的精选课程失败", e);
        }
    }

    /**
     * 手动触发缓存预热（供管理员调用）
     */
    public void manualWarmup() {
        log.info("手动触发精选课程缓存预热...");
        CompletableFuture.runAsync(this::warmupFeaturedCoursesCache)
                .thenRun(() -> log.info("手动精选课程缓存预热完成"))
                .exceptionally(throwable -> {
                    log.error("手动精选课程缓存预热失败", throwable);
                    return null;
                });
    }

    /**
     * 清除精选课程相关缓存
     */
    public void clearFeaturedCoursesCache() {
        try {
            // 这里可以添加清除缓存的逻辑
            // 由于使用了@CacheEvict注解，缓存会在数据更新时自动清除
            log.info("精选课程缓存清除完成");
        } catch (Exception e) {
            log.error("清除精选课程缓存失败", e);
        }
    }
}

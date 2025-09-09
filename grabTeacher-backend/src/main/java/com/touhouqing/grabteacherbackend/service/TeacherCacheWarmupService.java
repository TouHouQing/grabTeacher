package com.touhouqing.grabteacherbackend.service;

import com.touhouqing.grabteacherbackend.model.dto.TeacherMatchDTO;
import com.touhouqing.grabteacherbackend.mapper.TeacherMapper;
import com.touhouqing.grabteacherbackend.model.entity.Subject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 教师缓存预热服务
 * 负责教师相关缓存的预热、清理和管理
 */
@Slf4j
@Service
public class TeacherCacheWarmupService {

    @Autowired
    @org.springframework.context.annotation.Lazy
    private TeacherService teacherService;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private SubjectService subjectService;

    /**
     * 异步执行教师缓存预热
     */
    @Async
    public CompletableFuture<Void> warmupTeacherCaches() {
        try {
            log.info("开始预热教师缓存...");
            
            // 预热热门教师
            warmupPopularTeachers();
            
            // 预热教师列表
            warmupTeacherLists();
            
            // 预热教师匹配
            warmupTeacherMatches();
            
            
            log.info("教师缓存预热完成");
        } catch (Exception e) {
            log.error("教师缓存预热失败", e);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 预热热门教师详情
     */
    private void warmupPopularTeachers() {
        try {
            log.info("预热热门教师详情缓存...");
            
            // 获取活跃教师ID列表（前20个）
            List<Long> activeTeacherIds = teacherMapper.findActiveTeacherIds();
            
            activeTeacherIds.stream()
                    .limit(20)
                    .forEach(teacherId -> {
                        try {
                            // 预热教师基本信息
                            teacherService.getTeacherById(teacherId);
                            
                            // 预热教师详情
                            teacherService.getTeacherDetailById(teacherId);
                            
                            // 预热教师时间表（未来7天）
                            LocalDate startDate = LocalDate.now();
                            LocalDate endDate = startDate.plusDays(7);
                            teacherService.getTeacherPublicSchedule(teacherId, startDate, endDate);
                            
                        } catch (Exception e) {
                            log.warn("预热教师{}缓存失败", teacherId, e);
                        }
                    });
            
            log.info("预热热门教师详情缓存完成");
        } catch (Exception e) {
            log.error("预热热门教师详情缓存失败", e);
        }
    }

    /**
     * 预热教师列表缓存
     */
    private void warmupTeacherLists() {
        try {
            log.info("预热教师列表缓存...");
            
            // 预热第一页教师列表（最常访问）
            teacherService.getTeacherListWithSubjects(1, 10, null, null);
            
            // 预热科目的教师列表（从数据库动态获取激活科目）
            List<Subject> activeSubjects = subjectService.getAllActiveSubjects();
            for (Subject s : activeSubjects) {
                try {
                    teacherService.getTeacherListWithSubjects(1, 10, s.getName(), null);
                } catch (Exception e) {
                    log.debug("预热科目教师列表失败: {}", s.getName(), e);
                }
            }


            log.info("预热教师列表缓存完成");
        } catch (Exception e) {
            log.error("预热教师列表缓存失败", e);
        }
    }

    /**
     * 预热教师匹配缓存
     * 根据当前匹配逻辑（科目 + 常见时间偏好）进行预热，提升首查命中
     * 控制组合数量，避免对数据库造成压力
     */
    private void warmupTeacherMatches() {
        try {
            log.info("预热教师匹配缓存...");

            // 动态获取激活科目
            List<Subject> activeSubjects = subjectService.getAllActiveSubjects();

            for (Subject s : activeSubjects) {
                try {
                    String subjectName = s.getName();

                    // 1) 基线：仅按科目
                    TeacherMatchDTO base = new TeacherMatchDTO();
                    base.setSubject(subjectName);
                    base.setLimit(10);
                    teacherService.matchTeachers(base);

                    // 2) 工作日晚高峰：19:00-21:00（周一~周五）
                    TeacherMatchDTO weekdayEvening = new TeacherMatchDTO();
                    weekdayEvening.setSubject(subjectName);
                    weekdayEvening.setLimit(10);
                    weekdayEvening.setPreferredWeekdays(java.util.Arrays.asList(1, 2, 3, 4, 5));
                    weekdayEvening.setPreferredTimeSlots(java.util.Arrays.asList("19:00-21:00"));
                    teacherService.matchTeachers(weekdayEvening);

                    // 3) 周末上午：10:00-12:00（周六、周日）
                    TeacherMatchDTO weekendMorning = new TeacherMatchDTO();
                    weekendMorning.setSubject(subjectName);
                    weekendMorning.setLimit(10);
                    weekendMorning.setPreferredWeekdays(java.util.Arrays.asList(6, 7));
                    weekendMorning.setPreferredTimeSlots(java.util.Arrays.asList("10:00-12:00"));
                    teacherService.matchTeachers(weekendMorning);
                } catch (Exception e) {
                    log.debug("预热教师匹配缓存失败: subject={}", s.getName(), e);
                }
            }

            log.info("预热教师匹配缓存完成");
        } catch (Exception e) {
            log.error("预热教师匹配缓存失败", e);
        }
    }


    /**
     * 为智能匹配结果的教师进行就近预热（课表/可用性/忙时）— 事件监听器
     */
    // 已取消匹配结果的异步预热（避免与读路径的范围预载重复、拖慢首个请求）
    // @Async
    // @EventListener
    // public void onMatchedTeachersWarmupEvent(MatchedTeachersWarmupEvent event) { ... }

    /**
     * 定时清理教师缓存
     * 每天凌晨3点执行
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupTeacherCaches() {
        log.info("开始清理教师缓存...");
        
        try {
            // 清理所有教师相关缓存
            clearAllTeacherCaches();
            
            // 重新预热核心缓存
            warmupTeacherCaches();
            
            log.info("教师缓存清理完成");
        } catch (Exception e) {
            log.error("清理教师缓存失败", e);
        }
    }

    /**
     * 清理所有教师相关缓存
     */
    @CacheEvict(cacheNames = {
        "teachers", "teacherDetails", "teacherList", 
        "teacherMatch", "teacherSubjects", "teacherSchedule", 
        "teacherAvailability"
    }, allEntries = true)
    public void clearAllTeacherCaches() {
        log.info("清理所有教师相关缓存");
    }

    /**
     * 清理特定教师的缓存
     */
    public void clearTeacherCache(Long teacherId) {
        if (teacherId == null) {
            return;
        }
        
        try {
            // 这里可以添加更精确的缓存清理逻辑
            // 由于Spring Cache的限制，我们清理相关的缓存区域
            clearAllTeacherCaches();
            
            log.info("清理教师{}的相关缓存", teacherId);
        } catch (Exception e) {
            log.error("清理教师{}缓存失败", teacherId, e);
        }
    }

    /**
     * 清理特定科目的教师缓存
     */
    public void clearSubjectTeacherCache(String subject) {
        if (subject == null || subject.trim().isEmpty()) {
            return;
        }
        
        try {
            // 清理相关的列表和匹配缓存
            clearAllTeacherCaches();
            
            log.info("清理科目{}的教师缓存", subject);
        } catch (Exception e) {
            log.error("清理科目{}教师缓存失败", subject, e);
        }
    }

    /**
     * 手动触发教师缓存预热
     */
    public void manualWarmupTeachers() {
        log.info("手动触发教师缓存预热");
        warmupTeacherCaches();
    }

    /**
     * 获取教师缓存统计信息
     */
    public String getTeacherCacheStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("教师缓存统计信息:\n");
        stats.append("- 支持的缓存类型: teachers, teacherDetails, teacherList, teacherMatch\n");
        stats.append("- 预热策略: 热门教师、常用列表、匹配查询\n");
        stats.append("- 清理策略: 每日凌晨3点自动清理\n");
        return stats.toString();
    }
}

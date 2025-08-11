package com.touhouqing.grabteacherbackend.service;

import com.touhouqing.grabteacherbackend.dto.TeacherMatchRequest;
import com.touhouqing.grabteacherbackend.mapper.TeacherMapper;
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
    private TeacherService teacherService;

    @Autowired
    private TeacherMapper teacherMapper;

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
            
            // 预热年级数据
            warmupGradeData();
            
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
            teacherService.getTeacherListWithSubjects(1, 10, null, null, null);
            
            // 预热热门科目的教师列表
            String[] popularSubjects = {"数学", "英语", "语文", "物理", "化学"};
            for (String subject : popularSubjects) {
                teacherService.getTeacherListWithSubjects(1, 10, subject, null, null);
            }
            
            // 预热热门年级的教师列表
            String[] popularGrades = {"高一", "高二", "高三", "初一", "初二", "初三"};
            for (String grade : popularGrades) {
                teacherService.getTeacherListWithSubjects(1, 10, null, grade, null);
            }
            
            log.info("预热教师列表缓存完成");
        } catch (Exception e) {
            log.error("预热教师列表缓存失败", e);
        }
    }

    /**
     * 预热教师匹配缓存
     */
    private void warmupTeacherMatches() {
        try {
            log.info("预热教师匹配缓存...");
            
            // 预热常见的教师匹配请求
            String[] subjects = {"数学", "英语", "语文"};
            String[] grades = {"高一", "高二", "高三"};
            
            for (String subject : subjects) {
                for (String grade : grades) {
                    try {
                        TeacherMatchRequest request = new TeacherMatchRequest();
                        request.setSubject(subject);
                        request.setGrade(grade);
                        request.setLimit(10);
                        
                        teacherService.matchTeachers(request);
                    } catch (Exception e) {
                        log.warn("预热教师匹配缓存失败: {} - {}", subject, grade, e);
                    }
                }
            }
            
            log.info("预热教师匹配缓存完成");
        } catch (Exception e) {
            log.error("预热教师匹配缓存失败", e);
        }
    }

    /**
     * 预热年级数据缓存
     */
    private void warmupGradeData() {
        try {
            log.info("预热年级数据缓存...");
            
            // 预热可用年级列表
            teacherService.getAvailableGrades();
            
            log.info("预热年级数据缓存完成");
        } catch (Exception e) {
            log.error("预热年级数据缓存失败", e);
        }
    }

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
        "teacherAvailability", "grades"
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

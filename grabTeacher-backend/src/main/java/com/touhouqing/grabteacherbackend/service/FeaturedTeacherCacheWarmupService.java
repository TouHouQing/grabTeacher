package com.touhouqing.grabteacherbackend.service;

import com.touhouqing.grabteacherbackend.dto.TeacherListResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 精选教师缓存预热服务
 * 在应用启动时预热精选教师相关的缓存
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Order(3) // 在其他缓存预热服务之后执行
public class FeaturedTeacherCacheWarmupService implements CommandLineRunner {

    private final TeacherService teacherService;
    private final SubjectService subjectService;

    @Override
    public void run(String... args) throws Exception {
        log.info("开始预热精选教师缓存...");
        
        try {
            // 预热精选教师列表缓存
            warmupFeaturedTeachersCache();
            
            log.info("精选教师缓存预热完成");
        } catch (Exception e) {
            log.error("精选教师缓存预热失败", e);
        }
    }

    /**
     * 预热精选教师列表缓存
     */
    private void warmupFeaturedTeachersCache() {
        try {
            // 预热不同页面大小的精选教师列表
            int[] pageSizes = {6, 10, 12, 20};
            
            for (int pageSize : pageSizes) {
                // 预热第一页数据
                List<TeacherListResponseDTO> teachers = teacherService.getFeaturedTeachers(1, pageSize, null, null, null);
                log.debug("预热精选教师缓存: page=1, size={}, count={}", pageSize, teachers.size());
                
                // 如果有数据，预热第二页
                if (teachers.size() == pageSize) {
                    List<TeacherListResponseDTO> secondPage = teacherService.getFeaturedTeachers(2, pageSize, null, null, null);
                    log.debug("预热精选教师缓存: page=2, size={}, count={}", pageSize, secondPage.size());
                }
            }
            
            // 预热科目的精选教师列表（动态获取激活科目）
            List<com.touhouqing.grabteacherbackend.entity.Subject> activeSubjects = subjectService.getAllActiveSubjects();
            for (com.touhouqing.grabteacherbackend.entity.Subject s : activeSubjects) {
                List<TeacherListResponseDTO> teachers = teacherService.getFeaturedTeachers(1, 10, s.getName(), null, null);
                log.debug("预热精选教师缓存: subject={}, count={}", s.getName(), teachers.size());
            }

            // 预热年级的精选教师列表（动态获取可用年级）
            List<String> dbGrades = teacherService.getAvailableGrades();
            for (String grade : dbGrades) {
                List<TeacherListResponseDTO> teachers = teacherService.getFeaturedTeachers(1, 10, null, grade, null);
                log.debug("预热精选教师缓存: grade={}, count={}", grade, teachers.size());
            }

            log.info("精选教师列表缓存预热完成");
        } catch (Exception e) {
            log.error("预热精选教师列表缓存失败", e);
        }
    }
}

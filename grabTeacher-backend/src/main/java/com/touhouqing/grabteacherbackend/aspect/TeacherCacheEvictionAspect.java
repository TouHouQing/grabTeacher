package com.touhouqing.grabteacherbackend.aspect;

import com.touhouqing.grabteacherbackend.service.TeacherCacheWarmupService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 教师缓存失效切面
 * 在教师信息更新时自动清理相关缓存
 */
@Slf4j
@Aspect
@Component
public class TeacherCacheEvictionAspect {

    @Autowired
    @org.springframework.context.annotation.Lazy
    private TeacherCacheWarmupService teacherCacheWarmupService;

    /**
     * 教师信息更新后清理缓存
     */
    @AfterReturning(pointcut = "execution(* com.touhouqing.grabteacherbackend.service.impl.TeacherServiceImpl.updateTeacherInfo(..))", 
                    returning = "result")
    public void clearCacheAfterTeacherUpdate(JoinPoint joinPoint, Object result) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args.length > 0 && args[0] instanceof Long) {
                Long userId = (Long) args[0];
                log.info("教师信息更新后清理缓存，用户ID: {}", userId);
                
                // 清理所有教师相关缓存（因为可能影响列表和匹配结果）
                teacherCacheWarmupService.clearAllTeacherCaches();
            }
        } catch (Exception e) {
            log.error("清理教师更新缓存失败", e);
        }
    }

    /**
     * 教师认证状态变更后清理缓存
     */
    @AfterReturning(pointcut = "execution(* com.touhouqing.grabteacherbackend.service.impl.AdminServiceImpl.verifyTeacher(..))")
    public void clearCacheAfterTeacherVerification(JoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args.length > 0 && args[0] instanceof Long) {
                Long teacherId = (Long) args[0];
                log.info("教师认证状态变更后清理缓存，教师ID: {}", teacherId);

                // 认证状态变更会影响教师列表和匹配结果
                teacherCacheWarmupService.clearAllTeacherCaches();
            }
        } catch (Exception e) {
            log.error("清理教师认证缓存失败", e);
        }
    }

    /**
     * 教师科目信息变更后清理缓存
     */
    @AfterReturning(pointcut = "execution(* com.touhouqing.grabteacherbackend.service.impl.AdminServiceImpl.updateTeacher(..))")
    public void clearCacheAfterSubjectUpdate(JoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args.length > 0 && args[0] instanceof Long) {
                Long teacherId = (Long) args[0];
                log.info("教师科目信息变更后清理缓存，教师ID: {}", teacherId);

                // 科目变更会影响科目相关的查询
                teacherCacheWarmupService.clearAllTeacherCaches();
            }
        } catch (Exception e) {
            log.error("清理教师科目缓存失败", e);
        }
    }



    /**
     * 课程创建后清理教师缓存（因为可能影响教师的课程统计）
     */
    @AfterReturning(pointcut = "execution(* com.touhouqing.grabteacherbackend.service.impl.CourseServiceImpl.createCourse(..))", 
                    returning = "result")
    public void clearCacheAfterCourseCreation(JoinPoint joinPoint, Object result) {
        try {
            // 课程创建可能影响教师的课程数量统计，清理相关缓存
            log.info("课程创建后清理教师相关缓存");
            teacherCacheWarmupService.clearAllTeacherCaches();
        } catch (Exception e) {
            log.error("清理课程创建相关的教师缓存失败", e);
        }
    }

    /**
     * 课程更新后清理教师缓存
     */
    @AfterReturning(pointcut = "execution(* com.touhouqing.grabteacherbackend.service.impl.CourseServiceImpl.updateCourse(..))", 
                    returning = "result")
    public void clearCacheAfterCourseUpdate(JoinPoint joinPoint, Object result) {
        try {
            // 课程更新可能影响教师信息，清理相关缓存
            log.info("课程更新后清理教师相关缓存");
            teacherCacheWarmupService.clearAllTeacherCaches();
        } catch (Exception e) {
            log.error("清理课程更新相关的教师缓存失败", e);
        }
    }

    /**
     * 课程删除后清理教师缓存
     */
    @AfterReturning(pointcut = "execution(* com.touhouqing.grabteacherbackend.service.impl.CourseServiceImpl.deleteCourse(..))")
    public void clearCacheAfterCourseDeletion(JoinPoint joinPoint) {
        try {
            // 课程删除可能影响教师的课程数量统计，清理相关缓存
            log.info("课程删除后清理教师相关缓存");
            teacherCacheWarmupService.clearAllTeacherCaches();
        } catch (Exception e) {
            log.error("清理课程删除相关的教师缓存失败", e);
        }
    }
}

package com.touhouqing.grabteacherbackend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

/**
 * 课程查询专用的缓存Key生成器
 */
@Component("courseCacheKeyGenerator")
@Slf4j
class CourseCacheKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        StringBuilder keyBuilder = new StringBuilder();

        // 根据方法名生成不同的key策略
        String methodName = method.getName();
        keyBuilder.append(methodName).append(":");

        return switch (methodName) {
            case "getCourseList" -> generateCourseListKey(params);
            case "getTeacherCourses" -> generateTeacherCoursesKey(params);
            case "getCourseById" -> generateCourseByIdKey(params);
            case "getActiveCourses" -> "activeCourses:all";
            default -> new CustomCacheKeyGenerator().generate(target, method, params);
        };
    }

    /**
     * 生成课程列表查询的缓存key
     */
    private String generateCourseListKey(Object... params) {
        if (params == null || params.length < 2) {
            return "courseList:default";
        }

        StringBuilder key = new StringBuilder("courseList:");

        // page, size, keyword, subjectId, teacherId, status, courseType, grade
        key.append("page_").append(params[0] != null ? params[0] : "1");
        key.append(":size_").append(params[1] != null ? params[1] : "10");

        if (params.length > 2 && StringUtils.hasText((String) params[2])) {
            key.append(":keyword_").append(params[2]);
        }
        if (params.length > 3 && params[3] != null) {
            key.append(":subject_").append(params[3]);
        }
        if (params.length > 4 && params[4] != null) {
            key.append(":teacher_").append(params[4]);
        }
        if (params.length > 5 && StringUtils.hasText((String) params[5])) {
            key.append(":status_").append(params[5]);
        }
        if (params.length > 6 && StringUtils.hasText((String) params[6])) {
            key.append(":type_").append(params[6]);
        }
        if (params.length > 7 && StringUtils.hasText((String) params[7])) {
            key.append(":grade_").append(params[7]);
        }

        return key.toString();
    }

    /**
     * 生成教师课程查询的缓存key
     */
    private String generateTeacherCoursesKey(Object... params) {
        if (params == null || params.length < 1) {
            return "teacherCourses:unknown";
        }

        StringBuilder key = new StringBuilder("teacherCourses:");
        key.append("teacher_").append(params[0] != null ? params[0] : "null");

        if (params.length > 1) {
            key.append(":user_").append(params[1] != null ? params[1] : "null");
        }
        if (params.length > 2) {
            key.append(":type_").append(params[2] != null ? params[2] : "null");
        }

        return key.toString();
    }

    /**
     * 生成单个课程查询的缓存key
     */
    private String generateCourseByIdKey(Object... params) {
        if (params == null || params.length < 1 || params[0] == null) {
            return "course:unknown";
        }

        return "course:id_" + params[0];
    }
}

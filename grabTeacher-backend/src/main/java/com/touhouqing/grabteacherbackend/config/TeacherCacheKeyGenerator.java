package com.touhouqing.grabteacherbackend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 教师专用缓存Key生成器
 * 针对教师模块的复杂查询条件生成优化的缓存Key
 */
@Slf4j
@Component("teacherCacheKeyGenerator")
public class TeacherCacheKeyGenerator implements KeyGenerator {

    private static final String SEPARATOR = ":";
    private static final String NULL_VALUE = "null";
    private static final int MAX_KEY_LENGTH = 250;

    @Override
    public Object generate(Object target, Method method, Object... params) {
        String methodName = method.getName();
        
        switch (methodName) {
            case "getTeacherDetailById":
                return generateTeacherDetailKey(params);
            case "getTeacherListWithSubjects":
                return generateTeacherListKey(params);
            case "matchTeachers":
                return generateTeacherMatchKey(params);
            case "getTeacherByUserId":
                return generateTeacherByUserIdKey(params);
            case "getTeacherPublicSchedule":
                return generateTeacherScheduleKey(params);
            case "checkTeacherAvailability":
                return generateTeacherAvailabilityKey(params);
            case "getAvailableGrades":
                return "availableGrades:all";
            default:
                return generateDefaultKey(target, method, params);
        }
    }

    /**
     * 生成教师详情缓存Key
     */
    private String generateTeacherDetailKey(Object... params) {
        if (params == null || params.length < 1 || params[0] == null) {
            return "teacherDetail:unknown";
        }
        return "teacherDetail:id_" + params[0];
    }

    /**
     * 生成教师列表缓存Key
     */
    private String generateTeacherListKey(Object... params) {
        if (params == null || params.length < 2) {
            return "teacherList:default";
        }
        
        StringBuilder key = new StringBuilder("teacherList:");
        
        // page, size, subject, grade, keyword
        key.append("page_").append(params[0] != null ? params[0] : "1");
        key.append(":size_").append(params[1] != null ? params[1] : "10");
        
        if (params.length > 2 && StringUtils.hasText((String) params[2])) {
            key.append(":subject_").append(params[2]);
        }
        if (params.length > 3 && StringUtils.hasText((String) params[3])) {
            key.append(":grade_").append(params[3]);
        }
        if (params.length > 4 && StringUtils.hasText((String) params[4])) {
            key.append(":keyword_").append(params[4]);
        }
        
        String finalKey = key.toString();
        return finalKey.length() > MAX_KEY_LENGTH ? generateHashKey(finalKey) : finalKey;
    }

    /**
     * 生成教师匹配缓存Key
     */
    private String generateTeacherMatchKey(Object... params) {
        if (params == null || params.length < 1 || params[0] == null) {
            return "teacherMatch:unknown";
        }
        
        // 假设参数是TeacherMatchRequest对象
        Object request = params[0];
        StringBuilder key = new StringBuilder("teacherMatch:");
        
        try {
            // 使用反射获取请求对象的关键字段
            String subject = getFieldValue(request, "subject");
            String grade = getFieldValue(request, "grade");
            String gender = getFieldValue(request, "genderPreference");
            String budget = getFieldValue(request, "budgetRange");
            Integer limit = getIntFieldValue(request, "limit");
            
            if (StringUtils.hasText(subject)) {
                key.append("subject_").append(subject).append(":");
            }
            if (StringUtils.hasText(grade)) {
                key.append("grade_").append(grade).append(":");
            }
            if (StringUtils.hasText(gender)) {
                key.append("gender_").append(gender).append(":");
            }
            if (StringUtils.hasText(budget)) {
                key.append("budget_").append(budget).append(":");
            }
            if (limit != null) {
                key.append("limit_").append(limit);
            }
            
        } catch (Exception e) {
            log.warn("生成教师匹配缓存Key失败，使用默认策略", e);
            key.append("hash_").append(Math.abs(request.hashCode()));
        }
        
        String finalKey = key.toString();
        return finalKey.length() > MAX_KEY_LENGTH ? generateHashKey(finalKey) : finalKey;
    }

    /**
     * 生成根据用户ID查询教师的缓存Key
     */
    private String generateTeacherByUserIdKey(Object... params) {
        if (params == null || params.length < 1 || params[0] == null) {
            return "teacherByUserId:unknown";
        }
        return "teacherByUserId:userId_" + params[0];
    }

    /**
     * 生成教师课表缓存Key
     */
    private String generateTeacherScheduleKey(Object... params) {
        if (params == null || params.length < 3) {
            return "teacherSchedule:unknown";
        }
        
        StringBuilder key = new StringBuilder("teacherSchedule:");
        key.append("teacher_").append(params[0] != null ? params[0] : "null");
        key.append(":start_").append(params[1] != null ? params[1] : "null");
        key.append(":end_").append(params[2] != null ? params[2] : "null");
        
        return key.toString();
    }

    /**
     * 生成教师可用性检查缓存Key
     */
    private String generateTeacherAvailabilityKey(Object... params) {
        if (params == null || params.length < 3) {
            return "teacherAvailability:unknown";
        }
        
        StringBuilder key = new StringBuilder("teacherAvailability:");
        key.append("teacher_").append(params[0] != null ? params[0] : "null");
        key.append(":start_").append(params[1] != null ? params[1] : "null");
        key.append(":end_").append(params[2] != null ? params[2] : "null");
        
        // 如果有时间段参数
        if (params.length > 3 && params[3] != null) {
            if (params[3] instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> timeSlots = (List<String>) params[3];
                if (!timeSlots.isEmpty()) {
                    key.append(":slots_").append(String.join(",", timeSlots));
                }
            }
        }
        
        String finalKey = key.toString();
        return finalKey.length() > MAX_KEY_LENGTH ? generateHashKey(finalKey) : finalKey;
    }

    /**
     * 生成默认缓存Key
     */
    private String generateDefaultKey(Object target, Method method, Object... params) {
        StringBuilder key = new StringBuilder();
        key.append(target.getClass().getSimpleName()).append(":");
        key.append(method.getName()).append(":");
        
        if (params != null && params.length > 0) {
            String paramsKey = Arrays.stream(params)
                    .map(param -> param != null ? param.toString() : NULL_VALUE)
                    .collect(Collectors.joining(SEPARATOR));
            key.append(paramsKey);
        } else {
            key.append("noParams");
        }
        
        String finalKey = key.toString();
        return finalKey.length() > MAX_KEY_LENGTH ? generateHashKey(finalKey) : finalKey;
    }

    /**
     * 生成MD5哈希Key
     */
    private String generateHashKey(String originalKey) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(originalKey.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return "hash_" + sb.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("MD5算法不可用，使用hashCode替代", e);
            return "hash_" + Math.abs(originalKey.hashCode());
        }
    }

    /**
     * 通过反射获取对象字段值
     */
    private String getFieldValue(Object obj, String fieldName) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(obj);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 通过反射获取对象整数字段值
     */
    private Integer getIntFieldValue(Object obj, String fieldName) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(obj);
            return value instanceof Integer ? (Integer) value : null;
        } catch (Exception e) {
            return null;
        }
    }
}

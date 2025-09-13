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

        return switch (methodName) {
            case "getTeacherDetailById" -> generateTeacherDetailKey(params);
            case "getTeacherListWithSubjects" -> generateTeacherListKey(params);
            case "matchTeachers" -> generateTeacherMatchKey(params);
            case "getTeacherByUserId" -> generateTeacherByUserIdKey(params);
            case "getTeacherPublicSchedule" -> generateTeacherScheduleKey(params);
            case "checkTeacherAvailability" -> generateTeacherAvailabilityKey(params);
            default -> generateDefaultKey(target, method, params);
        };
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

        StringBuilder key = new StringBuilder("teacherList:v2:");

        // page, size, subject, keyword
        key.append("page_").append(params[0] != null ? params[0] : "1");
        key.append(":size_").append(params[1] != null ? params[1] : "10");

        if (params.length > 2 && params[2] instanceof String && StringUtils.hasText((String) params[2])) {
            key.append(":subject_").append(params[2]);
        }
        if (params.length > 3 && params[3] instanceof String && StringUtils.hasText((String) params[3])) {
            key.append(":keyword_").append(((String) params[3]).trim().replaceAll("\\s+", " "));
        }

        String finalKey = key.toString();
        return finalKey.length() > MAX_KEY_LENGTH ? generateHashKey(finalKey) : finalKey;
    }

    /**
     * 生成教师匹配缓存Key
     * 注意：必须包含时间偏好相关字段，否则不同时间偏好的请求会命中同一缓存
     */
    private String generateTeacherMatchKey(Object... params) {
        if (params == null || params.length < 1 || params[0] == null) {
            return "teacherMatch:unknown";
        }

        // 参数应为 TeacherMatchDTO
        Object request = params[0];
        StringBuilder key = new StringBuilder("teacherMatch:");

        try {
            // 基础字段
            String subject = getFieldValue(request, "subject");
            String grade = getFieldValue(request, "grade");
            String preferredGender = getFieldValue(request, "preferredGender");
            String preferredDateStart = getFieldValue(request, "preferredDateStart");
            String preferredDateEnd = getFieldValue(request, "preferredDateEnd");
            String teacherLevel = getFieldValue(request, "teacherLevel");
            Integer limit = getIntFieldValue(request, "limit");

            // 列表字段（星期几与时间段）
            String weekdays = getListFieldAsString(request, "preferredWeekdays");
            String timeSlots = getListFieldAsString(request, "preferredTimeSlots");

            if (StringUtils.hasText(subject)) key.append("subject_").append(subject).append(":");
            if (StringUtils.hasText(grade)) key.append("grade_").append(grade).append(":");
            if (StringUtils.hasText(preferredGender)) key.append("gender_").append(preferredGender).append(":");
            if (StringUtils.hasText(teacherLevel)) key.append("level_").append(teacherLevel.trim()).append(":");
            if (StringUtils.hasText(weekdays)) key.append("wd_").append(weekdays).append(":");
            if (StringUtils.hasText(timeSlots)) key.append("ts_").append(timeSlots).append(":");
            if (StringUtils.hasText(preferredDateStart)) key.append("ds_").append(preferredDateStart).append(":");
            if (StringUtils.hasText(preferredDateEnd)) key.append("de_").append(preferredDateEnd).append(":");
            if (limit != null) key.append("limit_").append(limit);

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

    /**
     * 通过反射将 List 字段转为稳定的字符串（排序后逗号拼接）
     */
    @SuppressWarnings("unchecked")
    private String getListFieldAsString(Object obj, String fieldName) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(obj);
            if (value instanceof List) {
                List<Object> list = (List<Object>) value;
                if (list.isEmpty()) {
                    return null;
                }
                List<String> items = list.stream()
                        .filter(java.util.Objects::nonNull)
                        .map(Object::toString)
                        .sorted()
                        .collect(Collectors.toList());
                return String.join("-", items);
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }
}

package com.touhouqing.grabteacherbackend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 自定义缓存Key生成器
 * 支持复杂查询条件的智能缓存Key生成
 */
@Slf4j
@Component("customCacheKeyGenerator")
public class CustomCacheKeyGenerator implements KeyGenerator {

    private static final String SEPARATOR = ":";
    private static final String NULL_VALUE = "null";
    private static final int MAX_KEY_LENGTH = 250; // Redis key最大长度限制

    @Override
    public Object generate(Object target, Method method, Object... params) {
        StringBuilder keyBuilder = new StringBuilder();
        
        // 添加类名
        keyBuilder.append(target.getClass().getSimpleName()).append(SEPARATOR);
        
        // 添加方法名
        keyBuilder.append(method.getName()).append(SEPARATOR);
        
        // 处理参数
        if (params != null && params.length > 0) {
            String paramsKey = generateParamsKey(params);
            keyBuilder.append(paramsKey);
        } else {
            keyBuilder.append("noParams");
        }
        
        String finalKey = keyBuilder.toString();
        
        // 如果key太长，使用MD5哈希
        if (finalKey.length() > MAX_KEY_LENGTH) {
            finalKey = generateHashKey(finalKey);
            log.debug("生成的缓存key过长，使用MD5哈希: {}", finalKey);
        }
        
        log.debug("生成缓存key: {}", finalKey);
        return finalKey;
    }

    /**
     * 生成参数部分的key
     */
    private String generateParamsKey(Object... params) {
        return Arrays.stream(params)
                .map(this::convertParamToString)
                .collect(Collectors.joining(SEPARATOR));
    }

    /**
     * 将参数转换为字符串
     */
    private String convertParamToString(Object param) {
        if (param == null) {
            return NULL_VALUE;
        }
        
        // 处理基本类型
        if (param instanceof String || param instanceof Number || param instanceof Boolean) {
            return param.toString();
        }
        
        // 处理枚举
        if (param instanceof Enum) {
            return param.toString();
        }
        
        // 处理数组
        if (param.getClass().isArray()) {
            return Arrays.toString((Object[]) param);
        }
        
        // 处理复杂对象，使用toString或者特定字段
        return generateObjectKey(param);
    }

    /**
     * 为复杂对象生成key
     */
    private String generateObjectKey(Object obj) {
        if (obj == null) {
            return NULL_VALUE;
        }
        
        // 可以根据具体的业务对象类型进行特殊处理
        String className = obj.getClass().getSimpleName();
        String objectString = obj.toString();
        
        // 如果toString太长，使用hashCode
        if (objectString.length() > 100) {
            return className + "_" + Math.abs(obj.hashCode());
        }
        
        return className + "_" + objectString;
    }

    /**
     * 生成MD5哈希key
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
}


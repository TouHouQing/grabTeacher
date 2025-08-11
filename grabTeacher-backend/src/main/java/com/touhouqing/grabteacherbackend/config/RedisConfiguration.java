package com.touhouqing.grabteacherbackend.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis缓存配置类
 * 提供高性能的Redis缓存解决方案
 */
@Slf4j
@Configuration
@EnableCaching
public class RedisConfiguration {

    @Value("${spring.cache.redis.time-to-live:PT30M}")
    private Duration defaultTtl;

    /**
     * 配置RedisTemplate
     * 使用优化的序列化器提升性能
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();

        // 设置连接工厂
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // 创建优化的JSON序列化器
        GenericJackson2JsonRedisSerializer jsonSerializer = createOptimizedJsonSerializer();
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // 设置key和value的序列化器
        redisTemplate.setKeySerializer(stringSerializer);
        redisTemplate.setValueSerializer(jsonSerializer);
        redisTemplate.setHashKeySerializer(stringSerializer);
        redisTemplate.setHashValueSerializer(jsonSerializer);

        // 启用事务支持
        redisTemplate.setEnableTransactionSupport(true);

        // 初始化
        redisTemplate.afterPropertiesSet();

        log.info("RedisTemplate配置完成，使用优化的序列化器");
        return redisTemplate;
    }

    /**
     * 配置缓存管理器
     * 支持多种缓存策略和TTL配置
     */
    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        // 创建优化的JSON序列化器
        GenericJackson2JsonRedisSerializer jsonSerializer = createOptimizedJsonSerializer();

        // 默认缓存配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(defaultTtl) // 默认30分钟过期
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues() // 不缓存null值
                .computePrefixWith(cacheName -> "grabTeacher:cache:" + cacheName + ":");

        // 针对不同业务场景的缓存配置
        Map<String, RedisCacheConfiguration> cacheConfigurations = createCacheConfigurations(jsonSerializer);

        RedisCacheManager cacheManager = RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware() // 支持事务
                .build();

        log.info("RedisCacheManager配置完成，支持{}种缓存策略", cacheConfigurations.size());
        return cacheManager;
    }

    /**
     * 创建优化的JSON序列化器
     */
    private GenericJackson2JsonRedisSerializer createOptimizedJsonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();

        // 配置可见性
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // 启用类型信息以支持多态
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        // 注册Java 8时间模块
        objectMapper.registerModule(new JavaTimeModule());

        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

    /**
     * 创建不同业务场景的缓存配置
     */
    private Map<String, RedisCacheConfiguration> createCacheConfigurations(GenericJackson2JsonRedisSerializer jsonSerializer) {
        Map<String, RedisCacheConfiguration> configs = new HashMap<>();

        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // 课程相关缓存 - 30分钟过期
        configs.put("course", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .computePrefixWith(cacheName -> "grabTeacher:course:"));

        // 课程列表缓存 - 15分钟过期（更新频繁）
        configs.put("courseList", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .computePrefixWith(cacheName -> "grabTeacher:courseList:"));

        // 教师课程缓存 - 20分钟过期
        configs.put("teacherCourses", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(20))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .computePrefixWith(cacheName -> "grabTeacher:teacherCourses:"));

        // 活跃课程缓存 - 10分钟过期（高频访问）
        configs.put("activeCourses", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .computePrefixWith(cacheName -> "grabTeacher:activeCourses:"));

        // 科目和年级缓存 - 1小时过期（变化较少）
        configs.put("subjects", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .computePrefixWith(cacheName -> "grabTeacher:subjects:"));

        configs.put("grades", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .computePrefixWith(cacheName -> "grabTeacher:grades:"));

        // 教师相关缓存 - 25分钟过期
        configs.put("teachers", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(25))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .computePrefixWith(cacheName -> "grabTeacher:teachers:"));

        // 教师详情缓存 - 30分钟过期
        configs.put("teacherDetails", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .computePrefixWith(cacheName -> "grabTeacher:teacherDetails:"));

        // 教师列表缓存 - 15分钟过期（更新频繁）
        configs.put("teacherList", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .computePrefixWith(cacheName -> "grabTeacher:teacherList:"));

        // 教师匹配缓存 - 10分钟过期（高频查询）
        configs.put("teacherMatch", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .computePrefixWith(cacheName -> "grabTeacher:teacherMatch:"));

        // 教师科目关联缓存 - 45分钟过期（变化较少）
        configs.put("teacherSubjects", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(45))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .computePrefixWith(cacheName -> "grabTeacher:teacherSubjects:"));

        // 教师时间表缓存 - 5分钟过期（实时性要求高）
        configs.put("teacherSchedule", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .computePrefixWith(cacheName -> "grabTeacher:teacherSchedule:"));

        // 教师可用性缓存 - 3分钟过期（实时性要求很高）
        configs.put("teacherAvailability", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(3))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .computePrefixWith(cacheName -> "grabTeacher:teacherAvailability:"));

        return configs;
    }
}

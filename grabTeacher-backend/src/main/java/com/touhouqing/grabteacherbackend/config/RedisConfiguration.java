package com.touhouqing.grabteacherbackend.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

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

        // 默认关闭全局事务支持，避免性能损耗；如需事务请使用局部SessionCallback
        // redisTemplate.setEnableTransactionSupport(true);

        // 初始化
        redisTemplate.afterPropertiesSet();

        log.info("RedisTemplate配置完成，使用优化的序列化器");
        return redisTemplate;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
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

    @Bean
    public org.springframework.cache.interceptor.CacheErrorHandler cacheErrorHandler() {
        return new org.springframework.cache.interceptor.SimpleCacheErrorHandler();
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

        // 注册Java 8时间模块与 Afterburner 加速模块
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(new AfterburnerModule());

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

        // 活跃课程缓存 - 分层：全量 10 分钟，限量 5 分钟（热点）
        configs.put("activeCoursesAll", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .computePrefixWith(cacheName -> "grabTeacher:activeCoursesAll:"));
        configs.put("activeCoursesLimited", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .computePrefixWith(cacheName -> "grabTeacher:activeCoursesLimited:"));

        // 兼容旧键（如仍被引用，可保留一段时间）；逐步迁移
        configs.put("activeCourses", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .computePrefixWith(cacheName -> "grabTeacher:activeCourses:"));

        // 精选课程缓存 - 20分钟过期（管理员设置，更新频率中等）
        configs.put("featuredCourses", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(20))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .computePrefixWith(cacheName -> "grabTeacher:featuredCourses:"));

        // 精选课程ID列表缓存 - 30分钟过期（变化较少）
        configs.put("featuredCourseIds", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .computePrefixWith(cacheName -> "grabTeacher:featuredCourseIds:"));

        // 科目缓存 - 1小时过期（变化较少）
        configs.put("subjects", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .computePrefixWith(cacheName -> "grabTeacher:subjects:"));


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

        // 缓存异常降级：任何缓存读写异常都仅记录日志，不影响主业务逻辑
        // 声明在配置类方法之后（下面有 @Bean cacheErrorHandler）

        // 继续配置其他缓存...


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

        // 教师时间表缓存 - 基础 5 分钟 TTL + 0~10% 抖动
        configs.put("teacherSchedule", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(jitter(Duration.ofMinutes(5)))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .computePrefixWith(cacheName -> "grabTeacher:teacherSchedule:"));

        // 教师月度基础段状态缓存 - 基础 5 分钟 TTL + 0~10% 抖动
        configs.put("teacherMonthlyCalendar", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(jitter(Duration.ofMinutes(5)))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .computePrefixWith(cacheName -> "grabTeacher:teacherMonthlyCalendar:"));


        // 教师可用性缓存 - 基础 3 分钟 TTL + 0~10% 抖动
        configs.put("teacherAvailability", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(jitter(Duration.ofMinutes(3)))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .computePrefixWith(cacheName -> "grabTeacher:teacherAvailability:"));

        // 教师可上课时间配置缓存（教师在个人中心设置的可上课时间）- 基础 5 分钟 TTL + 0~10% 抖动
        configs.put("teacherAvailableTime", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(jitter(Duration.ofMinutes(5)))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .computePrefixWith(cacheName -> "grabTeacher:teacherAvailableTime:"));

        // 公共：管理员联系方式 - 30 分钟 TTL（变化极少）
        configs.put("public:adminContacts", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .computePrefixWith(cacheName -> "grabTeacher:public:adminContacts:"));

        // 留学相关缓存 - 国家/阶段 60分钟，项目列表 20分钟，公开列表 10分钟
        configs.put("abroad:countries:list", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .computePrefixWith(cacheName -> "grabTeacher:abroad:countries:list:"));
        configs.put("abroad:countries:active", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .computePrefixWith(cacheName -> "grabTeacher:abroad:countries:active:"));
        configs.put("abroad:countries:get", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .computePrefixWith(cacheName -> "grabTeacher:abroad:countries:get:"));

        configs.put("abroad:stages:list", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .computePrefixWith(cacheName -> "grabTeacher:abroad:stages:list:"));
        configs.put("abroad:stages:active", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .computePrefixWith(cacheName -> "grabTeacher:abroad:stages:active:"));
        configs.put("abroad:stages:get", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .computePrefixWith(cacheName -> "grabTeacher:abroad:stages:get:"));

        configs.put("abroad:programs:list", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(20))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .computePrefixWith(cacheName -> "grabTeacher:abroad:programs:list:"));
        configs.put("abroad:programs:active", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .computePrefixWith(cacheName -> "grabTeacher:abroad:programs:active:"));
        configs.put("abroad:programs:get", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(20))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .computePrefixWith(cacheName -> "grabTeacher:abroad:programs:get:"));

        return configs;
    }

    // 简单的 TTL 抖动：0~10% 的随机偏移（应用级别，非逐 entry）
    private Duration jitter(Duration base) {
        long maxJitter = Math.max(1, base.toMillis() / 10);
        long delta = ThreadLocalRandom.current().nextLong(maxJitter + 1);
        return base.plusMillis(delta);
    }
}

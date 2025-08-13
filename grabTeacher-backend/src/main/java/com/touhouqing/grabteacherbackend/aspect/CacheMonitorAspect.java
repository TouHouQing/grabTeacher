package com.touhouqing.grabteacherbackend.aspect;

import com.touhouqing.grabteacherbackend.service.CacheMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 缓存监控切面
 * 自动记录缓存操作的统计信息
 */
@Slf4j
@Aspect
@Component
public class CacheMonitorAspect {

    @Autowired
    private CacheMonitorService cacheMonitorService;

    /**
     * 监控@Cacheable注解的方法
     */
    @Around("@annotation(org.springframework.cache.annotation.Cacheable)")
    public Object monitorCacheable(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        try {
            Object result = joinPoint.proceed();
            // 获取@Cacheable注解信息
            Method method = getMethod(joinPoint);
            if (method != null) {
                Cacheable cacheable = method.getAnnotation(Cacheable.class);
                if (cacheable != null) {
                    String[] cacheNames = cacheable.cacheNames().length > 0 ?
                                         cacheable.cacheNames() : cacheable.value();

                    // 记录缓存操作
                    for (String cacheName : cacheNames) {
                        if (result != null) {
                            // 如果有结果，说明缓存未命中，从数据库查询了
                            cacheMonitorService.recordCacheMiss(cacheName);
                            cacheMonitorService.recordCachePut(cacheName);
                        } else {
                            // 结果为null，可能是缓存命中但返回null
                            cacheMonitorService.recordCacheHit(cacheName);
                        }
                    }
                }
            }

            return result;
        } catch (Exception e) {
            log.error("缓存监控异常: {}.{}", className, methodName, e);
            throw e;
        }
    }

    /**
     * 监控@CacheEvict注解的方法
     */
    @Around("@annotation(org.springframework.cache.annotation.CacheEvict)")
    public Object monitorCacheEvict(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        try {
            Object result = joinPoint.proceed();
            // 获取@CacheEvict注解信息
            Method method = getMethod(joinPoint);
            if (method != null) {
                CacheEvict cacheEvict = method.getAnnotation(CacheEvict.class);
                if (cacheEvict != null) {
                    String[] cacheNames = cacheEvict.cacheNames().length > 0 ?
                                         cacheEvict.cacheNames() : cacheEvict.value();

                    // 记录缓存清除
                    for (String cacheName : cacheNames) {
                        cacheMonitorService.recordCacheEviction(cacheName);
                    }
                }
            }

            return result;
        } catch (Exception e) {
            log.error("缓存清除监控异常: {}.{}", className, methodName, e);
            throw e;
        }
    }

    /**
     * 监控@CachePut注解的方法
     */
    @Around("@annotation(org.springframework.cache.annotation.CachePut)")
    public Object monitorCachePut(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        try {
            Object result = joinPoint.proceed();
            // 获取@CachePut注解信息
            Method method = getMethod(joinPoint);
            if (method != null) {
                CachePut cachePut = method.getAnnotation(CachePut.class);
                if (cachePut != null) {
                    String[] cacheNames = cachePut.cacheNames().length > 0 ?
                                         cachePut.cacheNames() : cachePut.value();

                    // 记录缓存写入
                    for (String cacheName : cacheNames) {
                        cacheMonitorService.recordCachePut(cacheName);
                    }
                }
            }

            return result;
        } catch (Exception e) {
            log.error("缓存写入监控异常: {}.{}", className, methodName, e);
            throw e;
        }
    }

    /**
     * 监控@Caching注解的方法
     */
    @Around("@annotation(org.springframework.cache.annotation.Caching)")
    public Object monitorCaching(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        try {
            Object result = joinPoint.proceed();
            
            return result;
        } catch (Exception e) {
            log.error("复合缓存操作监控异常: {}.{}", className, methodName, e);
            throw e;
        }
    }

    /**
     * 获取方法对象
     */
    private Method getMethod(ProceedingJoinPoint joinPoint) {
        try {
            String methodName = joinPoint.getSignature().getName();

            // 直接从方法签名获取方法对象
            if (joinPoint.getSignature() instanceof org.aspectj.lang.reflect.MethodSignature) {
                org.aspectj.lang.reflect.MethodSignature methodSignature =
                    (org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature();
                return methodSignature.getMethod();
            }

            // 备用方案：通过反射获取
            Object[] args = joinPoint.getArgs();
            Class<?>[] paramTypes = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null) {
                    paramTypes[i] = args[i].getClass();
                    // 处理基本类型的包装类
                    if (paramTypes[i] == Integer.class) {
                        paramTypes[i] = int.class;
                    } else if (paramTypes[i] == Long.class) {
                        paramTypes[i] = Long.class; // Long保持不变
                    } else if (paramTypes[i] == Boolean.class) {
                        paramTypes[i] = boolean.class;
                    }
                } else {
                    paramTypes[i] = Object.class;
                }
            }
            return joinPoint.getTarget().getClass().getMethod(methodName, paramTypes);
        } catch (Exception e) {
            log.warn("获取方法对象失败: {}", e.getMessage());
            return null;
        }
    }
}

package com.touhouqing.grabteacherbackend.controller;

import com.touhouqing.grabteacherbackend.common.result.CommonResult;
import com.touhouqing.grabteacherbackend.model.entity.Subject;
import com.touhouqing.grabteacherbackend.service.SubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.touhouqing.grabteacherbackend.cache.ActiveSubjectsLocalCache;
import com.touhouqing.grabteacherbackend.config.PublicJsonCacheConfig;

import java.util.List;

@RestController
@RequestMapping("/api/public/subjects")
@Tag(name = "公共科目接口", description = "供所有用户访问的科目相关接口")
public class PublicSubjectController {

    private static final Logger logger = LoggerFactory.getLogger(PublicSubjectController.class);

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ActiveSubjectsLocalCache activeSubjectsLocalCache;

    @Autowired
    private PublicJsonCacheConfig publicJsonCacheConfig;

    // 单飞锁，避免并发回源击穿
    private final java.util.concurrent.ConcurrentHashMap<String, Object> keyLocks = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 获取所有激活的科目列表
     */
    @Operation(summary = "获取激活科目列表", description = "获取所有状态为激活的科目列表，供课程创建等功能使用")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping(value = "/active", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAllActiveSubjects() {
        try {
            final String cacheKey = "public:subjects:active:json";

            // L1 本地缓存
            String json = activeSubjectsLocalCache.get(cacheKey);
            if (json != null) {
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
            }

            // 单飞锁，避免并发回源击穿
            Object lock = keyLocks.computeIfAbsent(cacheKey, k -> new Object());
            synchronized (lock) {
                // 双检 L1
                json = activeSubjectsLocalCache.get(cacheKey);
                if (json != null) {
                    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
                }

                // L2 Redis
                json = stringRedisTemplate.opsForValue().get(cacheKey);
                if (json != null) {
                    activeSubjectsLocalCache.put(cacheKey, json);
                    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
                }

                // 回源
                List<Subject> subjects = subjectService.getAllActiveSubjects();
                json = objectMapper.writeValueAsString(CommonResult.success("获取激活科目成功", subjects));

                // 回填两级
                activeSubjectsLocalCache.put(cacheKey, json);
                stringRedisTemplate.opsForValue().set(cacheKey, json, publicJsonCacheConfig.getActiveSubjectsTtl());

                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
            }
        } catch (Exception e) {
            logger.error("获取激活科目异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败，请稍后重试"));
        }
    }

    /**
     * 根据ID获取科目详情
     */
    @Operation(summary = "获取科目详情", description = "根据科目ID获取科目详细信息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "科目不存在")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CommonResult<Subject>> getSubjectById(@PathVariable Long id) {
        try {
            Subject subject = subjectService.getSubjectById(id);
            if (subject == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(CommonResult.error("科目不存在"));
            }
            return ResponseEntity.ok(CommonResult.success("获取科目成功", subject));
        } catch (Exception e) {
            logger.error("获取科目异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败，请稍后重试"));
        }
    }
}

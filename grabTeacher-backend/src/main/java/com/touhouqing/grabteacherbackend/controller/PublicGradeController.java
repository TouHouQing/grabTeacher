package com.touhouqing.grabteacherbackend.controller;

import com.touhouqing.grabteacherbackend.common.result.CommonResult;
import com.touhouqing.grabteacherbackend.model.vo.GradeVO;
import com.touhouqing.grabteacherbackend.service.GradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.touhouqing.grabteacherbackend.cache.GradesLocalCache;
import com.touhouqing.grabteacherbackend.config.PublicJsonCacheConfig;

import java.util.List;

/**
 * 公开年级接口控制器
 * 提供不需要认证的年级相关接口
 */
@Slf4j
@RestController
@RequestMapping("/api/public/grades")
@RequiredArgsConstructor
@Tag(name = "公开年级接口", description = "提供公开访问的年级相关接口")
public class PublicGradeController {

    private final GradeService gradeService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final GradesLocalCache gradesLocalCache;
    private final com.touhouqing.grabteacherbackend.config.PublicJsonCacheConfig publicJsonCacheConfig;

    // 单飞锁，避免缓存击穿
    private final java.util.concurrent.ConcurrentHashMap<String, Object> keyLocks = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 获取所有年级列表（公开接口）
     */
    @Operation(summary = "获取所有年级列表", description = "获取系统中所有年级信息，无需认证")
    @GetMapping
    public ResponseEntity<CommonResult<List<GradeVO>>> getAllGrades() {
        try {
            List<GradeVO> grades = gradeService.getAllGrades();
            return ResponseEntity.ok(CommonResult.success("获取成功", grades));
        } catch (Exception e) {
            log.error("获取年级列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }

    /**
     * 获取年级名称列表（公开接口）- JSON 文本 L1+L2 缓存
     */
    @Operation(summary = "获取年级名称列表", description = "获取所有年级的名称列表，用于下拉选择")
    @GetMapping(value = "/names", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getGradeNames() {
        final String cacheKey = "public:grades:names:json";
        try {
            // L1 本地缓存（JSON文本）
            String json = gradesLocalCache.get(cacheKey);
            if (json != null) {
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
            }
            // 单飞锁：避免并发击穿
            Object lock = keyLocks.computeIfAbsent(cacheKey, k -> new Object());
            synchronized (lock) {
                // 双检 L1
                json = gradesLocalCache.get(cacheKey);
                if (json != null) {
                    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
                }
                // L2 Redis
                json = stringRedisTemplate.opsForValue().get(cacheKey);
                if (json != null) {
                    gradesLocalCache.put(cacheKey, json);
                    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
                }
                // 回源
                List<GradeVO> grades = gradeService.getAllGrades();
                List<String> gradeNames = grades.stream().map(GradeVO::getGradeName).toList();
                json = objectMapper.writeValueAsString(CommonResult.success("获取成功", gradeNames));
                gradesLocalCache.put(cacheKey, json);
                stringRedisTemplate.opsForValue().set(cacheKey, json, publicJsonCacheConfig.getGradeNamesTtl());
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
            }
        } catch (Exception e) {
            log.error("获取年级名称列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }
}

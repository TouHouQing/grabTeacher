package com.touhouqing.grabteacherbackend.controller;

import com.touhouqing.grabteacherbackend.service.CacheMonitorService;
import com.touhouqing.grabteacherbackend.service.CacheWarmupService;
import com.touhouqing.grabteacherbackend.service.TeacherCacheWarmupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 缓存管理控制器
 * 提供缓存监控、预热、清理等管理功能
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/cache")
@Tag(name = "缓存管理", description = "缓存监控、预热、清理等管理功能")
@PreAuthorize("hasRole('ADMIN')")
public class CacheManagementController {

    @Autowired
    private CacheMonitorService cacheMonitorService;

    @Autowired
    private CacheWarmupService cacheWarmupService;

    @Autowired
    private TeacherCacheWarmupService teacherCacheWarmupService;

    /**
     * 获取缓存统计报告
     */
    @GetMapping("/stats")
    @Operation(summary = "获取缓存统计报告", description = "获取详细的缓存命中率、性能统计等信息")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        try {
            Map<String, Object> stats = cacheMonitorService.getCacheStatsReport();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("获取缓存统计失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "获取缓存统计失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 获取缓存性能报告（文本格式）
     */
    @GetMapping("/performance-report")
    @Operation(summary = "获取缓存性能报告", description = "获取文本格式的缓存性能报告")
    public ResponseEntity<Map<String, String>> getPerformanceReport() {
        try {
            String report = cacheMonitorService.getCachePerformanceReport();
            Map<String, String> response = new HashMap<>();
            response.put("report", report);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取缓存性能报告失败", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "获取缓存性能报告失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 手动触发缓存预热
     */
    @PostMapping("/warmup")
    @Operation(summary = "手动触发缓存预热", description = "手动执行缓存预热操作，提升系统性能")
    public ResponseEntity<Map<String, String>> warmupCache() {
        try {
            cacheWarmupService.manualWarmup();
            Map<String, String> response = new HashMap<>();
            response.put("message", "缓存预热已启动，请稍后查看统计信息");
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("缓存预热失败", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "缓存预热失败: " + e.getMessage());
            error.put("status", "error");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 清理所有缓存
     */
    @DeleteMapping("/clear-all")
    @Operation(summary = "清理所有缓存", description = "清理所有课程相关缓存，慎用！")
    public ResponseEntity<Map<String, String>> clearAllCaches() {
        try {
            cacheWarmupService.clearAllCourseCaches();
            Map<String, String> response = new HashMap<>();
            response.put("message", "所有缓存已清理");
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("清理缓存失败", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "清理缓存失败: " + e.getMessage());
            error.put("status", "error");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 清理特定课程的缓存
     */
    @DeleteMapping("/clear-course/{courseId}")
    @Operation(summary = "清理特定课程缓存", description = "清理指定课程ID的相关缓存")
    public ResponseEntity<Map<String, String>> clearCourseCache(
            @Parameter(description = "课程ID", required = true)
            @PathVariable Long courseId) {
        try {
            cacheWarmupService.clearCourseCache(courseId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "课程 " + courseId + " 的缓存已清理");
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("清理课程缓存失败", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "清理课程缓存失败: " + e.getMessage());
            error.put("status", "error");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 清理特定教师的课程缓存
     */
    @DeleteMapping("/clear-teacher-courses/{teacherId}")
    @Operation(summary = "清理特定教师课程缓存", description = "清理指定教师ID的课程相关缓存")
    public ResponseEntity<Map<String, String>> clearTeacherCoursesCache(
            @Parameter(description = "教师ID", required = true)
            @PathVariable Long teacherId) {
        try {
            cacheWarmupService.clearTeacherCoursesCache(teacherId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "教师 " + teacherId + " 的课程缓存已清理");
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("清理教师课程缓存失败", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "清理教师课程缓存失败: " + e.getMessage());
            error.put("status", "error");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 重置缓存统计数据
     */
    @PostMapping("/reset-stats")
    @Operation(summary = "重置缓存统计数据", description = "重置所有缓存的统计计数器")
    public ResponseEntity<Map<String, String>> resetCacheStats() {
        try {
            cacheMonitorService.resetStats();
            Map<String, String> response = new HashMap<>();
            response.put("message", "缓存统计数据已重置");
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("重置缓存统计失败", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "重置缓存统计失败: " + e.getMessage());
            error.put("status", "error");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 获取缓存健康状态
     */
    @GetMapping("/health")
    @Operation(summary = "获取缓存健康状态", description = "检查缓存系统的健康状态")
    public ResponseEntity<Map<String, Object>> getCacheHealth() {
        try {
            Map<String, Object> health = new HashMap<>();
            
            // 检查Redis连接
            try {
                String ping = cacheWarmupService.getCacheStats();
                health.put("redis", "healthy");
                health.put("redisResponse", "connected");
            } catch (Exception e) {
                health.put("redis", "unhealthy");
                health.put("redisError", e.getMessage());
            }
            
            // 获取基本统计
            Map<String, Object> stats = cacheMonitorService.getCacheStatsReport();
            health.put("cacheStats", stats.get("overallStats"));
            health.put("cacheSizes", stats.get("cacheSizes"));
            
            health.put("status", "healthy");
            health.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("获取缓存健康状态失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "unhealthy");
            error.put("error", e.getMessage());
            error.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 手动触发教师缓存预热
     */
    @PostMapping("/warmup-teachers")
    @Operation(summary = "手动触发教师缓存预热", description = "手动执行教师缓存预热操作")
    public ResponseEntity<Map<String, String>> warmupTeacherCache() {
        try {
            teacherCacheWarmupService.manualWarmupTeachers();
            Map<String, String> response = new HashMap<>();
            response.put("message", "教师缓存预热已启动");
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("教师缓存预热失败", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "教师缓存预热失败: " + e.getMessage());
            error.put("status", "error");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 清理所有教师缓存
     */
    @DeleteMapping("/clear-teachers")
    @Operation(summary = "清理所有教师缓存", description = "清理所有教师相关缓存")
    public ResponseEntity<Map<String, String>> clearAllTeacherCaches() {
        try {
            teacherCacheWarmupService.clearAllTeacherCaches();
            Map<String, String> response = new HashMap<>();
            response.put("message", "所有教师缓存已清理");
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("清理教师缓存失败", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "清理教师缓存失败: " + e.getMessage());
            error.put("status", "error");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 清理特定教师的缓存
     */
    @DeleteMapping("/clear-teacher/{teacherId}")
    @Operation(summary = "清理特定教师缓存", description = "清理指定教师ID的相关缓存")
    public ResponseEntity<Map<String, String>> clearSpecificTeacherCache(
            @Parameter(description = "教师ID", required = true)
            @PathVariable Long teacherId) {
        try {
            teacherCacheWarmupService.clearTeacherCache(teacherId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "教师 " + teacherId + " 的缓存已清理");
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("清理教师缓存失败", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "清理教师缓存失败: " + e.getMessage());
            error.put("status", "error");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 清理特定科目的教师缓存
     */
    @DeleteMapping("/clear-subject-teachers/{subject}")
    @Operation(summary = "清理特定科目教师缓存", description = "清理指定科目的教师相关缓存")
    public ResponseEntity<Map<String, String>> clearSubjectTeacherCache(
            @Parameter(description = "科目名称", required = true)
            @PathVariable String subject) {
        try {
            teacherCacheWarmupService.clearSubjectTeacherCache(subject);
            Map<String, String> response = new HashMap<>();
            response.put("message", "科目 " + subject + " 的教师缓存已清理");
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("清理科目教师缓存失败", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "清理科目教师缓存失败: " + e.getMessage());
            error.put("status", "error");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 获取教师缓存统计信息
     */
    @GetMapping("/teacher-cache-stats")
    @Operation(summary = "获取教师缓存统计信息", description = "获取教师缓存的详细统计信息")
    public ResponseEntity<Map<String, String>> getTeacherCacheStats() {
        try {
            String stats = teacherCacheWarmupService.getTeacherCacheStats();
            Map<String, String> response = new HashMap<>();
            response.put("stats", stats);
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取教师缓存统计失败", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "获取教师缓存统计失败: " + e.getMessage());
            error.put("status", "error");
            return ResponseEntity.internalServerError().body(error);
        }
    }
}

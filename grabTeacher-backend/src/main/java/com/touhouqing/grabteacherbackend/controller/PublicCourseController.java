package com.touhouqing.grabteacherbackend.controller;

import com.touhouqing.grabteacherbackend.common.result.CommonResult;
import com.touhouqing.grabteacherbackend.model.vo.CourseVO;
import com.touhouqing.grabteacherbackend.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.touhouqing.grabteacherbackend.cache.ActiveCoursesLocalCache;
import org.springframework.http.MediaType;
import java.time.Duration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public/courses")
@Tag(name = "公共课程接口", description = "供所有用户访问的课程相关接口")
public class PublicCourseController {

    private static final Logger logger = LoggerFactory.getLogger(PublicCourseController.class);

    @Autowired
    private CourseService courseService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ActiveCoursesLocalCache activeCoursesLocalCache;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 获取公开课程列表（分页）
     */
    @Operation(summary = "获取公开课程列表", description = "获取所有活跃状态的课程列表，支持分页和筛选")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping
    public ResponseEntity<CommonResult<Map<String, Object>>> getPublicCourseList(
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "科目ID") @RequestParam(required = false) Long subjectId,
            @Parameter(description = "教师ID") @RequestParam(required = false) Long teacherId,
            @Parameter(description = "课程类型") @RequestParam(required = false) String courseType,
            @Parameter(description = "适用年级") @RequestParam(required = false) String grade) {
        try {
            // 只返回活跃状态的课程
            Page<CourseVO> coursePage = courseService.getCourseList(page, size, keyword,
                    subjectId, teacherId, "active", courseType, grade);
            
            Map<String, Object> response = new HashMap<>();
            response.put("courses", coursePage.getRecords());
            response.put("total", coursePage.getTotal());
            response.put("current", coursePage.getCurrent());
            response.put("size", coursePage.getSize());
            response.put("pages", coursePage.getPages());
            
            return ResponseEntity.ok(CommonResult.success("获取课程列表成功", response));
        } catch (Exception e) {
            logger.error("获取公开课程列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败，请稍后重试"));
        }
    }

    /**
     * 获取活跃状态的课程列表（不分页）
     */
    @Operation(summary = "获取活跃课程列表", description = "获取所有状态为活跃的课程列表")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/active")
    public ResponseEntity<?> getActiveCourses(
            @Parameter(description = "返回数量上限，不传则返回全部") @RequestParam(required = false) Integer limit) {
        try {
            int lim = (limit == null || limit <= 0) ? Integer.MAX_VALUE : limit;
            String cacheKey = (limit == null || limit <= 0)
                    ? "activeCoursesAll:json:all"
                    : "activeCoursesLimited:json:limit:" + lim;

            // 1) 尝试本地 L1 缓存（JSON 文本）
            String json = activeCoursesLocalCache.get(cacheKey);
            if (json != null) {
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
            }

            // 2) 尝试 Redis L2 缓存（JSON 文本）
            json = stringRedisTemplate.opsForValue().get(cacheKey);
            if (json != null) {
                activeCoursesLocalCache.put(cacheKey, json);
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
            }

            // 3) 回源：获取对象列表，序列化为 JSON 文本，并写回两级缓存
            List<CourseVO> courses = (limit == null || limit <= 0)
                    ? courseService.getActiveCourses()
                    : courseService.getActiveCoursesLimited(limit);

            // 使用项目全局 ObjectMapper 确保序列化一致
            json = objectMapper.writeValueAsString(CommonResult.success("获取活跃课程列表成功", courses));

            activeCoursesLocalCache.put(cacheKey, json);
            // 与 activeCoursesLimited TTL 对齐（5 分钟）；全量也使用 5 分钟，避免过期不一致
            stringRedisTemplate.opsForValue().set(cacheKey, json, Duration.ofMinutes(5));

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
        } catch (Exception e) {
            logger.error("获取活跃课程列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败，请稍后重试"));
        }
    }

    /**
     * 根据ID获取课程详情（公开接口）
     */
    @Operation(summary = "获取课程详情", description = "根据课程ID获取课程详细信息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "课程不存在")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CommonResult<CourseVO>> getCourseById(
            @Parameter(description = "课程ID", required = true) @PathVariable Long id) {
        try {
            CourseVO course = courseService.getCourseById(id);
            if (course == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(CommonResult.error("课程不存在"));
            }
            // 只返回活跃状态的课程
            if (!"active".equals(course.getStatus())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(CommonResult.error("课程不存在或已下架"));
            }
            return ResponseEntity.ok(CommonResult.success("获取课程成功", course));
        } catch (Exception e) {
            logger.error("获取课程异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败，请稍后重试"));
        }
    }

    /**
     * 获取最新课程列表（精选课程）
     */
    @Operation(summary = "获取最新课程列表", description = "获取管理员精选的最新课程列表")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/latest")
    public ResponseEntity<CommonResult<Map<String, Object>>> getLatestCourses(
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "6") int size,
            @Parameter(description = "科目ID") @RequestParam(required = false) Long subjectId,
            @Parameter(description = "适用年级") @RequestParam(required = false) String grade) {
        try {
            // 获取精选课程列表
            Page<CourseVO> coursePage = courseService.getFeaturedCourses(page, size, subjectId, grade);

            Map<String, Object> response = new HashMap<>();
            response.put("courses", coursePage.getRecords());
            response.put("total", coursePage.getTotal());
            response.put("current", coursePage.getCurrent());
            response.put("size", coursePage.getSize());
            response.put("pages", coursePage.getPages());

            return ResponseEntity.ok(CommonResult.success("获取最新课程列表成功", response));
        } catch (Exception e) {
            logger.error("获取最新课程列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败，请稍后重试"));
        }
    }

    /**
     * 获取所有精选课程列表（不分页，用于首页滚动展示）
     */
    @Operation(summary = "获取所有精选课程", description = "获取所有精选课程列表，不分页，用于首页滚动展示")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/featured")
    public ResponseEntity<CommonResult<List<CourseVO>>> getAllFeaturedCourses() {
        try {
            List<CourseVO> courses = courseService.getAllFeaturedCourses();
            return ResponseEntity.ok(CommonResult.success("获取精选课程列表成功", courses));
        } catch (Exception e) {
            logger.error("获取精选课程列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败，请稍后重试"));
        }
    }
}

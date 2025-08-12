package com.touhouqing.grabteacherbackend.controller;

import com.touhouqing.grabteacherbackend.dto.ApiResponse;
import com.touhouqing.grabteacherbackend.dto.CourseResponse;
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

    /**
     * 获取公开课程列表（分页）
     */
    @Operation(summary = "获取公开课程列表", description = "获取所有活跃状态的课程列表，支持分页和筛选")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPublicCourseList(
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "科目ID") @RequestParam(required = false) Long subjectId,
            @Parameter(description = "教师ID") @RequestParam(required = false) Long teacherId,
            @Parameter(description = "课程类型") @RequestParam(required = false) String courseType,
            @Parameter(description = "适用年级") @RequestParam(required = false) String grade) {
        try {
            // 只返回活跃状态的课程
            Page<CourseResponse> coursePage = courseService.getCourseList(page, size, keyword,
                    subjectId, teacherId, "active", courseType, grade);
            
            Map<String, Object> response = new HashMap<>();
            response.put("courses", coursePage.getRecords());
            response.put("total", coursePage.getTotal());
            response.put("current", coursePage.getCurrent());
            response.put("size", coursePage.getSize());
            response.put("pages", coursePage.getPages());
            
            return ResponseEntity.ok(ApiResponse.success("获取课程列表成功", response));
        } catch (Exception e) {
            logger.error("获取公开课程列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取失败，请稍后重试"));
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
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getActiveCourses() {
        try {
            List<CourseResponse> courses = courseService.getActiveCourses();
            return ResponseEntity.ok(ApiResponse.success("获取活跃课程列表成功", courses));
        } catch (Exception e) {
            logger.error("获取活跃课程列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取失败，请稍后重试"));
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
    public ResponseEntity<ApiResponse<CourseResponse>> getCourseById(
            @Parameter(description = "课程ID", required = true) @PathVariable Long id) {
        try {
            CourseResponse course = courseService.getCourseById(id);
            if (course == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("课程不存在"));
            }
            // 只返回活跃状态的课程
            if (!"active".equals(course.getStatus())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("课程不存在或已下架"));
            }
            return ResponseEntity.ok(ApiResponse.success("获取课程成功", course));
        } catch (Exception e) {
            logger.error("获取课程异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取失败，请稍后重试"));
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
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLatestCourses(
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "6") int size,
            @Parameter(description = "科目ID") @RequestParam(required = false) Long subjectId,
            @Parameter(description = "适用年级") @RequestParam(required = false) String grade) {
        try {
            // 获取精选课程列表
            Page<CourseResponse> coursePage = courseService.getFeaturedCourses(page, size, subjectId, grade);

            Map<String, Object> response = new HashMap<>();
            response.put("courses", coursePage.getRecords());
            response.put("total", coursePage.getTotal());
            response.put("current", coursePage.getCurrent());
            response.put("size", coursePage.getSize());
            response.put("pages", coursePage.getPages());

            return ResponseEntity.ok(ApiResponse.success("获取最新课程列表成功", response));
        } catch (Exception e) {
            logger.error("获取最新课程列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取失败，请稍后重试"));
        }
    }
}

package com.touhouqing.grabteacherbackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.dto.ApiResponse;
import com.touhouqing.grabteacherbackend.dto.CourseRequest;
import com.touhouqing.grabteacherbackend.dto.CourseResponse;
import com.touhouqing.grabteacherbackend.entity.Course;
import com.touhouqing.grabteacherbackend.security.UserPrincipal;
import com.touhouqing.grabteacherbackend.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
@Tag(name = "课程管理", description = "课程相关的增删改查操作")
@SecurityRequirement(name = "Bearer Authentication")
public class CourseController {

    private static final Logger logger = LoggerFactory.getLogger(CourseController.class);

    @Autowired
    private CourseService courseService;

    /**
     * 创建课程
     */
    @Operation(summary = "创建课程", description = "教师和管理员可以创建课程")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "创建成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PostMapping
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Course>> createCourse(
            @Valid @RequestBody CourseRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            Course course = courseService.createCourse(request, currentUser.getId(), 
                    currentUser.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "").toLowerCase());
            return ResponseEntity.ok(ApiResponse.success("课程创建成功", course));
        } catch (RuntimeException e) {
            logger.warn("创建课程失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("创建课程异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("创建失败，请稍后重试"));
        }
    }

    /**
     * 更新课程
     */
    @Operation(summary = "更新课程", description = "教师可以更新自己的课程，管理员可以更新任何课程")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "课程不存在")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Course>> updateCourse(
            @Parameter(description = "课程ID", required = true) @PathVariable Long id,
            @Valid @RequestBody CourseRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            Course course = courseService.updateCourse(id, request, currentUser.getId(), 
                    currentUser.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "").toLowerCase());
            return ResponseEntity.ok(ApiResponse.success("课程更新成功", course));
        } catch (RuntimeException e) {
            logger.warn("更新课程失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("更新课程异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("更新失败，请稍后重试"));
        }
    }

    /**
     * 删除课程
     */
    @Operation(summary = "删除课程", description = "教师可以删除自己的课程，管理员可以删除任何课程")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "课程不存在")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(
            @Parameter(description = "课程ID", required = true) @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            courseService.deleteCourse(id, currentUser.getId(), 
                    currentUser.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "").toLowerCase());
            return ResponseEntity.ok(ApiResponse.success("课程删除成功", null));
        } catch (RuntimeException e) {
            logger.warn("删除课程失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("删除课程异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("删除失败，请稍后重试"));
        }
    }

    /**
     * 根据ID获取课程详情
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
            return ResponseEntity.ok(ApiResponse.success("获取课程成功", course));
        } catch (Exception e) {
            logger.error("获取课程异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取失败，请稍后重试"));
        }
    }

    /**
     * 获取课程列表（分页）
     */
    @Operation(summary = "获取课程列表", description = "分页查询课程信息，支持多条件筛选")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCourseList(
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "科目ID") @RequestParam(required = false) Long subjectId,
            @Parameter(description = "教师ID") @RequestParam(required = false) Long teacherId,
            @Parameter(description = "课程状态") @RequestParam(required = false) String status,
            @Parameter(description = "课程类型") @RequestParam(required = false) String courseType,
            @Parameter(description = "适用年级") @RequestParam(required = false) String grade,
            @Parameter(description = "适合性别") @RequestParam(required = false) String gender) {
        try {
            Page<CourseResponse> coursePage = courseService.getCourseList(page, size, keyword,
                    subjectId, teacherId, status, courseType, grade, gender);
            
            Map<String, Object> response = new HashMap<>();
            response.put("courses", coursePage.getRecords());
            response.put("total", coursePage.getTotal());
            response.put("current", coursePage.getCurrent());
            response.put("size", coursePage.getSize());
            response.put("pages", coursePage.getPages());
            
            return ResponseEntity.ok(ApiResponse.success("获取课程列表成功", response));
        } catch (Exception e) {
            logger.error("获取课程列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取失败，请稍后重试"));
        }
    }

    /**
     * 获取活跃状态的课程列表
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
     * 获取教师的课程列表
     */
    @Operation(summary = "获取教师课程列表", description = "教师查看自己的课程，管理员可以查看任何教师的课程")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")
    })
    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getTeacherCourses(
            @Parameter(description = "教师ID", required = true) @PathVariable Long teacherId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            List<CourseResponse> courses = courseService.getTeacherCourses(teacherId, currentUser.getId(),
                    currentUser.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "").toLowerCase());
            return ResponseEntity.ok(ApiResponse.success("获取教师课程列表成功", courses));
        } catch (RuntimeException e) {
            logger.warn("获取教师课程列表失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("获取教师课程列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取失败，请稍后重试"));
        }
    }

    /**
     * 获取当前教师的课程列表
     */
    @Operation(summary = "获取当前教师课程列表", description = "教师查看自己的课程列表")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")
    })
    @GetMapping("/my-courses")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getMyTeacherCourses(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            // 需要先获取当前用户对应的教师ID
            // 这里简化处理，实际应该通过service获取
            List<CourseResponse> courses = courseService.getTeacherCourses(null, currentUser.getId(), "teacher");
            return ResponseEntity.ok(ApiResponse.success("获取我的课程列表成功", courses));
        } catch (RuntimeException e) {
            logger.warn("获取我的课程列表失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("获取我的课程列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取失败，请稍后重试"));
        }
    }

    /**
     * 更新课程状态
     */
    @Operation(summary = "更新课程状态", description = "教师可以更新自己课程的状态，管理员可以更新任何课程的状态")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "课程不存在")
    })
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateCourseStatus(
            @Parameter(description = "课程ID", required = true) @PathVariable Long id,
            @Parameter(description = "课程状态", required = true) @RequestParam String status,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            courseService.updateCourseStatus(id, status, currentUser.getId(),
                    currentUser.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "").toLowerCase());
            return ResponseEntity.ok(ApiResponse.success("课程状态更新成功", null));
        } catch (RuntimeException e) {
            logger.warn("更新课程状态失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("更新课程状态异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("更新失败，请稍后重试"));
        }
    }
}

package com.touhouqing.grabteacherbackend.controller;

import com.touhouqing.grabteacherbackend.dto.*;
import com.touhouqing.grabteacherbackend.entity.User;
import com.touhouqing.grabteacherbackend.entity.Student;
import com.touhouqing.grabteacherbackend.entity.Teacher;
import com.touhouqing.grabteacherbackend.service.AdminService;
import com.touhouqing.grabteacherbackend.service.GradeService;
import com.touhouqing.grabteacherbackend.service.CourseService;
import jakarta.validation.Valid;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "管理员功能", description = "管理员专用功能接口")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private AdminService adminService;

    @Autowired
    private GradeService gradeService;

    @Autowired
    private CourseService courseService;

    /**
     * 获取系统统计信息
     */
    @Operation(summary = "获取系统统计信息", description = "获取用户数量、教师数量等统计数据")
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getStatistics() {
        try {
            Map<String, Object> statistics = adminService.getSystemStatistics();
            return ResponseEntity.ok(ApiResponseDTO.success("获取成功", statistics));
        } catch (Exception e) {
            logger.error("获取统计信息异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("获取失败"));
        }
    }

    /**
     * 获取用户列表
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponseDTO<List<User>>> getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) String keyword) {
        try {
            List<User> users = adminService.getUserList(page, size, userType, keyword);
            return ResponseEntity.ok(ApiResponseDTO.success("获取成功", users));
        } catch (Exception e) {
            logger.error("获取用户列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("获取失败"));
        }
    }

    /**
     * 更新用户状态
     */
    @PutMapping("/users/{userId}/status")
    public ResponseEntity<ApiResponseDTO<Object>> updateUserStatus(
            @PathVariable Long userId,
            @RequestParam String status) {
        try {
            adminService.updateUserStatus(userId, status);
            return ResponseEntity.ok(ApiResponseDTO.success("更新成功", null));
        } catch (RuntimeException e) {
            logger.warn("更新用户状态失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("更新用户状态异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("更新失败"));
        }
    }

    // ===================== 学生管理接口 =====================

    /**
     * 获取学生列表（分页查询）
     */
    @Operation(summary = "获取学生列表", description = "分页查询学生信息，支持按姓名、年级搜索")
    @GetMapping("/students")
    public ResponseEntity<ApiResponseDTO<Page<Student>>> getStudentList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String gradeLevel) {
        try {
            Page<Student> students = adminService.getStudentList(page, size, keyword, gradeLevel);
            return ResponseEntity.ok(ApiResponseDTO.success("获取成功", students));
        } catch (Exception e) {
            logger.error("获取学生列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("获取失败"));
        }
    }

    /**
     * 根据ID获取学生详细信息
     */
    @Operation(summary = "获取学生详细信息", description = "根据学生ID获取详细信息")
    @GetMapping("/students/{studentId}")
    public ResponseEntity<ApiResponseDTO<Student>> getStudentById(@PathVariable Long studentId) {
        try {
            Student student = adminService.getStudentById(studentId);
            if (student == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(ApiResponseDTO.success("获取成功", student));
        } catch (Exception e) {
            logger.error("获取学生详细信息异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("获取失败"));
        }
    }

    /**
     * 添加学生
     */
    @Operation(summary = "添加学生", description = "管理员添加新学生")
    @PostMapping("/students")
    public ResponseEntity<ApiResponseDTO<Student>> addStudent(@RequestBody StudentInfoRequestDTO request) {
        try {
            Student student = adminService.addStudent(request);
            return ResponseEntity.ok(ApiResponseDTO.success("添加成功", student));
        } catch (RuntimeException e) {
            logger.warn("添加学生失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("添加学生异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("添加失败"));
        }
    }

    /**
     * 编辑学生信息
     */
    @Operation(summary = "编辑学生信息", description = "管理员编辑学生信息")
    @PutMapping("/students/{studentId}")
    public ResponseEntity<ApiResponseDTO<Student>> updateStudent(
            @PathVariable Long studentId,
            @RequestBody StudentInfoRequestDTO request) {
        try {
            Student student = adminService.updateStudent(studentId, request);
            return ResponseEntity.ok(ApiResponseDTO.success("更新成功", student));
        } catch (RuntimeException e) {
            logger.warn("更新学生信息失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("更新学生信息异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("更新失败"));
        }
    }

    /**
     * 删除学生
     */
    @Operation(summary = "删除学生", description = "管理员删除学生（逻辑删除）")
    @DeleteMapping("/students/{studentId}")
    public ResponseEntity<ApiResponseDTO<Object>> deleteStudent(@PathVariable Long studentId) {
        try {
            adminService.deleteStudent(studentId);
            return ResponseEntity.ok(ApiResponseDTO.success("删除成功", null));
        } catch (RuntimeException e) {
            logger.warn("删除学生失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("删除学生异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("删除失败"));
        }
    }

    // ===================== 教师管理接口 =====================

    /**
     * 获取教师列表（分页查询）
     */
    @Operation(summary = "获取教师列表", description = "分页查询教师信息，支持按姓名、科目搜索")
    @GetMapping("/teachers")
    public ResponseEntity<ApiResponseDTO<Page<Teacher>>> getTeacherList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) Boolean isVerified) {
        try {
            Page<Teacher> teachers = adminService.getTeacherList(page, size, keyword, subject, gender, isVerified);
            return ResponseEntity.ok(ApiResponseDTO.success("获取成功", teachers));
        } catch (Exception e) {
            logger.error("获取教师列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("获取失败"));
        }
    }

    /**
     * 根据ID获取教师详细信息
     */
    @Operation(summary = "获取教师详细信息", description = "根据教师ID获取详细信息")
    @GetMapping("/teachers/{teacherId}")
    public ResponseEntity<ApiResponseDTO<Teacher>> getTeacherById(@PathVariable Long teacherId) {
        try {
            Teacher teacher = adminService.getTeacherById(teacherId);
            if (teacher == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(ApiResponseDTO.success("获取成功", teacher));
        } catch (Exception e) {
            logger.error("获取教师详细信息异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("获取失败"));
        }
    }

    /**
     * 添加教师
     */
    @Operation(summary = "添加教师", description = "管理员添加新教师")
    @PostMapping("/teachers")
    public ResponseEntity<ApiResponseDTO<Teacher>> addTeacher(@RequestBody TeacherInfoRequestDTO request) {
        try {
            Teacher teacher = adminService.addTeacher(request);
            return ResponseEntity.ok(ApiResponseDTO.success("添加成功", teacher));
        } catch (RuntimeException e) {
            logger.warn("添加教师失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("添加教师异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("添加失败"));
        }
    }

    /**
     * 编辑教师信息
     */
    @Operation(summary = "编辑教师信息", description = "管理员编辑教师信息")
    @PutMapping("/teachers/{teacherId}")
    public ResponseEntity<ApiResponseDTO<Teacher>> updateTeacher(
            @PathVariable Long teacherId,
            @RequestBody TeacherInfoRequestDTO request) {
        try {
            Teacher teacher = adminService.updateTeacher(teacherId, request);
            return ResponseEntity.ok(ApiResponseDTO.success("更新成功", teacher));
        } catch (RuntimeException e) {
            logger.warn("更新教师信息失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("更新教师信息异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("更新失败"));
        }
    }

    /**
     * 删除教师
     */
    @Operation(summary = "删除教师", description = "管理员删除教师（逻辑删除）")
    @DeleteMapping("/teachers/{teacherId}")
    public ResponseEntity<ApiResponseDTO<Object>> deleteTeacher(@PathVariable Long teacherId) {
        try {
            adminService.deleteTeacher(teacherId);
            return ResponseEntity.ok(ApiResponseDTO.success("删除成功", null));
        } catch (RuntimeException e) {
            logger.warn("删除教师失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("删除教师异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("删除失败"));
        }
    }

    /**
     * 审核教师
     */
    @Operation(summary = "审核教师", description = "管理员审核教师认证状态")
    @PutMapping("/teachers/{teacherId}/verify")
    public ResponseEntity<ApiResponseDTO<Object>> verifyTeacher(
            @PathVariable Long teacherId,
            @RequestBody Map<String, Boolean> requestBody) {
        try {
            Boolean isVerified = requestBody.get("isVerified");
            adminService.verifyTeacher(teacherId, isVerified);
            return ResponseEntity.ok(ApiResponseDTO.success("审核成功", null));
        } catch (RuntimeException e) {
            logger.warn("审核教师失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("审核教师异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("审核失败"));
        }
    }

    /**
     * 设置教师为精选教师
     */
    @Operation(summary = "设置精选教师", description = "管理员设置教师是否为天下名师")
    @PutMapping("/teachers/{teacherId}/featured")
    public ResponseEntity<ApiResponseDTO<Object>> setTeacherFeatured(
            @PathVariable Long teacherId,
            @RequestBody Map<String, Boolean> requestBody) {
        try {
            Boolean isFeatured = requestBody.get("isFeatured");
            adminService.setTeacherFeatured(teacherId, isFeatured);
            String message = isFeatured ? "设置为天下名师成功" : "取消天下名师成功";
            return ResponseEntity.ok(ApiResponseDTO.success(message, null));
        } catch (RuntimeException e) {
            logger.warn("设置精选教师失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("设置精选教师异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("设置失败"));
        }
    }

    /**
     * 获取教师科目列表
     */
    @Operation(summary = "获取教师科目列表", description = "获取指定教师的科目ID列表")
    @GetMapping("/teachers/{teacherId}/subjects")
    public ResponseEntity<ApiResponseDTO<List<Long>>> getTeacherSubjects(@PathVariable Long teacherId) {
        try {
            List<Long> subjectIds = adminService.getTeacherSubjects(teacherId);
            return ResponseEntity.ok(ApiResponseDTO.success("获取成功", subjectIds));
        } catch (Exception e) {
            logger.error("获取教师科目异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("获取失败"));
        }
    }

    /**
     * 获取学生感兴趣的科目列表
     */
    @Operation(summary = "获取学生科目列表", description = "获取指定学生感兴趣的科目ID列表")
    @GetMapping("/students/{studentId}/subjects")
    public ResponseEntity<ApiResponseDTO<List<Long>>> getStudentSubjects(@PathVariable Long studentId) {
        try {
            List<Long> subjectIds = adminService.getStudentSubjects(studentId);
            return ResponseEntity.ok(ApiResponseDTO.success("获取成功", subjectIds));
        } catch (Exception e) {
            logger.error("获取学生科目异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("获取失败"));
        }
    }

    // ==================== 年级管理接口 ====================

    /**
     * 获取所有年级列表
     */
    @Operation(summary = "获取所有年级列表", description = "获取系统中所有年级信息")
    @GetMapping("/grades")
    public ResponseEntity<ApiResponseDTO<List<GradeResponseDTO>>> getAllGrades() {
        try {
            List<GradeResponseDTO> grades = gradeService.getAllGrades();
            return ResponseEntity.ok(ApiResponseDTO.success("获取成功", grades));
        } catch (Exception e) {
            logger.error("获取年级列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("获取失败"));
        }
    }

    /**
     * 根据ID获取年级信息
     */
    @Operation(summary = "根据ID获取年级信息", description = "根据年级ID获取详细信息")
    @GetMapping("/grades/{id}")
    public ResponseEntity<ApiResponseDTO<GradeResponseDTO>> getGradeById(@PathVariable Long id) {
        try {
            GradeResponseDTO grade = gradeService.getGradeById(id);
            return ResponseEntity.ok(ApiResponseDTO.success("获取成功", grade));
        } catch (RuntimeException e) {
            logger.warn("获取年级失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("获取年级异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("获取失败"));
        }
    }

    /**
     * 创建年级
     */
    @Operation(summary = "创建年级", description = "创建新的年级")
    @PostMapping("/grades")
    public ResponseEntity<ApiResponseDTO<GradeResponseDTO>> createGrade(@Valid @RequestBody GradeRequestDTO request) {
        try {
            GradeResponseDTO grade = gradeService.createGrade(request);
            return ResponseEntity.ok(ApiResponseDTO.success("创建成功", grade));
        } catch (RuntimeException e) {
            logger.warn("创建年级失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("创建年级异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("创建失败"));
        }
    }

    /**
     * 更新年级
     */
    @Operation(summary = "更新年级", description = "更新年级信息")
    @PutMapping("/grades/{id}")
    public ResponseEntity<ApiResponseDTO<GradeResponseDTO>> updateGrade(@PathVariable Long id, @Valid @RequestBody GradeRequestDTO request) {
        try {
            GradeResponseDTO grade = gradeService.updateGrade(id, request);
            return ResponseEntity.ok(ApiResponseDTO.success("更新成功", grade));
        } catch (RuntimeException e) {
            logger.warn("更新年级失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("更新年级异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("更新失败"));
        }
    }

    /**
     * 删除年级
     */
    @Operation(summary = "删除年级", description = "删除指定年级")
    @DeleteMapping("/grades/{id}")
    public ResponseEntity<ApiResponseDTO<String>> deleteGrade(@PathVariable Long id) {
        try {
            gradeService.deleteGrade(id);
            return ResponseEntity.ok(ApiResponseDTO.success("删除成功", "删除成功"));
        } catch (RuntimeException e) {
            logger.warn("删除年级失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("删除年级异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("删除失败"));
        }
    }

    // ==================== 精选课程管理接口 ====================

    /**
     * 获取精选课程列表
     */
    @Operation(summary = "获取精选课程列表", description = "获取所有精选课程的分页列表")
    @GetMapping("/featured-courses")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getFeaturedCourses(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String grade) {
        try {
            Page<CourseResponseDTO> coursePage = courseService.getFeaturedCourses(page, size, subjectId, grade);

            Map<String, Object> response = new HashMap<>();
            response.put("courses", coursePage.getRecords());
            response.put("total", coursePage.getTotal());
            response.put("current", coursePage.getCurrent());
            response.put("size", coursePage.getSize());
            response.put("pages", coursePage.getPages());

            return ResponseEntity.ok(ApiResponseDTO.success("获取精选课程列表成功", response));
        } catch (Exception e) {
            logger.error("获取精选课程列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("获取失败"));
        }
    }

    /**
     * 获取所有精选课程ID列表
     */
    @Operation(summary = "获取精选课程ID列表", description = "获取所有精选课程的ID列表")
    @GetMapping("/featured-courses/ids")
    public ResponseEntity<ApiResponseDTO<List<Long>>> getFeaturedCourseIds() {
        try {
            List<Long> courseIds = courseService.getFeaturedCourseIds();
            return ResponseEntity.ok(ApiResponseDTO.success("获取精选课程ID列表成功", courseIds));
        } catch (Exception e) {
            logger.error("获取精选课程ID列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("获取失败"));
        }
    }

    /**
     * 设置单个课程为精选课程
     */
    @Operation(summary = "设置精选课程", description = "设置单个课程为精选课程或取消精选")
    @PutMapping("/courses/{courseId}/featured")
    public ResponseEntity<ApiResponseDTO<Object>> setCourseAsFeatured(
            @PathVariable Long courseId,
            @RequestBody Map<String, Boolean> requestBody) {
        try {
            Boolean featured = requestBody.get("featured");
            if (featured == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponseDTO.error("featured参数不能为空"));
            }

            courseService.setCourseAsFeatured(courseId, featured);
            String action = featured ? "设置为精选课程" : "取消精选课程";
            return ResponseEntity.ok(ApiResponseDTO.success(action + "成功", null));
        } catch (RuntimeException e) {
            logger.warn("设置精选课程失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("设置精选课程异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("操作失败"));
        }
    }

    /**
     * 批量设置精选课程
     */
    @Operation(summary = "批量设置精选课程", description = "批量设置多个课程为精选课程或取消精选")
    @PutMapping("/courses/batch-featured")
    public ResponseEntity<ApiResponseDTO<Object>> batchSetFeaturedCourses(
            @RequestBody Map<String, Object> requestBody) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> courseIds = (List<Long>) requestBody.get("courseIds");
            Boolean featured = (Boolean) requestBody.get("featured");

            if (courseIds == null || courseIds.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponseDTO.error("课程ID列表不能为空"));
            }

            if (featured == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponseDTO.error("featured参数不能为空"));
            }

            courseService.batchSetFeaturedCourses(courseIds, featured);
            String action = featured ? "批量设置为精选课程" : "批量取消精选课程";
            return ResponseEntity.ok(ApiResponseDTO.success(action + "成功", null));
        } catch (RuntimeException e) {
            logger.warn("批量设置精选课程失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("批量设置精选课程异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("操作失败"));
        }
    }
}
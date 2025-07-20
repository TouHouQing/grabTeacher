package com.touhouqing.grabteacherbackend.controller;

import com.touhouqing.grabteacherbackend.dto.ApiResponse;
import com.touhouqing.grabteacherbackend.entity.User;
import com.touhouqing.grabteacherbackend.entity.Student;
import com.touhouqing.grabteacherbackend.entity.Teacher;
import com.touhouqing.grabteacherbackend.dto.StudentInfoRequest;
import com.touhouqing.grabteacherbackend.dto.TeacherInfoRequest;
import com.touhouqing.grabteacherbackend.service.AdminService;
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

    /**
     * 获取系统统计信息
     */
    @Operation(summary = "获取系统统计信息", description = "获取用户数量、教师数量等统计数据")
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics() {
        try {
            Map<String, Object> statistics = adminService.getSystemStatistics();
            return ResponseEntity.ok(ApiResponse.success("获取成功", statistics));
        } catch (Exception e) {
            logger.error("获取统计信息异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取失败"));
        }
    }

    /**
     * 获取用户列表
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<User>>> getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) String keyword) {
        try {
            List<User> users = adminService.getUserList(page, size, userType, keyword);
            return ResponseEntity.ok(ApiResponse.success("获取成功", users));
        } catch (Exception e) {
            logger.error("获取用户列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取失败"));
        }
    }

    /**
     * 更新用户状态
     */
    @PutMapping("/users/{userId}/status")
    public ResponseEntity<ApiResponse<Object>> updateUserStatus(
            @PathVariable Long userId,
            @RequestParam String status) {
        try {
            adminService.updateUserStatus(userId, status);
            return ResponseEntity.ok(ApiResponse.success("更新成功", null));
        } catch (RuntimeException e) {
            logger.warn("更新用户状态失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("更新用户状态异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("更新失败"));
        }
    }

    // ===================== 学生管理接口 =====================

    /**
     * 获取学生列表（分页查询）
     */
    @Operation(summary = "获取学生列表", description = "分页查询学生信息，支持按姓名、年级搜索")
    @GetMapping("/students")
    public ResponseEntity<ApiResponse<Page<Student>>> getStudentList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String gradeLevel) {
        try {
            Page<Student> students = adminService.getStudentList(page, size, keyword, gradeLevel);
            return ResponseEntity.ok(ApiResponse.success("获取成功", students));
        } catch (Exception e) {
            logger.error("获取学生列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取失败"));
        }
    }

    /**
     * 根据ID获取学生详细信息
     */
    @Operation(summary = "获取学生详细信息", description = "根据学生ID获取详细信息")
    @GetMapping("/students/{studentId}")
    public ResponseEntity<ApiResponse<Student>> getStudentById(@PathVariable Long studentId) {
        try {
            Student student = adminService.getStudentById(studentId);
            if (student == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(ApiResponse.success("获取成功", student));
        } catch (Exception e) {
            logger.error("获取学生详细信息异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取失败"));
        }
    }

    /**
     * 添加学生
     */
    @Operation(summary = "添加学生", description = "管理员添加新学生")
    @PostMapping("/students")
    public ResponseEntity<ApiResponse<Student>> addStudent(@RequestBody StudentInfoRequest request) {
        try {
            Student student = adminService.addStudent(request);
            return ResponseEntity.ok(ApiResponse.success("添加成功", student));
        } catch (RuntimeException e) {
            logger.warn("添加学生失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("添加学生异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("添加失败"));
        }
    }

    /**
     * 编辑学生信息
     */
    @Operation(summary = "编辑学生信息", description = "管理员编辑学生信息")
    @PutMapping("/students/{studentId}")
    public ResponseEntity<ApiResponse<Student>> updateStudent(
            @PathVariable Long studentId,
            @RequestBody StudentInfoRequest request) {
        try {
            Student student = adminService.updateStudent(studentId, request);
            return ResponseEntity.ok(ApiResponse.success("更新成功", student));
        } catch (RuntimeException e) {
            logger.warn("更新学生信息失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("更新学生信息异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("更新失败"));
        }
    }

    /**
     * 删除学生
     */
    @Operation(summary = "删除学生", description = "管理员删除学生（逻辑删除）")
    @DeleteMapping("/students/{studentId}")
    public ResponseEntity<ApiResponse<Object>> deleteStudent(@PathVariable Long studentId) {
        try {
            adminService.deleteStudent(studentId);
            return ResponseEntity.ok(ApiResponse.success("删除成功", null));
        } catch (RuntimeException e) {
            logger.warn("删除学生失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("删除学生异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("删除失败"));
        }
    }

    // ===================== 教师管理接口 =====================

    /**
     * 获取教师列表（分页查询）
     */
    @Operation(summary = "获取教师列表", description = "分页查询教师信息，支持按姓名、科目搜索")
    @GetMapping("/teachers")
    public ResponseEntity<ApiResponse<Page<Teacher>>> getTeacherList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) Boolean isVerified) {
        try {
            Page<Teacher> teachers = adminService.getTeacherList(page, size, keyword, subject, gender, isVerified);
            return ResponseEntity.ok(ApiResponse.success("获取成功", teachers));
        } catch (Exception e) {
            logger.error("获取教师列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取失败"));
        }
    }

    /**
     * 根据ID获取教师详细信息
     */
    @Operation(summary = "获取教师详细信息", description = "根据教师ID获取详细信息")
    @GetMapping("/teachers/{teacherId}")
    public ResponseEntity<ApiResponse<Teacher>> getTeacherById(@PathVariable Long teacherId) {
        try {
            Teacher teacher = adminService.getTeacherById(teacherId);
            if (teacher == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(ApiResponse.success("获取成功", teacher));
        } catch (Exception e) {
            logger.error("获取教师详细信息异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取失败"));
        }
    }

    /**
     * 添加教师
     */
    @Operation(summary = "添加教师", description = "管理员添加新教师")
    @PostMapping("/teachers")
    public ResponseEntity<ApiResponse<Teacher>> addTeacher(@RequestBody TeacherInfoRequest request) {
        try {
            Teacher teacher = adminService.addTeacher(request);
            return ResponseEntity.ok(ApiResponse.success("添加成功", teacher));
        } catch (RuntimeException e) {
            logger.warn("添加教师失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("添加教师异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("添加失败"));
        }
    }

    /**
     * 编辑教师信息
     */
    @Operation(summary = "编辑教师信息", description = "管理员编辑教师信息")
    @PutMapping("/teachers/{teacherId}")
    public ResponseEntity<ApiResponse<Teacher>> updateTeacher(
            @PathVariable Long teacherId,
            @RequestBody TeacherInfoRequest request) {
        try {
            Teacher teacher = adminService.updateTeacher(teacherId, request);
            return ResponseEntity.ok(ApiResponse.success("更新成功", teacher));
        } catch (RuntimeException e) {
            logger.warn("更新教师信息失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("更新教师信息异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("更新失败"));
        }
    }

    /**
     * 删除教师
     */
    @Operation(summary = "删除教师", description = "管理员删除教师（逻辑删除）")
    @DeleteMapping("/teachers/{teacherId}")
    public ResponseEntity<ApiResponse<Object>> deleteTeacher(@PathVariable Long teacherId) {
        try {
            adminService.deleteTeacher(teacherId);
            return ResponseEntity.ok(ApiResponse.success("删除成功", null));
        } catch (RuntimeException e) {
            logger.warn("删除教师失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("删除教师异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("删除失败"));
        }
    }

    /**
     * 审核教师
     */
    @Operation(summary = "审核教师", description = "管理员审核教师认证状态")
    @PutMapping("/teachers/{teacherId}/verify")
    public ResponseEntity<ApiResponse<Object>> verifyTeacher(
            @PathVariable Long teacherId,
            @RequestBody Map<String, Boolean> requestBody) {
        try {
            Boolean isVerified = requestBody.get("isVerified");
            adminService.verifyTeacher(teacherId, isVerified);
            return ResponseEntity.ok(ApiResponse.success("审核成功", null));
        } catch (RuntimeException e) {
            logger.warn("审核教师失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("审核教师异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("审核失败"));
        }
    }
} 
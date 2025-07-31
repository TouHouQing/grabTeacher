package com.touhouqing.grabteacherbackend.controller;

import com.touhouqing.grabteacherbackend.dto.ApiResponse;
import com.touhouqing.grabteacherbackend.dto.StudentInfoRequest;
import com.touhouqing.grabteacherbackend.dto.StudentProfileResponse;
import com.touhouqing.grabteacherbackend.entity.Student;
import com.touhouqing.grabteacherbackend.security.UserPrincipal;
import com.touhouqing.grabteacherbackend.service.StudentService;
import java.util.Map;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('STUDENT')")
public class StudentController {

    private static final Logger logger = LoggerFactory.getLogger(StudentController.class);

    @Autowired
    private StudentService studentService;

    /**
     * 获取学生个人信息
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<StudentProfileResponse>> getProfile(Authentication authentication) {
        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            StudentProfileResponse studentProfile = studentService.getStudentProfileByUserId(userPrincipal.getId());

            if (studentProfile == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("学生信息不存在"));
            }

            return ResponseEntity.ok(ApiResponse.success("获取成功", studentProfile));
        } catch (Exception e) {
            logger.error("获取学生信息异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取失败"));
        }
    }

    /**
     * 更新学生信息
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Student>> updateProfile(
            @Valid @RequestBody StudentInfoRequest request,
            Authentication authentication) {
        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            Student updatedStudent = studentService.updateStudentInfo(userPrincipal.getId(), request);

            return ResponseEntity.ok(ApiResponse.success("更新成功", updatedStudent));
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
     * 获取学生控制台统计数据
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics(Authentication authentication) {
        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            Map<String, Object> statistics = studentService.getStudentStatistics(userPrincipal.getId());

            return ResponseEntity.ok(ApiResponse.success("获取成功", statistics));
        } catch (Exception e) {
            logger.error("获取学生统计数据异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取失败"));
        }
    }
} 
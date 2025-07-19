package com.touhouqing.grabteacherbackend.controller;

import com.touhouqing.grabteacherbackend.dto.ApiResponse;
import com.touhouqing.grabteacherbackend.entity.User;
import com.touhouqing.grabteacherbackend.service.AdminService;
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
} 
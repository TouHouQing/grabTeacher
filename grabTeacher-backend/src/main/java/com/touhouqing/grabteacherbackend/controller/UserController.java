package com.touhouqing.grabteacherbackend.controller;

import com.touhouqing.grabteacherbackend.entity.dto.ApiResponse;
import com.touhouqing.grabteacherbackend.entity.dto.PasswordChangeRequest;
import com.touhouqing.grabteacherbackend.entity.dto.EmailUpdateRequest;
import com.touhouqing.grabteacherbackend.entity.dto.UserUpdateRequest;
import com.touhouqing.grabteacherbackend.entity.User;
import com.touhouqing.grabteacherbackend.security.UserPrincipal;
import com.touhouqing.grabteacherbackend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "用户信息", description = "用户个人信息管理接口")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private AuthService authService;

    /**
     * 获取用户基本信息
     */
    @Operation(summary = "获取用户信息", description = "获取当前登录用户的基本信息")
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<User>> getUserInfo(Authentication authentication) {
        try {
            if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(401, "未登录"));
            }

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            User user = authService.getUserById(userPrincipal.getId());
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("用户不存在"));
            }
            
            // 清除敏感信息
            user.setPassword(null);
            
            return ResponseEntity.ok(ApiResponse.success("获取成功", user));
        } catch (Exception e) {
            logger.error("获取用户信息异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取失败"));
        }
    }

    /**
     * 修改密码
     */
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @Valid @RequestBody PasswordChangeRequest request,
            Authentication authentication) {
        try {
            if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(401, "未登录"));
            }

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            // 验证新密码和确认密码是否一致
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("新密码和确认密码不一致"));
            }

            boolean success = authService.changePassword(userPrincipal.getId(),
                    request.getCurrentPassword(), request.getNewPassword());

            if (success) {
                return ResponseEntity.ok(ApiResponse.success("密码修改成功", null));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("当前密码错误"));
            }
        } catch (Exception e) {
            logger.error("修改密码异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("修改密码失败"));
        }
    }

    /**
     * 更新邮箱
     */
    @Operation(summary = "更新邮箱", description = "更新当前登录用户的邮箱地址")
    @PutMapping("/update-email")
    public ResponseEntity<ApiResponse<String>> updateEmail(
            @Valid @RequestBody EmailUpdateRequest request,
            Authentication authentication) {
        try {
            if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(401, "未登录"));
            }

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            boolean success = authService.updateEmail(userPrincipal.getId(),
                    request.getNewEmail(), request.getCurrentPassword());

            if (success) {
                return ResponseEntity.ok(ApiResponse.success("邮箱更新成功", null));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("邮箱更新失败，请检查密码是否正确或邮箱是否已被使用"));
            }
        } catch (Exception e) {
            logger.error("更新邮箱异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("邮箱更新失败"));
        }
    }

    /**
     * 更新用户基本信息
     */
    @Operation(summary = "更新用户基本信息", description = "更新当前登录用户的基本信息（如出生年月）")
    @PutMapping("/update-profile")
    public ResponseEntity<ApiResponse<String>> updateUserProfile(
            @Valid @RequestBody UserUpdateRequest request,
            Authentication authentication) {
        try {
            if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(401, "未登录"));
            }

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            boolean success = authService.updateUserProfile(userPrincipal.getId(), request);

            if (success) {
                return ResponseEntity.ok(ApiResponse.success("用户信息更新成功", null));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户信息更新失败"));
            }
        } catch (Exception e) {
            logger.error("更新用户信息异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("更新用户信息失败"));
        }
    }
}
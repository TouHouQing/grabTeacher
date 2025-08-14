package com.touhouqing.grabteacherbackend.controller;

import com.touhouqing.grabteacherbackend.dto.ApiResponseDTO;
import com.touhouqing.grabteacherbackend.dto.AuthResponseDTO;
import com.touhouqing.grabteacherbackend.dto.LoginRequestDTO;
import com.touhouqing.grabteacherbackend.dto.RegisterRequestDTO;
import com.touhouqing.grabteacherbackend.dto.PasswordChangeRequestDTO;
import com.touhouqing.grabteacherbackend.security.UserPrincipal;
import com.touhouqing.grabteacherbackend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "用户认证", description = "用户注册、登录、登出等认证相关接口")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    /**
     * 用户注册
     */
    @Operation(summary = "用户注册", description = "注册新用户（学生或教师）")
    @PostMapping("/register")
    public ResponseEntity<ApiResponseDTO<AuthResponseDTO>> registerUser(@Valid @RequestBody RegisterRequestDTO registerRequestDTO) {
        try {
            logger.info("收到注册请求: 用户名={}, 邮箱={}, 类型={}", 
                       registerRequestDTO.getUsername(),
                       registerRequestDTO.getEmail(),
                       registerRequestDTO.getUserType());
            
            AuthResponseDTO authResponse = authService.registerUser(registerRequestDTO);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponseDTO.success("注册成功", authResponse));
        } catch (RuntimeException e) {
            logger.warn("注册失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("注册异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("注册失败，请稍后重试"));
        }
    }

    /**
     * 用户登录
     */
    @Operation(summary = "用户登录", description = "用户登录验证")
    @PostMapping("/login")
    public ResponseEntity<ApiResponseDTO<AuthResponseDTO>> authenticateUser(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        try {
            logger.info("收到登录请求: {}", loginRequestDTO.getUsername());
            
            AuthResponseDTO authResponse = authService.authenticateUser(loginRequestDTO);
            return ResponseEntity.ok(ApiResponseDTO.success("登录成功", authResponse));
        } catch (RuntimeException e) {
            logger.warn("登录失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponseDTO.error(401, e.getMessage()));
        } catch (Exception e) {
            logger.error("登录异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("登录失败，请稍后重试"));
        }
    }

    /**
     * 用户退出登录
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDTO<Object>> logout() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
                UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
                logger.info("用户退出登录: {}", userPrincipal.getEmail());
            }
            
            // JWT 是无状态的，前端删除 token 即可
            SecurityContextHolder.clearContext();
            return ResponseEntity.ok(ApiResponseDTO.success("退出登录成功", null));
        } catch (Exception e) {
            logger.error("退出登录异常: ", e);
            return ResponseEntity.ok(ApiResponseDTO.success("退出登录成功", null));
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponseDTO<Object>> getCurrentUser(Authentication authentication) {
        try {
            if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponseDTO.error(401, "未登录"));
            }

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", userPrincipal.getId());
            userInfo.put("username", userPrincipal.getUsername());
            userInfo.put("email", userPrincipal.getEmail());
            userInfo.put("authorities", userPrincipal.getAuthorities());
            
            return ResponseEntity.ok(ApiResponseDTO.success("获取用户信息成功", userInfo));
        } catch (Exception e) {
            logger.error("获取用户信息异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("获取用户信息失败"));
        }
    }

    /**
     * 检查用户名是否可用
     */
    @GetMapping("/check-username")
    public ResponseEntity<ApiResponseDTO<Boolean>> checkUsername(@RequestParam String username) {
        try {
            boolean available = authService.isUsernameAvailable(username);
            return ResponseEntity.ok(ApiResponseDTO.success("检查完成", available));
        } catch (Exception e) {
            logger.error("检查用户名异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("检查失败"));
        }
    }

    /**
     * 检查邮箱是否可用
     */
    @GetMapping("/check-email")
    public ResponseEntity<ApiResponseDTO<Boolean>> checkEmail(@RequestParam String email) {
        try {
            boolean available = authService.isEmailAvailable(email);
            return ResponseEntity.ok(ApiResponseDTO.success("检查完成", available));
        } catch (Exception e) {
            logger.error("检查邮箱异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("检查失败"));
        }
    }

    /**
     * API 测试接口
     */
    @GetMapping("/test")
    public ResponseEntity<ApiResponseDTO<String>> test() {
        return ResponseEntity.ok(ApiResponseDTO.success("API 测试成功", "服务器运行正常"));
    }

    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponseDTO<Object>> changePassword(
            @Valid @RequestBody PasswordChangeRequestDTO request,
            Authentication authentication) {
        try {
            if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponseDTO.error(401, "未登录"));
            }

            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponseDTO.error("新密码和确认密码不匹配"));
            }

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            authService.updatePassword(userPrincipal.getId(), request.getCurrentPassword(), request.getNewPassword());
            
            return ResponseEntity.ok(ApiResponseDTO.success("密码修改成功", null));
        } catch (RuntimeException e) {
            logger.warn("密码修改失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("密码修改异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("密码修改失败"));
        }
    }

    /**
     * 管理员登录
     */
    @Operation(summary = "管理员登录", description = "管理员专用登录接口")
    @PostMapping("/admin/login")
    public ResponseEntity<ApiResponseDTO<AuthResponseDTO>> authenticateAdmin(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        try {
            logger.info("收到管理员登录请求: {}", loginRequestDTO.getUsername());
            
            AuthResponseDTO authResponse = authService.authenticateAdmin(loginRequestDTO);
            return ResponseEntity.ok(ApiResponseDTO.success("登录成功", authResponse));
        } catch (RuntimeException e) {
            logger.warn("管理员登录失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponseDTO.error(401, e.getMessage()));
        } catch (Exception e) {
            logger.error("管理员登录异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("登录失败，请稍后重试"));
        }
    }
} 
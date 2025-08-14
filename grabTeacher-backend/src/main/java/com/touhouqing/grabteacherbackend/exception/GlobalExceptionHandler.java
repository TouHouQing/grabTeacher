package com.touhouqing.grabteacherbackend.exception;

import com.touhouqing.grabteacherbackend.entity.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        // 获取第一个错误信息作为主要错误信息
        String firstError = errors.values().iterator().next();
        logger.warn("数据验证失败: {}", firstError);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "数据验证失败: " + firstError));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex) {
        String message = "请求体格式错误或缺失";
        if (ex.getMessage().contains("Required request body is missing")) {
            message = "请求体不能为空，请提供有效的JSON数据";
        } else if (ex.getMessage().contains("JSON parse error")) {
            message = "JSON格式错误，请检查请求数据格式";
        }
        logger.warn("请求体错误: {}", message);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, message));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex) {
        String message = "缺少必需的请求参数: " + ex.getParameterName();
        logger.warn("缺少请求参数: {}", ex.getParameterName());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, message));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex) {
        String message = "参数类型错误: " + ex.getName() + " 应为 " + ex.getRequiredType().getSimpleName() + " 类型";
        logger.warn("参数类型错误: {}", message);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, message));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex) {
        String message = "不支持的请求方法: " + ex.getMethod() + "，支持的方法: " + String.join(", ", ex.getSupportedMethods());
        logger.warn("不支持的请求方法: {}", ex.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(405, message));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoHandlerFoundException(
            NoHandlerFoundException ex) {
        String message = "请求的接口不存在: " + ex.getRequestURL();
        logger.warn("接口不存在: {}", ex.getRequestURL());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, message));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentialsException(BadCredentialsException ex) {
        logger.warn("认证失败: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, "邮箱或密码错误"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthenticationException(AuthenticationException ex) {
        logger.warn("认证异常: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, "认证失败"));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntimeException(RuntimeException ex) {
        logger.error("运行时异常: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("参数异常: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "参数错误: " + ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneralException(Exception ex) {
        // 打印详细错误信息到日志，但不暴露给前端
        logger.error("系统异常: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "服务器内部错误，请稍后重试"));
    }
} 
package com.touhouqing.grabteacherbackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.dto.*;
import com.touhouqing.grabteacherbackend.security.UserPrincipal;
import com.touhouqing.grabteacherbackend.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/booking")
@Tag(name = "预约管理", description = "课程预约相关接口")
@SecurityRequirement(name = "Bearer Authentication")
public class BookingController {

    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);

    @Autowired
    private BookingService bookingService;

    /**
     * 创建预约申请（学生操作）
     */
    @PostMapping("/request")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "创建预约申请", description = "学生创建课程预约申请")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "预约申请创建成功",
                    content = @Content(schema = @Schema(implementation = BookingResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")
    })
    public ResponseEntity<ApiResponseDTO<BookingResponseDTO>> createBookingRequest(
            @Valid @RequestBody BookingRequestDTO request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            BookingResponseDTO response = bookingService.createBookingRequest(request, currentUser.getId());
            return ResponseEntity.ok(ApiResponseDTO.success("预约申请创建成功", response));
        } catch (RuntimeException e) {
            logger.warn("创建预约申请失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("创建预约申请异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("创建预约申请失败"));
        }
    }

    /**
     * 管理员审批预约申请
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员审批预约申请", description = "管理员审批学生的预约申请")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "审批成功",
                    content = @Content(schema = @Schema(implementation = BookingResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "预约申请不存在")
    })
    public ResponseEntity<ApiResponseDTO<BookingResponseDTO>> approveBookingRequest(
            @Parameter(description = "预约申请ID", required = true) @PathVariable Long id,
            @Valid @RequestBody BookingApprovalDTO approval,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            BookingResponseDTO response = bookingService.approveBookingRequest(id, approval, currentUser.getId());
            return ResponseEntity.ok(ApiResponseDTO.success("审批成功", response));
        } catch (RuntimeException e) {
            logger.warn("审批预约申请失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("审批预约申请异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("审批失败"));
        }
    }

    /**
     * 学生取消预约申请
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "学生取消预约申请", description = "学生取消自己的预约申请")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "取消成功",
                    content = @Content(schema = @Schema(implementation = BookingResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "预约申请不存在")
    })
    public ResponseEntity<ApiResponseDTO<BookingResponseDTO>> cancelBookingRequest(
            @Parameter(description = "预约申请ID", required = true) @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            BookingResponseDTO response = bookingService.cancelBookingRequest(id, currentUser.getId());
            return ResponseEntity.ok(ApiResponseDTO.success("取消成功", response));
        } catch (RuntimeException e) {
            logger.warn("取消预约申请失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("取消预约申请异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("取消失败"));
        }
    }

    /**
     * 获取预约申请详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "获取预约申请详情", description = "根据ID获取预约申请的详细信息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功",
                    content = @Content(schema = @Schema(implementation = BookingResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "预约申请不存在")
    })
    public ResponseEntity<ApiResponseDTO<BookingResponseDTO>> getBookingRequest(
            @Parameter(description = "预约申请ID", required = true) @PathVariable Long id) {
        try {
            BookingResponseDTO response = bookingService.getBookingRequestById(id);
            return ResponseEntity.ok(ApiResponseDTO.success("获取成功", response));
        } catch (RuntimeException e) {
            logger.warn("获取预约申请失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("获取预约申请异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("获取失败"));
        }
    }

    /**
     * 获取学生的预约申请列表
     */
    @GetMapping("/student/my")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "获取学生的预约申请列表", description = "学生查看自己的预约申请列表")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")
    })
    public ResponseEntity<ApiResponseDTO<Page<BookingResponseDTO>>> getStudentBookingRequests(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "状态筛选") @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            Page<BookingResponseDTO> response = bookingService.getStudentBookingRequests(currentUser.getId(), page, size, status);
            return ResponseEntity.ok(ApiResponseDTO.success("获取成功", response));
        } catch (Exception e) {
            logger.error("获取学生预约申请列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("获取失败"));
        }
    }

    /**
     * 获取教师的预约申请列表
     */
    @GetMapping("/teacher/my")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "获取教师的预约申请列表", description = "教师查看收到的预约申请列表")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")
    })
    public ResponseEntity<ApiResponseDTO<Page<BookingResponseDTO>>> getTeacherBookingRequests(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "状态筛选") @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            Page<BookingResponseDTO> response = bookingService.getTeacherBookingRequests(currentUser.getId(), page, size, status);
            return ResponseEntity.ok(ApiResponseDTO.success("获取成功", response));
        } catch (Exception e) {
            logger.error("获取教师预约申请列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("获取失败"));
        }
    }

    /**
     * 获取教师的课程安排列表
     */
    @GetMapping("/teacher/schedules")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "获取教师的课程安排列表", description = "教师查看自己的课程安排")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")
    })
    public ResponseEntity<ApiResponseDTO<List<ScheduleResponseDTO>>> getTeacherSchedules(
            @Parameter(description = "开始日期", example = "2024-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期", example = "2024-01-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            List<ScheduleResponseDTO> response = bookingService.getTeacherSchedules(currentUser.getId(), startDate, endDate);
            return ResponseEntity.ok(ApiResponseDTO.success("获取成功", response));
        } catch (Exception e) {
            logger.error("获取教师课程安排列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("获取失败"));
        }
    }

    /**
     * 获取学生的课程安排列表
     */
    @GetMapping("/student/schedules")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "获取学生的课程安排列表", description = "学生查看自己的课程安排")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")
    })
    public ResponseEntity<ApiResponseDTO<List<ScheduleResponseDTO>>> getStudentSchedules(
            @Parameter(description = "开始日期", example = "2024-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期", example = "2024-01-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            List<ScheduleResponseDTO> response = bookingService.getStudentSchedules(currentUser.getId(), startDate, endDate);
            return ResponseEntity.ok(ApiResponseDTO.success("获取成功", response));
        } catch (Exception e) {
            logger.error("获取学生课程安排列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("获取失败"));
        }
    }

    /**
     * 获取学生的所有课程安排列表（不限日期范围）
     */
    @GetMapping("/student/schedules/all")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "获取学生的所有课程安排列表", description = "学生查看自己的所有课程安排，不限日期范围")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")
    })
    public ResponseEntity<ApiResponseDTO<List<ScheduleResponseDTO>>> getAllStudentSchedules(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            List<ScheduleResponseDTO> response = bookingService.getAllStudentSchedules(currentUser.getId());
            return ResponseEntity.ok(ApiResponseDTO.success("获取成功", response));
        } catch (Exception e) {
            logger.error("获取学生所有课程安排列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("获取失败"));
        }
    }

    /**
     * 检查用户是否可以使用免费试听
     */
    @GetMapping("/trial/check")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "检查免费试听资格", description = "检查学生是否可以使用免费试听课")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "检查成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")
    })
    public ResponseEntity<ApiResponseDTO<Boolean>> checkTrialEligibility(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            boolean canUseTrial = bookingService.canUseFreeTrial(currentUser.getId());
            return ResponseEntity.ok(ApiResponseDTO.success("检查成功", canUseTrial));
        } catch (Exception e) {
            logger.error("检查试听资格异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("检查失败"));
        }
    }

    /**
     * 管理员获取预约申请列表
     */
    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员获取预约申请列表", description = "管理员查看所有预约申请")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")
    })
    public ResponseEntity<ApiResponseDTO<Page<BookingResponseDTO>>> getAdminBookingRequests(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "状态筛选") @RequestParam(required = false) String status,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {
        try {
            Page<BookingResponseDTO> response = bookingService.getAdminBookingRequests(page, size, status, keyword);
            return ResponseEntity.ok(ApiResponseDTO.success("获取成功", response));
        } catch (Exception e) {
            logger.error("获取管理员预约申请列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("获取失败"));
        }
    }


}

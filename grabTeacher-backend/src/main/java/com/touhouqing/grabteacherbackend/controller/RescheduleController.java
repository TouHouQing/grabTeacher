package com.touhouqing.grabteacherbackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.dto.ApiResponse;
import com.touhouqing.grabteacherbackend.dto.RescheduleApprovalDTO;
import com.touhouqing.grabteacherbackend.dto.RescheduleRequestDTO;
import com.touhouqing.grabteacherbackend.dto.RescheduleResponseDTO;
import com.touhouqing.grabteacherbackend.security.UserPrincipal;
import com.touhouqing.grabteacherbackend.service.RescheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

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

/**
 * 调课管理控制器
 */
@RestController
@RequestMapping("/api/reschedule")
@Tag(name = "调课管理", description = "调课申请、审批、查询等功能")
public class RescheduleController {

    private static final Logger logger = LoggerFactory.getLogger(RescheduleController.class);

    @Autowired
    private RescheduleService rescheduleService;

    /**
     * 创建调课申请（学生操作）
     */
    @PostMapping("/request")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "创建调课申请", description = "学生创建调课申请")
    public ResponseEntity<ApiResponse<RescheduleResponseDTO>> createRescheduleRequest(
            @Valid @RequestBody RescheduleRequestDTO request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            RescheduleResponseDTO response = rescheduleService.createRescheduleRequest(request, currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success("调课申请创建成功", response));
        } catch (RuntimeException e) {
            logger.warn("创建调课申请失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("创建调课申请异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("创建调课申请失败"));
        }
    }

    /**
     * 教师审批调课申请
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "教师审批调课申请", description = "教师审批学生的调课申请")
    public ResponseEntity<ApiResponse<RescheduleResponseDTO>> approveRescheduleRequest(
            @Parameter(description = "调课申请ID", required = true) @PathVariable Long id,
            @Valid @RequestBody RescheduleApprovalDTO approval,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            RescheduleResponseDTO response = rescheduleService.approveRescheduleRequest(id, approval, currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success("审批成功", response));
        } catch (RuntimeException e) {
            logger.warn("审批调课申请失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("审批调课申请异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("审批失败"));
        }
    }

    /**
     * 取消调课申请（学生操作）
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "取消调课申请", description = "学生取消自己的调课申请")
    public ResponseEntity<ApiResponse<RescheduleResponseDTO>> cancelRescheduleRequest(
            @Parameter(description = "调课申请ID", required = true) @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            RescheduleResponseDTO response = rescheduleService.cancelRescheduleRequest(id, currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success("取消成功", response));
        } catch (RuntimeException e) {
            logger.warn("取消调课申请失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("取消调课申请异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("取消失败"));
        }
    }

    /**
     * 获取学生的调课申请列表
     */
    @GetMapping("/student/requests")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "获取学生调课申请列表", description = "学生查看自己的调课申请列表")
    public ResponseEntity<ApiResponse<Page<RescheduleResponseDTO>>> getStudentRescheduleRequests(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "状态筛选", example = "pending") @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            Page<RescheduleResponseDTO> result = rescheduleService.getStudentRescheduleRequests(
                    currentUser.getId(), page, size, status);
            return ResponseEntity.ok(ApiResponse.success("获取成功", result));
        } catch (RuntimeException e) {
            logger.warn("获取学生调课申请列表失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("获取学生调课申请列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取失败"));
        }
    }

    /**
     * 获取教师需要审批的调课申请列表
     */
    @GetMapping("/teacher/requests")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "获取教师调课申请列表", description = "教师查看需要审批的调课申请列表")
    public ResponseEntity<ApiResponse<Page<RescheduleResponseDTO>>> getTeacherRescheduleRequests(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "状态筛选", example = "pending") @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            Page<RescheduleResponseDTO> result = rescheduleService.getTeacherRescheduleRequests(
                    currentUser.getId(), page, size, status);
            return ResponseEntity.ok(ApiResponse.success("获取成功", result));
        } catch (RuntimeException e) {
            logger.warn("获取教师调课申请列表失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("获取教师调课申请列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取失败"));
        }
    }

    /**
     * 根据ID获取调课申请详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "获取调课申请详情", description = "根据ID获取调课申请的详细信息")
    public ResponseEntity<ApiResponse<RescheduleResponseDTO>> getRescheduleRequestById(
            @Parameter(description = "调课申请ID", required = true) @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            String userType = currentUser.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "").toLowerCase();
            RescheduleResponseDTO response = rescheduleService.getRescheduleRequestById(id, currentUser.getId(), userType);
            return ResponseEntity.ok(ApiResponse.success("获取成功", response));
        } catch (RuntimeException e) {
            logger.warn("获取调课申请详情失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("获取调课申请详情异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取失败"));
        }
    }

    /**
     * 获取教师待处理的调课申请数量
     */
    @GetMapping("/teacher/pending-count")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "获取待处理调课申请数量", description = "教师获取待处理的调课申请数量")
    public ResponseEntity<ApiResponse<Integer>> getPendingRescheduleCount(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            int count = rescheduleService.getPendingRescheduleCount(currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success("获取成功", count));
        } catch (Exception e) {
            logger.error("获取待处理调课申请数量异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取失败"));
        }
    }

    /**
     * 检查是否可以申请调课
     */
    @GetMapping("/can-apply/{scheduleId}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "检查是否可以申请调课", description = "学生检查指定课程是否可以申请调课")
    public ResponseEntity<ApiResponse<Boolean>> canApplyReschedule(
            @Parameter(description = "课程安排ID", required = true) @PathVariable Long scheduleId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            boolean canApply = rescheduleService.canApplyReschedule(scheduleId, currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success("检查完成", canApply));
        } catch (Exception e) {
            logger.error("检查是否可以申请调课异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("检查失败"));
        }
    }
}

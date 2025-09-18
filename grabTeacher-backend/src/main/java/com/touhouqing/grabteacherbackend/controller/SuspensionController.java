package com.touhouqing.grabteacherbackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.common.result.CommonResult;
import com.touhouqing.grabteacherbackend.model.dto.SuspensionApprovalDTO;
import com.touhouqing.grabteacherbackend.model.dto.SuspensionApplyDTO;
import com.touhouqing.grabteacherbackend.model.vo.SuspensionVO;
import com.touhouqing.grabteacherbackend.security.UserPrincipal;
import com.touhouqing.grabteacherbackend.service.SuspensionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/suspension")
@Tag(name = "请假管理", description = "请假申请、审批、查询等功能")
public class SuspensionController {

    @Autowired
    private SuspensionService suspensionService;

    @PostMapping("/request")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER')")
    @Operation(summary = "创建请假申请", description = "学生或教师创建请假申请（1v1）")
    public ResponseEntity<CommonResult<SuspensionVO>> createSuspensionRequest(
            @Valid @RequestBody SuspensionApplyDTO request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            SuspensionVO vo = suspensionService.createSuspensionRequest(request, currentUser.getId());
            return ResponseEntity.ok(CommonResult.success("请假申请创建成功", vo));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(CommonResult.error("创建请假申请失败"));
        }
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员审批请假申请", description = "管理员审批请假申请")
    public ResponseEntity<CommonResult<SuspensionVO>> approveSuspension(
            @Parameter(description = "请假申请ID", required = true) @PathVariable Long id,
            @Valid @RequestBody SuspensionApprovalDTO approval,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            SuspensionVO vo = suspensionService.adminApproveSuspension(id, approval, currentUser.getId());
            return ResponseEntity.ok(CommonResult.success("审批成功", vo));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(CommonResult.error("审批失败"));
        }
    }

    @GetMapping("/student/requests")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "获取学生请假申请列表", description = "学生查看自己的请假申请列表")
    public ResponseEntity<CommonResult<Page<SuspensionVO>>> getStudentSuspensionRequests(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            Page<SuspensionVO> result = suspensionService.getStudentSuspensionRequests(currentUser.getId(), page, size, status);
            return ResponseEntity.ok(CommonResult.success("获取成功", result));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(CommonResult.error("获取失败"));
        }
    }

    @GetMapping("/admin/requests")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员获取请假申请列表", description = "管理员查看所有请假申请列表")
    public ResponseEntity<CommonResult<Page<SuspensionVO>>> getAdminSuspensionRequests(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        try {
            Page<SuspensionVO> result = suspensionService.getAllSuspensionRequests(page, size, status);
            return ResponseEntity.ok(CommonResult.success("获取成功", result));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(CommonResult.error("获取失败"));
        }
    }

    @GetMapping("/teacher/requests")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "教师获取请假申请列表", description = "教师查看自己的请假申请列表")
    public ResponseEntity<CommonResult<Page<SuspensionVO>>> getTeacherSuspensionRequests(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            Page<SuspensionVO> result = suspensionService.getTeacherSuspensionRequests(currentUser.getId(), page, size, status);
            return ResponseEntity.ok(CommonResult.success("获取成功", result));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(CommonResult.error("获取失败"));
        }
    }

}


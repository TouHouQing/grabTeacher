package com.touhouqing.grabteacherbackend.controller;

import com.touhouqing.grabteacherbackend.common.result.CommonResult;
import com.touhouqing.grabteacherbackend.model.dto.AvailableTimeQueryDTO;
import com.touhouqing.grabteacherbackend.model.vo.AvailableTimeVO;
import com.touhouqing.grabteacherbackend.service.AvailableTimeService;
import com.touhouqing.grabteacherbackend.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 可上课时间管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/available-time")
@RequiredArgsConstructor
@Tag(name = "可上课时间管理", description = "教师可上课时间的设置和查询接口")
public class AvailableTimeController {

    private final AvailableTimeService availableTimeService;

    /**
     * 获取教师的可上课时间
     */
    @Operation(summary = "获取教师可上课时间", description = "获取指定教师的可上课时间安排")
    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<CommonResult<AvailableTimeVO>> getTeacherAvailableTime(
            @PathVariable Long teacherId) {
        try {
            AvailableTimeVO response = availableTimeService.getTeacherAvailableTime(teacherId);
            return ResponseEntity.ok(CommonResult.success("获取成功", response));
        } catch (Exception e) {
            log.error("获取教师可上课时间失败: teacherId={}", teacherId, e);
            return ResponseEntity.badRequest()
                    .body(CommonResult.error("获取失败: " + e.getMessage()));
        }
    }

    /**
     * 批量获取教师的可上课时间
     */
    @Operation(summary = "批量获取教师可上课时间", description = "传入teacherIds列表，返回各教师的可上课时间")
    @PostMapping("/teacher/batch")
    public ResponseEntity<CommonResult<java.util.List<AvailableTimeVO>>> getTeachersAvailableTime(
            @RequestBody java.util.List<Long> teacherIds) {
        try {
            java.util.List<AvailableTimeVO> list = availableTimeService.getTeachersAvailableTime(teacherIds);
            return ResponseEntity.ok(CommonResult.success("获取成功", list));
        } catch (Exception e) {
            log.error("批量获取教师可上课时间失败: teacherIds={}", teacherIds, e);
            return ResponseEntity.badRequest().body(CommonResult.error("获取失败: " + e.getMessage()));
        }
    }


    /**
     * 教师设置自己的可上课时间
     */
    @Operation(summary = "教师设置可上课时间", description = "教师设置自己的可上课时间安排")
    @PostMapping("/teacher/set")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<CommonResult<AvailableTimeVO>> setTeacherAvailableTime(
            @Valid @RequestBody AvailableTimeQueryDTO request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            // 确保教师只能设置自己的时间
            Long teacherId = availableTimeService.getTeacherIdByUserId(userPrincipal.getId());
            if (teacherId == null) {
                return ResponseEntity.badRequest()
                        .body(CommonResult.error("教师信息不存在"));
            }

            request.setTeacherId(teacherId);
            AvailableTimeVO response = availableTimeService.setTeacherAvailableTime(request);
            return ResponseEntity.ok(CommonResult.success("设置成功", response));
        } catch (Exception e) {
            log.error("教师设置可上课时间失败: userId={}", userPrincipal.getId(), e);
            return ResponseEntity.badRequest()
                    .body(CommonResult.error("设置失败: " + e.getMessage()));
        }
    }

    /**
     * 管理员设置教师的可上课时间
     */
    @Operation(summary = "管理员设置教师可上课时间", description = "管理员为指定教师设置可上课时间安排")
    @PostMapping("/admin/set")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResult<AvailableTimeVO>> setTeacherAvailableTimeByAdmin(
            @Valid @RequestBody AvailableTimeQueryDTO request) {
        try {
            AvailableTimeVO response = availableTimeService.setTeacherAvailableTime(request);
            return ResponseEntity.ok(CommonResult.success("设置成功", response));
        } catch (Exception e) {
            log.error("管理员设置教师可上课时间失败: teacherId={}", request.getTeacherId(), e);
            return ResponseEntity.badRequest()
                    .body(CommonResult.error("设置失败: " + e.getMessage()));
        }
    }

    /**
     * 获取当前登录教师的可上课时间
     */
    @Operation(summary = "获取当前教师可上课时间", description = "获取当前登录教师的可上课时间安排")
    @GetMapping("/teacher/current")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<CommonResult<AvailableTimeVO>> getCurrentTeacherAvailableTime(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            Long teacherId = availableTimeService.getTeacherIdByUserId(userPrincipal.getId());
            if (teacherId == null) {
                return ResponseEntity.badRequest()
                        .body(CommonResult.error("教师信息不存在"));
            }

            AvailableTimeVO response = availableTimeService.getTeacherAvailableTime(teacherId);
            return ResponseEntity.ok(CommonResult.success("获取成功", response));
        } catch (Exception e) {
            log.error("获取当前教师可上课时间失败: userId={}", userPrincipal.getId(), e);
            return ResponseEntity.badRequest()
                    .body(CommonResult.error("获取失败: " + e.getMessage()));
        }
    }


}

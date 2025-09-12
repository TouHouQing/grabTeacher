package com.touhouqing.grabteacherbackend.controller;

import com.touhouqing.grabteacherbackend.common.result.CommonResult;
import com.touhouqing.grabteacherbackend.model.dto.DailyAvailabilitySetDTO;
import com.touhouqing.grabteacherbackend.security.UserPrincipal;
import com.touhouqing.grabteacherbackend.service.TeacherDailyAvailabilityService;
import com.touhouqing.grabteacherbackend.service.TeacherService;
import com.touhouqing.grabteacherbackend.model.entity.Teacher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/available-time/daily")
@RequiredArgsConstructor
@Tag(name = "日历可上课时间", description = "教师按日历设置/查询可上课时间接口")
public class DailyAvailabilityController {

    private final TeacherDailyAvailabilityService dailyService;
    private final TeacherService teacherService;

    @Operation(summary = "教师设置自己某些日期的可上课时间段")
    @PostMapping("/teacher/set")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<CommonResult<String>> setByTeacher(
            @Valid @RequestBody DailyAvailabilitySetDTO req,
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            // 强制使用当前教师身份的 teacherId
            req.setTeacherId(getTeacherIdFromPrincipal(principal));
            dailyService.setDailyAvailability(req.getTeacherId(), req.getItems(), Boolean.TRUE.equals(req.getOverwrite()));
            return ResponseEntity.ok(CommonResult.success("设置成功", "OK"));
        } catch (Exception e) {
            log.error("教师设置日历可上课时间失败", e);
            return ResponseEntity.badRequest().body(CommonResult.error("设置失败: " + e.getMessage()));
        }
    }

    @Operation(summary = "管理员设置某教师某些日期的可上课时间段")
    @PostMapping("/admin/set")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResult<String>> setByAdmin(@Valid @RequestBody DailyAvailabilitySetDTO req) {
        try {
            dailyService.setDailyAvailability(req.getTeacherId(), req.getItems(), Boolean.TRUE.equals(req.getOverwrite()));
            return ResponseEntity.ok(CommonResult.success("设置成功", "OK"));
        } catch (Exception e) {
            log.error("管理员设置日历可上课时间失败, teacherId={}", req.getTeacherId(), e);
            return ResponseEntity.badRequest().body(CommonResult.error("设置失败: " + e.getMessage()));
        }
    }

    @Operation(summary = "查询某教师在日期范围内的日历可上课时间")
    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<CommonResult<Map<LocalDate, List<String>>>> query(
            @PathVariable Long teacherId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            Map<LocalDate, List<String>> data = dailyService.getDailyAvailability(teacherId, startDate, endDate);
            return ResponseEntity.ok(CommonResult.success("获取成功", data));
        } catch (Exception e) {
            log.error("查询日历可上课时间失败, teacherId={}, {}-{}", teacherId, startDate, endDate, e);
            return ResponseEntity.badRequest().body(CommonResult.error("获取失败: " + e.getMessage()));
        }
    }

    private Long getTeacherIdFromPrincipal(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new RuntimeException("未获取到登录用户");
        }
        Long userId = principal.getId();
        Teacher teacher = teacherService.getTeacherByUserId(userId);
        if (teacher == null) {
            throw new RuntimeException("未找到教师信息，请先完善教师资料");
        }
        return teacher.getId();
    }
}


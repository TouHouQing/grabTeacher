package com.touhouqing.grabteacherbackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.common.result.CommonResult;
import com.touhouqing.grabteacherbackend.common.result.RescheduleTimeCheckResult;
import com.touhouqing.grabteacherbackend.model.dto.RescheduleApprovalDTO;
import com.touhouqing.grabteacherbackend.model.dto.RescheduleApplyDTO;
import com.touhouqing.grabteacherbackend.model.entity.CourseEnrollment;
import com.touhouqing.grabteacherbackend.model.entity.CourseSchedule;
import com.touhouqing.grabteacherbackend.model.vo.RescheduleVO;
import com.touhouqing.grabteacherbackend.model.entity.Student;
import com.touhouqing.grabteacherbackend.model.entity.Teacher;
import com.touhouqing.grabteacherbackend.mapper.CourseScheduleMapper;
import com.touhouqing.grabteacherbackend.mapper.CourseEnrollmentMapper;
import com.touhouqing.grabteacherbackend.mapper.StudentMapper;
import com.touhouqing.grabteacherbackend.mapper.TeacherMapper;


import com.touhouqing.grabteacherbackend.security.UserPrincipal;
import com.touhouqing.grabteacherbackend.service.RescheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.time.LocalTime;
import java.util.List;

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

    @Autowired
    private CourseScheduleMapper courseScheduleMapper;

    @Autowired
    private CourseEnrollmentMapper courseEnrollmentMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private com.touhouqing.grabteacherbackend.service.TeacherDailyAvailabilityService dailyService;

    /**
     * 创建调课申请（学生操作）
     */
    @PostMapping("/request")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "创建调课申请", description = "学生创建调课申请")
    public ResponseEntity<CommonResult<RescheduleVO>> createRescheduleRequest(
            @Valid @RequestBody RescheduleApplyDTO request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            RescheduleVO response = rescheduleService.createRescheduleRequest(request, currentUser.getId());
            return ResponseEntity.ok(CommonResult.success("调课申请创建成功", response));
        } catch (RuntimeException e) {
            logger.warn("创建调课申请失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("创建调课申请异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("创建调课申请失败"));
        }
    }

    /**
     * 管理员审批调课申请
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员审批调课申请", description = "管理员审批调课申请")
    public ResponseEntity<CommonResult<RescheduleVO>> approveRescheduleRequest(
            @Parameter(description = "调课申请ID", required = true) @PathVariable Long id,
            @Valid @RequestBody RescheduleApprovalDTO approval,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            RescheduleVO response = rescheduleService.adminApproveRescheduleRequest(id, approval, currentUser.getId());
            return ResponseEntity.ok(CommonResult.success("审批成功", response));
        } catch (RuntimeException e) {
            logger.warn("审批调课申请失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("审批调课申请异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("审批失败"));
        }
    }

    /**
     * 取消调课申请（学生操作）
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "取消调课申请", description = "学生取消自己的调课申请")
    public ResponseEntity<CommonResult<RescheduleVO>> cancelRescheduleRequest(
            @Parameter(description = "调课申请ID", required = true) @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            RescheduleVO response = rescheduleService.cancelRescheduleRequest(id, currentUser.getId());
            return ResponseEntity.ok(CommonResult.success("取消成功", response));
        } catch (RuntimeException e) {
            logger.warn("取消调课申请失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("取消调课申请异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("取消失败"));
        }
    }

    /**
     * 获取学生的调课申请列表
     */
    @GetMapping("/student/requests")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "获取学生调课申请列表", description = "学生查看自己的调课申请列表")
    public ResponseEntity<CommonResult<Page<RescheduleVO>>> getStudentRescheduleRequests(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "状态筛选", example = "pending") @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            Page<RescheduleVO> result = rescheduleService.getStudentRescheduleRequests(
                    currentUser.getId(), page, size, status);
            return ResponseEntity.ok(CommonResult.success("获取成功", result));
        } catch (RuntimeException e) {
            logger.warn("获取学生调课申请列表失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("获取学生调课申请列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }

    /**
     * 管理员获取所有调课申请列表
     */
    @GetMapping("/admin/requests")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员获取调课申请列表", description = "管理员查看所有调课申请列表")
    public ResponseEntity<CommonResult<Page<RescheduleVO>>> getAdminRescheduleRequests(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "状态筛选", example = "pending") @RequestParam(required = false) String status) {
        try {
            Page<RescheduleVO> result = rescheduleService.getAllRescheduleRequests(page, size, status);
            return ResponseEntity.ok(CommonResult.success("获取成功", result));
        } catch (RuntimeException e) {
            logger.warn("获取管理员调课申请列表失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("获取管理员调课申请列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }

    /**
     * 根据ID获取调课申请详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "获取调课申请详情", description = "根据ID获取调课申请的详细信息")
    public ResponseEntity<CommonResult<RescheduleVO>> getRescheduleRequestById(
            @Parameter(description = "调课申请ID", required = true) @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            String userType = currentUser.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "").toLowerCase();
            RescheduleVO response = rescheduleService.getRescheduleRequestById(id, currentUser.getId(), userType);
            return ResponseEntity.ok(CommonResult.success("获取成功", response));
        } catch (RuntimeException e) {
            logger.warn("获取调课申请详情失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("获取调课申请详情异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }

    /**
     * 获取教师待处理的调课申请数量
     */
    @GetMapping("/teacher/pending-count")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "获取待处理调课申请数量", description = "教师获取待处理的调课申请数量")
    public ResponseEntity<CommonResult<Integer>> getPendingRescheduleCount(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            int count = rescheduleService.getPendingRescheduleCount(currentUser.getId());
            return ResponseEntity.ok(CommonResult.success("获取成功", count));
        } catch (Exception e) {
            logger.error("获取待处理调课申请数量异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }

    /**
     * 检查是否可以申请调课
     */
    @GetMapping("/can-apply/{scheduleId}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "检查是否可以申请调课", description = "学生检查指定课程是否可以申请调课")
    public ResponseEntity<CommonResult<Boolean>> canApplyReschedule(
            @Parameter(description = "课程安排ID", required = true) @PathVariable Long scheduleId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            boolean canApply = rescheduleService.canApplyReschedule(scheduleId, currentUser.getId());
            return ResponseEntity.ok(CommonResult.success("检查完成", canApply));
        } catch (Exception e) {
            logger.error("检查是否可以申请调课异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("检查失败"));
        }
    }

    /**
     * 检查调课时间冲突
     */
    @GetMapping("/check-conflict/{scheduleId}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "检查调课时间冲突", description = "学生检查调课新时间是否与现有课程冲突")
    public ResponseEntity<CommonResult<RescheduleTimeCheckResult>> checkRescheduleTimeConflict(
            @Parameter(description = "课程安排ID", required = true) @PathVariable Long scheduleId,
            @Parameter(description = "新日期", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newDate,
            @Parameter(description = "新开始时间", required = true) @RequestParam String newStartTime,
            @Parameter(description = "新结束时间", required = true) @RequestParam String newEndTime,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            // 获取课程安排信息
            CourseSchedule schedule = courseScheduleMapper.findById(scheduleId);
            if (schedule == null) {
                return ResponseEntity.badRequest()
                        .body(CommonResult.error("课程安排不存在"));
            }

            // 验证学生权限
            Student student = studentMapper.findByUserId(currentUser.getId());
            CourseEnrollment enrollment = courseEnrollmentMapper.selectById(schedule.getEnrollmentId());
            if (student == null || enrollment == null || !enrollment.getStudentId().equals(student.getId())) {
                return ResponseEntity.badRequest()
                        .body(CommonResult.error("无权限操作此课程安排"));
            }

            // 首先检查教师可用时间
            try {
                validateSingleRescheduleTime(schedule.getTeacherId(), newDate, newStartTime, newEndTime);
            } catch (RuntimeException e) {
                // 如果教师时间不可用，返回冲突
                RescheduleTimeCheckResult result = new RescheduleTimeCheckResult(
                    true, "teacher_unavailable", e.getMessage()
                );
                return ResponseEntity.ok(CommonResult.success("检查完成", result));
            }

            // 然后检查时间冲突
            boolean hasConflict = rescheduleService.hasTimeConflict(
                    enrollment.getTeacherId(),
                    enrollment.getStudentId(),
                    newDate.toString(),
                    newStartTime,
                    newEndTime,
                    scheduleId
            );

            RescheduleTimeCheckResult result = new RescheduleTimeCheckResult(
                hasConflict,
                hasConflict ? "time_conflict" : null,
                hasConflict ? "该时间段已有其他课程安排，请选择其他时间" : "时间可用"
            );

            return ResponseEntity.ok(CommonResult.success("检查完成", result));
        } catch (Exception e) {
            logger.error("检查调课时间冲突异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("检查失败"));
        }
    }

    /**
     * 验证单次调课时间是否在教师可预约时间范围内
     * 参考预约试听课的逻辑
     */
    private void validateSingleRescheduleTime(Long teacherId, LocalDate date, String startTime, String endTime) {
        // 严格按“日历”可用性校验：当日未开放则不可调课；时间段需被某个当日可用段完全包含
        String requestedTimeSlot = startTime + "-" + endTime;
        var daily = dailyService.getDailyAvailability(teacherId, date, date);
        var daySlots = daily.getOrDefault(date, java.util.Collections.emptyList());
        if (daySlots.isEmpty()) {
            throw new RuntimeException("教师未在该日开放可预约时间");
        }
        boolean ok = daySlots.stream().anyMatch(available -> isTimeSlotContained(requestedTimeSlot, available));
        if (!ok) {
            throw new RuntimeException("该时间不在教师该日开放的可预约时间内");
        }
        logger.info("单次调课时间验证通过，教师ID: {}, 日期: {}, 时间: {}", teacherId, date, requestedTimeSlot);
    }

    /**
     * 检查请求的时间段是否完全在可预约时间段内
     */
    private boolean isTimeSlotContained(String requestedTimeSlot, String availableTimeSlot) {
        if (requestedTimeSlot == null || availableTimeSlot == null) {
            return false;
        }

        try {
            String[] requestedTimes = requestedTimeSlot.split("-");
            String[] availableTimes = availableTimeSlot.split("-");

            LocalTime requestedStart = LocalTime.parse(requestedTimes[0]);
            LocalTime requestedEnd = LocalTime.parse(requestedTimes[1]);
            LocalTime availableStart = LocalTime.parse(availableTimes[0]);
            LocalTime availableEnd = LocalTime.parse(availableTimes[1]);

            // 检查请求的时间段是否完全在可预约时间段内
            return !requestedStart.isBefore(availableStart) && !requestedEnd.isAfter(availableEnd);
        } catch (Exception e) {
            logger.error("时间段比较失败: requested={}, available={}", requestedTimeSlot, availableTimeSlot, e);
            return false;
        }
    }

    /**
     * 获取星期几的中文名称
     */
    private String getWeekdayName(int weekday) {
        String[] names = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        return weekday >= 1 && weekday <= 7 ? names[weekday] : "未知";
    }

    /**
     * 教师端：检查调课时间冲突
     */
    @GetMapping("/teacher/check-conflict/{scheduleId}")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "教师检查调课时间冲突", description = "教师检查调课新时间是否与现有课程冲突")
    public ResponseEntity<CommonResult<RescheduleTimeCheckResult>> checkTeacherRescheduleTimeConflict(
            @Parameter(description = "课程安排ID", required = true) @PathVariable Long scheduleId,
            @Parameter(description = "新日期", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newDate,
            @Parameter(description = "新开始时间", required = true) @RequestParam String newStartTime,
            @Parameter(description = "新结束时间", required = true) @RequestParam String newEndTime,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            // 获取课程安排信息
            CourseSchedule schedule = courseScheduleMapper.findById(scheduleId);
            if (schedule == null) {
                return ResponseEntity.badRequest()
                        .body(CommonResult.error("课程安排不存在"));
            }

            // 验证教师权限
            Teacher teacher = teacherMapper.findByUserId(currentUser.getId());
            CourseEnrollment enrollment2 = courseEnrollmentMapper.selectById(schedule.getEnrollmentId());
            if (teacher == null || enrollment2 == null || !enrollment2.getTeacherId().equals(teacher.getId())) {
                return ResponseEntity.badRequest()
                        .body(CommonResult.error("无权限操作此课程安排"));
            }

            // 首先检查教师可用时间
            try {
                validateSingleRescheduleTime(schedule.getTeacherId(), newDate, newStartTime, newEndTime);
            } catch (RuntimeException e) {
                // 如果教师时间不可用，返回冲突
                RescheduleTimeCheckResult result = new RescheduleTimeCheckResult(
                    true, "teacher_unavailable", e.getMessage()
                );
                return ResponseEntity.ok(CommonResult.success("检查完成", result));
            }

            // 然后检查时间冲突
            boolean hasConflict = rescheduleService.hasTimeConflict(
                    enrollment2.getTeacherId(),
                    enrollment2.getStudentId(),
                    newDate.toString(),
                    newStartTime,
                    newEndTime,
                    scheduleId
            );

            RescheduleTimeCheckResult result = new RescheduleTimeCheckResult(
                hasConflict,
                hasConflict ? "time_conflict" : null,
                hasConflict ? "该时间段已有其他课程安排，请选择其他时间" : "时间可用"
            );

            return ResponseEntity.ok(CommonResult.success("检查完成", result));
        } catch (Exception e) {
            logger.error("教师检查调课时间冲突异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("检查失败"));
        }
    }

    /**
     * 教师端：调课记录列表（含筛选）
     */
    @GetMapping("/teacher/requests")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "教师获取调课记录", description = "教师查看全部调课/请假记录，支持筛选")
    public ResponseEntity<CommonResult<Page<RescheduleVO>>> getTeacherRescheduleRequests(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "审批状态", example = "pending") @RequestParam(required = false) String status,
            @Parameter(description = "年份", example = "2025") @RequestParam(required = false) Integer year,
            @Parameter(description = "月份(1-12)", example = "9") @RequestParam(required = false) Integer month,
            @Parameter(description = "类型：single/recurring/cancel", example = "single") @RequestParam(required = false) String requestType,
            @Parameter(description = "申请人类型：student/teacher", example = "student") @RequestParam(required = false) String applicantType,
            @Parameter(description = "申请人姓名关键词") @RequestParam(required = false) String applicantName,
            @Parameter(description = "课程名称关键词") @RequestParam(required = false) String courseName,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            Page<RescheduleVO> result = rescheduleService.getTeacherRescheduleRequests(
                    currentUser.getId(), page, size, status, year, month, requestType, applicantType, applicantName, courseName);
            return ResponseEntity.ok(CommonResult.success("获取成功", result));
        } catch (RuntimeException e) {
            logger.warn("获取教师调课记录失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("获取教师调课记录异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }

    /**
     * 教师端：创建调课申请
     */
    @PostMapping("/teacher/request")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "教师创建调课申请", description = "教师创建调课申请")
    public ResponseEntity<CommonResult<RescheduleVO>> createTeacherRescheduleRequest(
            @Valid @RequestBody RescheduleApplyDTO request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            // 验证教师权限
            Teacher teacher = teacherMapper.findByUserId(currentUser.getId());
            if (teacher == null) {
                return ResponseEntity.badRequest()
                        .body(CommonResult.error("教师信息不存在"));
            }

            // 验证课程安排是否属于该教师
            CourseSchedule schedule = courseScheduleMapper.findById(request.getScheduleId());
            if (schedule == null || !schedule.getTeacherId().equals(teacher.getId())) {
                return ResponseEntity.badRequest()
                        .body(CommonResult.error("无权限操作此课程安排"));
            }

            // 调用服务创建调课申请
            RescheduleVO response = rescheduleService.createTeacherRescheduleRequest(request, currentUser.getId());
            return ResponseEntity.ok(CommonResult.success("调课申请创建成功", response));
        } catch (RuntimeException e) {
            logger.warn("教师创建调课申请失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("教师创建调课申请异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("创建调课申请失败"));
        }
    }
}

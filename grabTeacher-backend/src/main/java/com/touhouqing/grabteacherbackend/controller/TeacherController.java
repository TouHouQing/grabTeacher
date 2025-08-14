package com.touhouqing.grabteacherbackend.controller;

import com.touhouqing.grabteacherbackend.common.result.CommonResult;
import com.touhouqing.grabteacherbackend.model.vo.TeacherDetailVO;
import com.touhouqing.grabteacherbackend.model.dto.TeacherInfoDTO;
import com.touhouqing.grabteacherbackend.model.vo.TeacherListVO;
import com.touhouqing.grabteacherbackend.model.dto.TeacherMatchDTO;
import com.touhouqing.grabteacherbackend.model.vo.TeacherMatchVO;
import com.touhouqing.grabteacherbackend.model.vo.TeacherProfileVO;
import com.touhouqing.grabteacherbackend.model.vo.TeacherScheduleVO;
import com.touhouqing.grabteacherbackend.model.dto.TimeSlotAvailabilityDTO;
import com.touhouqing.grabteacherbackend.model.dto.TimeValidationResultDTO;
import com.touhouqing.grabteacherbackend.model.entity.Teacher;
import com.touhouqing.grabteacherbackend.security.UserPrincipal;
import com.touhouqing.grabteacherbackend.service.TeacherService;
import com.touhouqing.grabteacherbackend.service.TimeValidationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TeacherController {

    private static final Logger logger = LoggerFactory.getLogger(TeacherController.class);

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private TimeValidationService timeValidationService;

    /**
     * 获取教师个人信息（包含科目信息）
     */
    @GetMapping("/profile")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<CommonResult<TeacherProfileVO>> getProfile(Authentication authentication) {
        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            TeacherProfileVO teacherProfile = teacherService.getTeacherProfileByUserId(userPrincipal.getId());

            if (teacherProfile == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(CommonResult.error("教师信息不存在"));
            }

            return ResponseEntity.ok(CommonResult.success("获取成功", teacherProfile));
        } catch (Exception e) {
            logger.error("获取教师信息异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }

    /**
     * 更新教师信息
     */
    @PutMapping("/profile")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<CommonResult<Teacher>> updateProfile(
            @Valid @RequestBody TeacherInfoDTO request,
            Authentication authentication) {
        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            Teacher updatedTeacher = teacherService.updateTeacherInfo(userPrincipal.getId(), request);

            return ResponseEntity.ok(CommonResult.success("更新成功", updatedTeacher));
        } catch (RuntimeException e) {
            logger.warn("更新教师信息失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("更新教师信息异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("更新失败"));
        }
    }

    /**
     * 获取教师列表（公开接口，用于学生浏览）
     */
    @GetMapping("/list")
    public ResponseEntity<CommonResult<List<TeacherListVO>>> getTeacherList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String keyword) {
        try {
            // 查询归一化：去除首尾空白；空字符串统一为 null，减少缓存键碎片
            String normSubject = normalizeParam(subject);
            String normGrade = normalizeParam(grade);
            String normKeyword = normalizeKeyword(keyword);

            List<TeacherListVO> teachers = teacherService.getTeacherListWithSubjects(page, size, normSubject, normGrade, normKeyword);
            return ResponseEntity.ok(CommonResult.success("获取成功", teachers));
        } catch (Exception e) {
            logger.error("获取教师列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }

    /**
     * 获取精选教师列表（天下名师页面使用）
     */
    @GetMapping("/featured")
    public ResponseEntity<CommonResult<List<TeacherListVO>>> getFeaturedTeachers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String keyword) {
        try {
            String normSubject = normalizeParam(subject);
            String normGrade = normalizeParam(grade);
            String normKeyword = normalizeKeyword(keyword);

            List<TeacherListVO> teachers = teacherService.getFeaturedTeachers(page, size, normSubject, normGrade, normKeyword);
            return ResponseEntity.ok(CommonResult.success("获取成功", teachers));
        } catch (Exception e) {
            logger.error("获取精选教师列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }

    /**
     * 获取教师详情（公开接口）
     */
    @GetMapping("/{teacherId}")
    public ResponseEntity<CommonResult<TeacherDetailVO>> getTeacherDetail(@PathVariable Long teacherId) {
        try {
            TeacherDetailVO teacherDetail = teacherService.getTeacherDetailById(teacherId);

            if (teacherDetail == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(CommonResult.error("教师不存在"));
            }

            return ResponseEntity.ok(CommonResult.success("获取成功", teacherDetail));
        } catch (Exception e) {
            logger.error("获取教师详情异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }

    /**
     * 匹配教师（公开接口）
     */
    @PostMapping("/match")
    public ResponseEntity<CommonResult<List<TeacherMatchVO>>> matchTeachers(
            @Valid @RequestBody TeacherMatchDTO request) {
        try {
            List<TeacherMatchVO> matchedTeachers = teacherService.matchTeachers(request);
            return ResponseEntity.ok(CommonResult.success("匹配成功", matchedTeachers));
        } catch (Exception e) {
            logger.error("匹配教师异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("匹配失败"));
        }
    }

    /**
     * 获取所有可用的年级选项（公开接口）
     */
    @GetMapping("/grades")
    public ResponseEntity<CommonResult<List<String>>> getAvailableGrades() {
        try {
            List<String> grades = teacherService.getAvailableGrades();
            return ResponseEntity.ok(CommonResult.success("获取年级选项成功", grades));
        } catch (Exception e) {
            logger.error("获取年级选项异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }

    /**
     * 获取教师的公开课表（供学生查看）
     */
    @GetMapping("/{teacherId}/schedule")
    public ResponseEntity<CommonResult<TeacherScheduleVO>> getTeacherPublicSchedule(
            @PathVariable Long teacherId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            TeacherScheduleVO schedule = teacherService.getTeacherPublicSchedule(teacherId, startDate, endDate);
            return ResponseEntity.ok(CommonResult.success("获取教师课表成功", schedule));
        } catch (Exception e) {
            logger.error("获取教师课表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }

    /**
     * 检查教师时间段可用性（供学生预约时查看）
     */
    @GetMapping("/{teacherId}/availability")
    public ResponseEntity<CommonResult<List<TimeSlotAvailabilityDTO>>> checkTeacherAvailability(
            @PathVariable Long teacherId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) List<String> timeSlots) {
        try {
            List<TimeSlotAvailabilityDTO> availability = teacherService.checkTeacherAvailability(teacherId, startDate, endDate, timeSlots);
            return ResponseEntity.ok(CommonResult.success("获取时间段可用性成功", availability));
        } catch (Exception e) {
            logger.error("检查教师时间段可用性异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }

    /**
     * 获取教师控制台统计数据
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<CommonResult<Map<String, Object>>> getStatistics(Authentication authentication) {
        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            Map<String, Object> statistics = teacherService.getTeacherStatistics(userPrincipal.getId());

            return ResponseEntity.ok(CommonResult.success("获取成功", statistics));
        } catch (Exception e) {
            logger.error("获取教师统计数据异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }

    /**
     * 验证学生预约时间匹配度（供学生预约时使用）
     */
    @PostMapping("/{teacherId}/validate-booking-time")
    public ResponseEntity<CommonResult<TimeValidationResultDTO>> validateStudentBookingTime(
            @PathVariable Long teacherId,
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Integer> weekdays = (List<Integer>) request.get("weekdays");
            @SuppressWarnings("unchecked")
            List<String> timeSlots = (List<String>) request.get("timeSlots");
            String startDateStr = (String) request.get("startDate");
            String endDateStr = (String) request.get("endDate");
            Integer totalTimes = (Integer) request.get("totalTimes");

            if (weekdays == null || timeSlots == null) {
                return ResponseEntity.badRequest()
                        .body(CommonResult.error("星期几和时间段不能为空"));
            }

            TimeValidationResultDTO result;

            // 如果提供了日期范围，使用更准确的周期性预约验证
            if (startDateStr != null && endDateStr != null) {
                LocalDate startDate = LocalDate.parse(startDateStr);
                LocalDate endDate = LocalDate.parse(endDateStr);
                result = timeValidationService.validateRecurringBookingTime(
                        teacherId, weekdays, timeSlots, startDate, endDate, totalTimes);
            } else {
                // 否则使用简单的时间匹配验证
                result = timeValidationService.validateStudentBookingTime(
                        teacherId, weekdays, timeSlots);
            }

            return ResponseEntity.ok(CommonResult.success("验证完成", result));
        } catch (Exception e) {
            logger.error("验证学生预约时间异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("验证失败"));
        }
    }

    // 参数归一化：去首尾空白；空串->null，减少缓存键碎片
    private String normalizeParam(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    // 关键词归一化：去首尾空白，合并连续空白为单个空格；空串->null
    private String normalizeKeyword(String v) {
        if (v == null) return null;
        String t = v.trim().replaceAll("\\s+", " ");
        return t.isEmpty() ? null : t;
    }

}
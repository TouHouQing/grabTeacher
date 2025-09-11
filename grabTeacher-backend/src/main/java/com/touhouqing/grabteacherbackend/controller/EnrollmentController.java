package com.touhouqing.grabteacherbackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.touhouqing.grabteacherbackend.common.result.CommonResult;
import com.touhouqing.grabteacherbackend.mapper.CourseEnrollmentMapper;
import com.touhouqing.grabteacherbackend.mapper.CourseMapper;
import com.touhouqing.grabteacherbackend.mapper.StudentMapper;
import com.touhouqing.grabteacherbackend.mapper.TeacherMapper;
import com.touhouqing.grabteacherbackend.mapper.SubjectMapper;
import com.touhouqing.grabteacherbackend.mapper.CourseScheduleMapper;
import com.touhouqing.grabteacherbackend.model.dto.TimeSlotDTO;
import com.touhouqing.grabteacherbackend.model.entity.Course;
import com.touhouqing.grabteacherbackend.model.entity.CourseEnrollment;
import com.touhouqing.grabteacherbackend.model.entity.CourseSchedule;
import com.touhouqing.grabteacherbackend.model.entity.Student;
import com.touhouqing.grabteacherbackend.model.entity.Teacher;
import com.touhouqing.grabteacherbackend.model.entity.Subject;
import com.touhouqing.grabteacherbackend.model.vo.SuspendedEnrollmentVO;
import com.touhouqing.grabteacherbackend.security.UserPrincipal;
import com.touhouqing.grabteacherbackend.service.StudentService;
import com.touhouqing.grabteacherbackend.service.BookingService;
import com.touhouqing.grabteacherbackend.service.CacheKeyEvictor;
import com.touhouqing.grabteacherbackend.service.TeacherScheduleCacheService;
import com.touhouqing.grabteacherbackend.util.TimeSlotUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;


import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
@Tag(name = "报名关系", description = "报名查询相关接口")
public class EnrollmentController {

    @Autowired
    private CourseEnrollmentMapper courseEnrollmentMapper;
    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private TeacherMapper teacherMapper;
    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private StudentService studentService;

    @Autowired
    private SubjectMapper subjectMapper;

    // 新增依赖：用于生成课程安排与缓存回填
    @Autowired
    private CourseScheduleMapper courseScheduleMapper;
    @Autowired
    private BookingService bookingService;
    @Autowired
    private CacheKeyEvictor cacheKeyEvictor;
    @Autowired
    private TeacherScheduleCacheService teacherScheduleCacheService;

    @GetMapping("/student/suspended")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "学生查询已停课报名", description = "返回当前学生 enrollment_status = suspended 的报名列表")
    public CommonResult<List<SuspendedEnrollmentVO>> getStudentSuspended(@AuthenticationPrincipal UserPrincipal currentUser) {
        Student student = studentMapper.findByUserId(currentUser.getId());
        if (student == null) {
            return CommonResult.error("学生信息不存在");
        }
        QueryWrapper<CourseEnrollment> qw = new QueryWrapper<>();
        qw.eq("student_id", student.getId()).eq("enrollment_status", "suspended").eq("is_deleted", false);
        List<CourseEnrollment> list = courseEnrollmentMapper.selectList(qw);
        List<SuspendedEnrollmentVO> vos = new ArrayList<>();
        for (CourseEnrollment ce : list) {
            Teacher t = ce.getTeacherId() != null ? teacherMapper.selectById(ce.getTeacherId()) : null;
            Course c = ce.getCourseId() != null ? courseMapper.selectById(ce.getCourseId()) : null;
            String subjectName = null;
            if (c != null && c.getSubjectId() != null) {
                Subject s = subjectMapper.selectById(c.getSubjectId());
                subjectName = s != null ? s.getName() : null;
            }
            SuspendedEnrollmentVO vo = SuspendedEnrollmentVO.builder()
                    .id(ce.getId())
                    .studentId(ce.getStudentId())
                    .studentName(student.getRealName())
                    .teacherId(ce.getTeacherId())
                    .teacherName(t != null ? t.getRealName() : null)
                    .courseId(ce.getCourseId())
                    .courseTitle(c != null ? c.getTitle() : null)
                    .subjectName(subjectName)
                    .totalTimes(ce.getTotalSessions())
                    .completedTimes(ce.getCompletedSessions())
                    .durationMinutes(c != null ? c.getDurationMinutes() : null)
                    .build();
            vos.add(vo);
        }
        return CommonResult.success("获取成功", vos);
    }

    @GetMapping("/by-booking/{bookingRequestId}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "根据预约申请ID查询报名", description = "用于从已审批预约占位卡解析报名ID（若已创建报名）")
    public CommonResult<?> getEnrollmentByBooking(@PathVariable("bookingRequestId") Long bookingRequestId,
                                                  @AuthenticationPrincipal UserPrincipal currentUser) {
        Student student = studentMapper.findByUserId(currentUser.getId());
        if (student == null) {
            return CommonResult.error("学生信息不存在");
        }
        QueryWrapper<CourseEnrollment> qw = new QueryWrapper<>();
        qw.eq("booking_request_id", bookingRequestId)
          .eq("student_id", student.getId())
          .eq("is_deleted", false);
        CourseEnrollment ce = courseEnrollmentMapper.selectOne(qw);
        if (ce == null) {
            return CommonResult.error("未找到对应报名");
        }
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("id", ce.getId());
        data.put("studentId", ce.getStudentId());
        data.put("teacherId", ce.getTeacherId());
        data.put("courseId", ce.getCourseId());
        data.put("enrollmentStatus", ce.getEnrollmentStatus());
        return CommonResult.success("获取成功", data);
    }



    @PostMapping("/course/{courseId}/enroll")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "学生报名大班课", description = "学生对大班课进行报名并扣除相应M豆")
    public CommonResult<?> enrollCourse(@PathVariable("courseId") Long courseId,
                                        @AuthenticationPrincipal UserPrincipal currentUser) {
        // 1) 校验学生与课程
        Student student = studentMapper.findByUserId(currentUser.getId());
        if (student == null) {
            return CommonResult.error("学生信息不存在");
        }
        Course course = courseMapper.selectById(courseId);
        if (course == null || Boolean.TRUE.equals(course.getDeleted())) {
            return CommonResult.error("课程不存在或已下架");
        }
        if (!"active".equalsIgnoreCase(course.getStatus())) {
            return CommonResult.error("课程未开放报名，请联系客服预约课程");
        }
        if (!"large_class".equalsIgnoreCase(course.getCourseType())) {
            return CommonResult.error("该课程暂不支持一键报名，请联系客服预约课程");
        }

        // 2) 价格与余额校验
        BigDecimal price = course.getPrice();
        if (price == null || price.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return CommonResult.error("课程价格未配置，请联系客服预约课程");
        }

        // 已报名校验（避免重复扣费）
        QueryWrapper<CourseEnrollment> qw = new QueryWrapper<>();
        qw.eq("student_id", student.getId())
          .eq("course_id", course.getId())
          .eq("is_deleted", false)
          .in("enrollment_status", "active", "suspended");
        CourseEnrollment existed = courseEnrollmentMapper.selectOne(qw);
        if (existed != null) {
            return CommonResult.success("您已报名该课程", existed);
        }

        // 3) 余额检查
        if (!studentService.checkBalance(currentUser.getId(), price)) {
            return CommonResult.error("您的M豆余额不足，请联系客服预约课程");
        }

        // 4) 扣费
        boolean deducted = studentService.updateStudentBalance(currentUser.getId(), price.negate(), "报名课程费用", null, null);
        if (!deducted) {
            return CommonResult.error("扣费失败，请稍后重试");
        }

        // 4.1) 原子占位：报名人数+1并判断是否满员（失败则回滚扣费）
        int affected = courseMapper.incrementEnrollmentAndSetFullIfNeeded(course.getId());
        if (affected <= 0) {
            // 占位失败，尝试退款
            studentService.updateStudentBalance(currentUser.getId(), price, "报名失败退款", null, null);
            return CommonResult.error("报名失败，课程已满或不可报名");
        }

        // 5) 创建报名关系
        CourseEnrollment enrollment = CourseEnrollment.builder()
                .studentId(student.getId())
                .teacherId(course.getTeacherId())
                .courseId(course.getId())
                .enrollmentType("large_class")
                .totalSessions(null)
                .completedSessions(0)
                .enrollmentStatus("active")
                .enrollmentDate(java.time.LocalDate.now())
                .startDate(course.getStartDate())
                .endDate(course.getEndDate())
                .trial(false)
                .deleted(false)
                .build();
        courseEnrollmentMapper.insert(enrollment);

        // 6) 生成课程安排（基于课程模板 course_time_slots）
        try {
            if (course.getStartDate() != null && course.getEndDate() != null &&
                course.getCourseTimeSlots() != null && !course.getCourseTimeSlots().trim().isEmpty()) {

                java.util.List<TimeSlotDTO> timeSlotDTOs = TimeSlotUtil.fromJsonString(course.getCourseTimeSlots());
                if (timeSlotDTOs != null && !timeSlotDTOs.isEmpty()) {
                    // 建立 weekday -> slots 映射
                    java.util.Map<Integer, java.util.List<String>> weekdaySlots = new java.util.HashMap<>();
                    for (TimeSlotDTO dto : timeSlotDTOs) {
                        if (dto.getWeekday() != null && dto.getTimeSlots() != null && !dto.getTimeSlots().isEmpty()) {
                            weekdaySlots.computeIfAbsent(dto.getWeekday(), k -> new java.util.ArrayList<>()).addAll(dto.getTimeSlots());
                        }
                    }

                    int sessionNumber = 1;
                    java.util.Set<java.time.LocalDate> affectedDates = new java.util.HashSet<>();
                    java.time.LocalDate d = course.getStartDate();
                    while (!d.isAfter(course.getEndDate())) {
                        int weekday = d.getDayOfWeek().getValue(); // 1=周一..7=周日
                        java.util.List<String> slots = weekdaySlots.get(weekday);
                        if (slots != null && !slots.isEmpty()) {
                            for (String slot : slots) {
                                String[] times = slot.split("-");
                                if (times.length == 2) {
                                    String startStr = times[0];
                                    String endStr = times[1];

                                    // 学生冲突校验：避免与学生自身其他课程冲突
                                    boolean studentConflict = false;
                                    try {
                                        studentConflict = bookingService.hasStudentTimeConflict(student.getId(), d, startStr, endStr);
                                    } catch (Exception ignore) {}

                                    if (!studentConflict) {
                                        java.time.LocalTime startTime = java.time.LocalTime.parse(startStr);
                                        java.time.LocalTime endTime = java.time.LocalTime.parse(endStr);

                                        CourseSchedule cs = CourseSchedule.builder()
                                                .enrollmentId(enrollment.getId())
                                                .scheduledDate(d)
                                                .startTime(startTime)
                                                .endTime(endTime)
                                                .sessionNumber(sessionNumber)
                                                .scheduleStatus("scheduled")
                                                .deleted(false)
                                                .build();
                                        courseScheduleMapper.insert(cs);
                                        sessionNumber++;
                                        affectedDates.add(d);
                                    }
                                }
                            }
                        }
                        d = d.plusDays(1);
                    }

                    // 精确清理并回填教师忙时缓存
                    try {
                        if (!affectedDates.isEmpty()) {
                            cacheKeyEvictor.evictTeacherScheduleAndAvailability(course.getTeacherId(), affectedDates);
                            for (java.time.LocalDate date : affectedDates) {
                                java.util.List<CourseSchedule> dayAll =
                                        courseScheduleMapper.findByTeacherIdAndDateRange(course.getTeacherId(), date, date);
                                java.util.List<String> busy = new java.util.ArrayList<>();
                                for (CourseSchedule s : dayAll) {
                                    busy.add(s.getStartTime().toString() + "-" + s.getEndTime().toString());
                                }
                                teacherScheduleCacheService.putBusySlots(course.getTeacherId(), date,
                                        busy.isEmpty() ? java.util.Collections.emptyList() : busy);
                            }
                        }
                    } catch (Exception ignore) {}
                }
            }
        } catch (Exception e) {
            // 不影响主流程
        }

        return CommonResult.success("报名成功", enrollment);
    }
}
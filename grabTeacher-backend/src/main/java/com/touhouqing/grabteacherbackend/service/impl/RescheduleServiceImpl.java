package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.model.dto.RescheduleApprovalDTO;
import com.touhouqing.grabteacherbackend.model.dto.RescheduleApplyDTO;
import com.touhouqing.grabteacherbackend.model.entity.*;
import com.touhouqing.grabteacherbackend.model.vo.RescheduleVO;
import com.touhouqing.grabteacherbackend.mapper.*;
import com.touhouqing.grabteacherbackend.service.RescheduleService;
import com.touhouqing.grabteacherbackend.service.TeacherCacheWarmupService;
import com.touhouqing.grabteacherbackend.service.CacheKeyEvictor;
import com.touhouqing.grabteacherbackend.service.TeacherScheduleCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 调课服务实现类
 */
@Slf4j
@Service
public class RescheduleServiceImpl implements RescheduleService {

    @Autowired
    private RescheduleRequestMapper rescheduleRequestMapper;

    @Autowired
    private ScheduleMapper scheduleMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private SubjectMapper subjectMapper;

    @Autowired
    private TeacherCacheWarmupService teacherCacheWarmupService;

    @Autowired
    private CacheKeyEvictor cacheKeyEvictor;

    @Autowired
    private TeacherScheduleCacheService teacherScheduleCacheService;

    @Override
    @Transactional
    public RescheduleVO createRescheduleRequest(RescheduleApplyDTO request, Long studentUserId) {
        log.info("创建调课申请，学生用户ID: {}, 课程安排ID: {}", studentUserId, request.getScheduleId());

        // 获取学生信息
        Student student = studentMapper.findByUserId(studentUserId);
        if (student == null) {
            throw new RuntimeException("学生信息不存在");
        }

        // 获取课程安排信息
        Schedule schedule = scheduleMapper.selectById(request.getScheduleId());
        if (schedule == null || schedule.getIsDeleted()) {
            throw new RuntimeException("课程安排不存在");
        }

        // 验证学生权限
        if (!schedule.getStudentId().equals(student.getId())) {
            throw new RuntimeException("无权限操作此课程安排");
        }

        // 检查是否可以申请调课
        if (!canApplyReschedule(request.getScheduleId(), studentUserId)) {
            throw new RuntimeException("当前课程不允许申请调课");
        }

        // 检查是否已有待处理的调课申请
        QueryWrapper<RescheduleRequest> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("schedule_id", request.getScheduleId())
                   .eq("applicant_id", student.getId())
                   .eq("applicant_type", "student")
                   .eq("status", "pending")
                   .eq("is_deleted", false);
        
        RescheduleRequest existingRequest = rescheduleRequestMapper.selectOne(queryWrapper);
        if (existingRequest != null) {
            throw new RuntimeException("该课程已有待处理的调课申请，请等待审批结果");
        }

        // 计算提前通知小时数
        int advanceNoticeHours = calculateAdvanceNoticeHours(schedule.getScheduledDate(), schedule.getStartTime());

        // 创建调课申请
        RescheduleRequest rescheduleRequest = RescheduleRequest.builder()
                .scheduleId(request.getScheduleId())
                .applicantId(student.getId())
                .applicantType("student")
                .requestType(request.getRequestType())
                .originalDate(schedule.getScheduledDate())
                .originalStartTime(schedule.getStartTime())
                .originalEndTime(schedule.getEndTime())
                .newDate(request.getNewDate())
                .newStartTime(request.getNewStartTime())
                .newEndTime(request.getNewEndTime())
                .newWeeklySchedule(buildWeeklyScheduleString(request.getNewRecurringWeekdays(), request.getNewRecurringTimeSlots()))
                .reason(request.getReason())
                .urgencyLevel(request.getUrgencyLevel())
                .advanceNoticeHours(advanceNoticeHours)
                .affectsFutureSessions(request.getAffectsFutureSessions())
                .status("pending")

                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isDeleted(false)
                .build();

        rescheduleRequestMapper.insert(rescheduleRequest);

        log.info("调课申请创建成功，ID: {}", rescheduleRequest.getId());
        return convertToRescheduleResponseDTO(rescheduleRequest);
    }

    @Override
    @Transactional
    public RescheduleVO approveRescheduleRequest(Long rescheduleId, RescheduleApprovalDTO approval, Long teacherUserId) {
        log.info("教师审批调课申请，申请ID: {}, 教师用户ID: {}, 状态: {}", rescheduleId, teacherUserId, approval.getStatus());

        RescheduleRequest rescheduleRequest = rescheduleRequestMapper.selectById(rescheduleId);
        if (rescheduleRequest == null || rescheduleRequest.getIsDeleted()) {
            throw new RuntimeException("调课申请不存在");
        }

        // 获取教师信息
        Teacher teacher = teacherMapper.findByUserId(teacherUserId);
        if (teacher == null) {
            throw new RuntimeException("教师信息不存在");
        }

        // 获取课程安排信息验证权限
        Schedule schedule = scheduleMapper.selectById(rescheduleRequest.getScheduleId());
        if (schedule == null || !schedule.getTeacherId().equals(teacher.getId())) {
            throw new RuntimeException("无权限操作此调课申请");
        }

        if (!"pending".equals(rescheduleRequest.getStatus())) {
            throw new RuntimeException("该调课申请已被处理");
        }

        // 如果是同意申请，需要检查时间冲突
        if ("approved".equals(approval.getStatus()) && "single".equals(rescheduleRequest.getRequestType())) {
            boolean hasConflict = hasTimeConflict(
                teacher.getId(),
                schedule.getStudentId(),
                rescheduleRequest.getNewDate().toString(),
                rescheduleRequest.getNewStartTime().toString(),
                rescheduleRequest.getNewEndTime().toString(),
                schedule.getId()
            );
            
            if (hasConflict) {
                throw new RuntimeException("新的时间安排存在冲突，请选择其他时间");
            }
        }

        // 更新调课申请状态
        rescheduleRequest.setStatus(approval.getStatus());
        rescheduleRequest.setReviewerId(teacher.getId());
        rescheduleRequest.setReviewerType("teacher");
        rescheduleRequest.setReviewNotes(approval.getReviewNotes());
        rescheduleRequest.setCompensationAmount(approval.getCompensationAmount());
        if (approval.getAffectsFutureSessions() != null) {
            rescheduleRequest.setAffectsFutureSessions(approval.getAffectsFutureSessions());
        }
        rescheduleRequest.setReviewedAt(LocalDateTime.now());
        rescheduleRequest.setUpdatedAt(LocalDateTime.now());
        


        rescheduleRequestMapper.updateById(rescheduleRequest);

        // 如果审批通过，更新课程安排
        if ("approved".equals(approval.getStatus())) {
            updateScheduleAfterApproval(rescheduleRequest, schedule);

            // 课表变化后，精准清理 + 立即回填教师 busy 缓存
            try {
                Long teacherId = schedule.getTeacherId();
                java.util.Set<java.time.LocalDate> affectedDates = new java.util.HashSet<>();
                if ("single".equals(rescheduleRequest.getRequestType())) {
                    if (rescheduleRequest.getOriginalDate() != null) affectedDates.add(rescheduleRequest.getOriginalDate());
                    if (rescheduleRequest.getNewDate() != null) affectedDates.add(rescheduleRequest.getNewDate());
                } else if ("cancel".equals(rescheduleRequest.getRequestType())) {
                    if (schedule.getScheduledDate() != null) affectedDates.add(schedule.getScheduledDate());
                }
                cacheKeyEvictor.evictTeacherScheduleAndAvailability(teacherId, affectedDates);
                if (!affectedDates.isEmpty()) {
                    for (java.time.LocalDate d : affectedDates) {
                        java.util.List<Schedule> dayAll = scheduleMapper.findByTeacherIdAndDate(teacherId, d);
                        java.util.List<String> slots = new java.util.ArrayList<>();
                        for (Schedule s : dayAll) {
                            slots.add(s.getStartTime().toString() + "-" + s.getEndTime().toString());
                        }
                        teacherScheduleCacheService.putBusySlots(teacherId, d,
                            slots.isEmpty() ? java.util.Collections.emptyList() : slots);
                    }
                }
            } catch (Exception e) {
                log.warn("精准清理/回填教师缓存失败，但不影响主流程", e);
            }
        }

        log.info("调课申请审批完成，状态: {}", approval.getStatus());
        return convertToRescheduleResponseDTO(rescheduleRequest);
    }

    @Override
    @Transactional
    public RescheduleVO cancelRescheduleRequest(Long rescheduleId, Long studentUserId) {
        log.info("取消调课申请，申请ID: {}, 学生用户ID: {}", rescheduleId, studentUserId);

        RescheduleRequest rescheduleRequest = rescheduleRequestMapper.selectById(rescheduleId);
        if (rescheduleRequest == null || rescheduleRequest.getIsDeleted()) {
            throw new RuntimeException("调课申请不存在");
        }

        // 获取学生信息
        Student student = studentMapper.findByUserId(studentUserId);
        if (student == null) {
            throw new RuntimeException("学生信息不存在");
        }

        // 验证权限
        if (!rescheduleRequest.getApplicantId().equals(student.getId()) || 
            !"student".equals(rescheduleRequest.getApplicantType())) {
            throw new RuntimeException("无权限操作此调课申请");
        }

        if (!"pending".equals(rescheduleRequest.getStatus())) {
            throw new RuntimeException("只能取消待处理的调课申请");
        }

        // 更新状态为已取消
        rescheduleRequest.setStatus("cancelled");
        rescheduleRequest.setUpdatedAt(LocalDateTime.now());
        rescheduleRequestMapper.updateById(rescheduleRequest);

        log.info("调课申请取消成功");
        return convertToRescheduleResponseDTO(rescheduleRequest);
    }

    @Override
    public Page<RescheduleVO> getStudentRescheduleRequests(Long studentUserId, int page, int size, String status) {
        log.info("获取学生调课申请列表，学生用户ID: {}, 页码: {}, 大小: {}, 状态: {}", studentUserId, page, size, status);

        Student student = studentMapper.findByUserId(studentUserId);
        if (student == null) {
            throw new RuntimeException("学生信息不存在");
        }

        Page<RescheduleRequest> requestPage = new Page<>(page, size);
        Page<RescheduleRequest> resultPage = rescheduleRequestMapper.findByApplicantWithPage(
            requestPage, student.getId(), "student", status);

        Page<RescheduleVO> responsePage = new Page<>(page, size);
        responsePage.setTotal(resultPage.getTotal());
        responsePage.setRecords(
            resultPage.getRecords().stream()
                .map(this::convertToRescheduleResponseDTO)
                .collect(Collectors.toList())
        );

        return responsePage;
    }

    @Override
    public Page<RescheduleVO> getTeacherRescheduleRequests(Long teacherUserId, int page, int size, String status) {
        log.info("获取教师调课申请列表，教师用户ID: {}, 页码: {}, 大小: {}, 状态: {}", teacherUserId, page, size, status);

        Teacher teacher = teacherMapper.findByUserId(teacherUserId);
        if (teacher == null) {
            throw new RuntimeException("教师信息不存在");
        }

        Page<RescheduleRequest> requestPage = new Page<>(page, size);
        Page<RescheduleRequest> resultPage = rescheduleRequestMapper.findByTeacherIdWithPage(
            requestPage, teacher.getId(), status);

        Page<RescheduleVO> responsePage = new Page<>(page, size);
        responsePage.setTotal(resultPage.getTotal());
        responsePage.setRecords(
            resultPage.getRecords().stream()
                .map(this::convertToRescheduleResponseDTO)
                .collect(Collectors.toList())
        );

        return responsePage;
    }

    @Override
    public RescheduleVO getRescheduleRequestById(Long rescheduleId, Long currentUserId, String userType) {
        log.info("获取调课申请详情，申请ID: {}, 用户ID: {}, 用户类型: {}", rescheduleId, currentUserId, userType);

        RescheduleRequest rescheduleRequest = rescheduleRequestMapper.selectById(rescheduleId);
        if (rescheduleRequest == null || rescheduleRequest.getIsDeleted()) {
            throw new RuntimeException("调课申请不存在");
        }

        // 权限验证
        if (!hasPermissionToViewReschedule(rescheduleRequest, currentUserId, userType)) {
            throw new RuntimeException("无权限查看此调课申请");
        }

        return convertToRescheduleResponseDTO(rescheduleRequest);
    }

    @Override
    public int getPendingRescheduleCount(Long teacherUserId) {
        Teacher teacher = teacherMapper.findByUserId(teacherUserId);
        if (teacher == null) {
            return 0;
        }
        return rescheduleRequestMapper.countPendingByTeacherId(teacher.getId());
    }

    @Override
    public boolean canApplyReschedule(Long scheduleId, Long studentUserId) {
        Schedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null || schedule.getIsDeleted()) {
            return false;
        }

        Student student = studentMapper.findByUserId(studentUserId);
        if (student == null || !schedule.getStudentId().equals(student.getId())) {
            return false;
        }

        // 检查课程状态
        if (!"progressing".equals(schedule.getStatus())) {
            return false;
        }

        // 检查是否在课程开始前
        LocalDateTime scheduleDateTime = LocalDateTime.of(schedule.getScheduledDate(), schedule.getStartTime());
        if (scheduleDateTime.isBefore(LocalDateTime.now().plusHours(2))) {
            return false; // 课程开始前2小时内不允许调课
        }

        return true;
    }

    @Override
    public boolean hasTimeConflict(Long teacherId, Long studentId, String newDate, String newStartTime, String newEndTime, Long excludeScheduleId) {
        try {
            LocalDate date = LocalDate.parse(newDate);
            LocalTime startTime = LocalTime.parse(newStartTime);
            LocalTime endTime = LocalTime.parse(newEndTime);

            // 检查教师时间冲突
            QueryWrapper<Schedule> teacherQuery = new QueryWrapper<>();
            teacherQuery.eq("teacher_id", teacherId)
                       .eq("scheduled_date", date)
                       .eq("is_deleted", false)
                       .ne("status", "cancelled");

            if (excludeScheduleId != null) {
                teacherQuery.ne("id", excludeScheduleId);
            }

            List<Schedule> teacherSchedules = scheduleMapper.selectList(teacherQuery);
            for (Schedule schedule : teacherSchedules) {
                if (isTimeOverlap(startTime, endTime, schedule.getStartTime(), schedule.getEndTime())) {
                    return true;
                }
            }

            // 检查学生时间冲突
            QueryWrapper<Schedule> studentQuery = new QueryWrapper<>();
            studentQuery.eq("student_id", studentId)
                       .eq("scheduled_date", date)
                       .eq("is_deleted", false)
                       .ne("status", "cancelled");

            if (excludeScheduleId != null) {
                studentQuery.ne("id", excludeScheduleId);
            }

            List<Schedule> studentSchedules = scheduleMapper.selectList(studentQuery);
            for (Schedule schedule : studentSchedules) {
                if (isTimeOverlap(startTime, endTime, schedule.getStartTime(), schedule.getEndTime())) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            log.error("检查时间冲突时发生错误", e);
            return true; // 出错时保守处理，认为有冲突
        }
    }

    /**
     * 检查时间是否重叠
     */
    private boolean isTimeOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    /**
     * 计算提前通知小时数
     */
    private int calculateAdvanceNoticeHours(LocalDate scheduleDate, LocalTime scheduleTime) {
        LocalDateTime scheduleDateTime = LocalDateTime.of(scheduleDate, scheduleTime);
        LocalDateTime now = LocalDateTime.now();

        if (scheduleDateTime.isBefore(now)) {
            return 0;
        }

        return (int) java.time.Duration.between(now, scheduleDateTime).toHours();
    }

    /**
     * 构建周期性安排字符串
     */
    private String buildWeeklyScheduleString(List<Integer> weekdays, List<String> timeSlots) {
        if (weekdays == null || timeSlots == null || weekdays.isEmpty() || timeSlots.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        String[] weekdayNames = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};

        for (int i = 0; i < weekdays.size(); i++) {
            if (i > 0) sb.append(";");
            sb.append(weekdayNames[weekdays.get(i)]);
            if (i < timeSlots.size()) {
                sb.append(" ").append(timeSlots.get(i));
            }
        }

        return sb.toString();
    }

    /**
     * 审批通过后更新课程安排
     */
    private void updateScheduleAfterApproval(RescheduleRequest rescheduleRequest, Schedule schedule) {
        if ("single".equals(rescheduleRequest.getRequestType())) {
            // 单次调课：更新课程安排时间
            schedule.setScheduledDate(rescheduleRequest.getNewDate());
            schedule.setStartTime(rescheduleRequest.getNewStartTime());
            schedule.setEndTime(rescheduleRequest.getNewEndTime());
            scheduleMapper.updateById(schedule);

            // 重新计算同一预约申请下所有课程的session_number
            if (schedule.getBookingRequestId() != null) {
                recalculateSessionNumbers(schedule.getBookingRequestId());
            }
        } else if ("recurring".equals(rescheduleRequest.getRequestType())) {
            // 周期性调课：需要更新后续所有课程安排
            // 这里简化处理，实际可能需要更复杂的逻辑
            log.info("周期性调课审批通过，需要更新后续课程安排");
        } else if ("cancel".equals(rescheduleRequest.getRequestType())) {
            // 取消课程：更新课程状态
            schedule.setStatus("cancelled");
            scheduleMapper.updateById(schedule);
        }
    }

    /**
     * 重新计算指定预约申请下所有课程的session_number
     * 根据实际上课时间（日期+时间）重新排序
     */
    private void recalculateSessionNumbers(Long bookingRequestId) {
        log.info("重新计算预约申请 {} 下所有课程的session_number", bookingRequestId);

        // 获取该预约申请下的所有课程安排，按时间排序
        List<Schedule> schedules = scheduleMapper.findByBookingRequestId(bookingRequestId);

        if (schedules.isEmpty()) {
            log.warn("预约申请 {} 下没有找到课程安排", bookingRequestId);
            return;
        }

        // 按实际上课时间排序（日期+时间）
        schedules.sort((s1, s2) -> {
            // 首先按日期排序
            int dateCompare = s1.getScheduledDate().compareTo(s2.getScheduledDate());
            if (dateCompare != 0) {
                return dateCompare;
            }
            // 如果日期相同，按开始时间排序
            return s1.getStartTime().compareTo(s2.getStartTime());
        });

        // 重新分配session_number
        for (int i = 0; i < schedules.size(); i++) {
            Schedule schedule = schedules.get(i);
            int newSessionNumber = i + 1;

            // 只有当session_number发生变化时才更新
            if (schedule.getSessionNumber() == null || !schedule.getSessionNumber().equals(newSessionNumber)) {
                schedule.setSessionNumber(newSessionNumber);
                scheduleMapper.updateById(schedule);
                log.debug("更新课程安排 {} 的session_number: {} -> {}",
                         schedule.getId(), schedule.getSessionNumber(), newSessionNumber);
            }
        }

        log.info("预约申请 {} 下共 {} 个课程安排的session_number重新计算完成", bookingRequestId, schedules.size());
    }

    /**
     * 检查是否有权限查看调课申请
     */
    private boolean hasPermissionToViewReschedule(RescheduleRequest rescheduleRequest, Long currentUserId, String userType) {
        if ("admin".equals(userType)) {
            return true;
        }

        if ("student".equals(userType)) {
            Student student = studentMapper.findByUserId(currentUserId);
            return student != null &&
                   rescheduleRequest.getApplicantId().equals(student.getId()) &&
                   "student".equals(rescheduleRequest.getApplicantType());
        }

        if ("teacher".equals(userType)) {
            Teacher teacher = teacherMapper.findByUserId(currentUserId);
            if (teacher == null) return false;

            Schedule schedule = scheduleMapper.selectById(rescheduleRequest.getScheduleId());
            return schedule != null && schedule.getTeacherId().equals(teacher.getId());
        }

        return false;
    }

    /**
     * 转换为响应DTO
     */
    private RescheduleVO convertToRescheduleResponseDTO(RescheduleRequest rescheduleRequest) {
        RescheduleVO.RescheduleVOBuilder builder = RescheduleVO.builder()
                .id(rescheduleRequest.getId())
                .scheduleId(rescheduleRequest.getScheduleId())
                .applicantId(rescheduleRequest.getApplicantId())
                .applicantType(rescheduleRequest.getApplicantType())
                .requestType(rescheduleRequest.getRequestType())
                .originalDate(rescheduleRequest.getOriginalDate())
                .originalStartTime(rescheduleRequest.getOriginalStartTime())
                .originalEndTime(rescheduleRequest.getOriginalEndTime())
                .newDate(rescheduleRequest.getNewDate())
                .newStartTime(rescheduleRequest.getNewStartTime())
                .newEndTime(rescheduleRequest.getNewEndTime())
                .newWeeklySchedule(rescheduleRequest.getNewWeeklySchedule())
                .reason(rescheduleRequest.getReason())
                .urgencyLevel(rescheduleRequest.getUrgencyLevel())
                .advanceNoticeHours(rescheduleRequest.getAdvanceNoticeHours())
                .status(rescheduleRequest.getStatus())
                .statusDisplay(getStatusDisplay(rescheduleRequest.getStatus()))
                .reviewerId(rescheduleRequest.getReviewerId())
                .reviewerType(rescheduleRequest.getReviewerType())
                .reviewNotes(rescheduleRequest.getReviewNotes())
                .compensationAmount(rescheduleRequest.getCompensationAmount())
                .affectsFutureSessions(rescheduleRequest.getAffectsFutureSessions())
                .reviewedAt(rescheduleRequest.getReviewedAt())

                .createdAt(rescheduleRequest.getCreatedAt())
                .updatedAt(rescheduleRequest.getUpdatedAt());

        // 获取关联信息
        try {
            Schedule schedule = scheduleMapper.selectById(rescheduleRequest.getScheduleId());
            if (schedule != null) {
                Course course = courseMapper.selectById(schedule.getCourseId());
                if (course != null) {
                    builder.courseTitle(course.getTitle());

                    Subject subject = subjectMapper.selectById(course.getSubjectId());
                    if (subject != null) {
                        builder.subjectName(subject.getName());
                    }
                }

                Teacher teacher = teacherMapper.selectById(schedule.getTeacherId());
                if (teacher != null) {
                    builder.teacherName(teacher.getRealName());
                }

                Student student = studentMapper.selectById(schedule.getStudentId());
                if (student != null) {
                    builder.studentName(student.getRealName());
                }
            }

            // 获取申请人姓名
            if ("student".equals(rescheduleRequest.getApplicantType())) {
                Student applicant = studentMapper.selectById(rescheduleRequest.getApplicantId());
                if (applicant != null) {
                    builder.applicantName(applicant.getRealName());
                }
            } else if ("teacher".equals(rescheduleRequest.getApplicantType())) {
                Teacher applicant = teacherMapper.selectById(rescheduleRequest.getApplicantId());
                if (applicant != null) {
                    builder.applicantName(applicant.getRealName());
                }
            }

            // 获取审核人姓名
            if (rescheduleRequest.getReviewerId() != null) {
                if ("teacher".equals(rescheduleRequest.getReviewerType())) {
                    Teacher reviewer = teacherMapper.selectById(rescheduleRequest.getReviewerId());
                    if (reviewer != null) {
                        builder.reviewerName(reviewer.getRealName());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取调课申请关联信息时发生错误", e);
        }

        return builder.build();
    }

    /**
     * 获取状态显示文本
     */
    private String getStatusDisplay(String status) {
        switch (status) {
            case "pending": return "待审批";
            case "approved": return "已同意";
            case "rejected": return "已拒绝";
            case "cancelled": return "已取消";
            default: return status;
        }
    }
}

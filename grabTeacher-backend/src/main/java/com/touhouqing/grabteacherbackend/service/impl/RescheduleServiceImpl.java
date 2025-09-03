package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.model.dto.RescheduleApprovalDTO;
import com.touhouqing.grabteacherbackend.model.dto.RescheduleApplyDTO;
import com.touhouqing.grabteacherbackend.model.entity.*;
import com.touhouqing.grabteacherbackend.model.vo.RescheduleVO;
import com.touhouqing.grabteacherbackend.mapper.*;
import com.touhouqing.grabteacherbackend.service.RescheduleService;
import com.touhouqing.grabteacherbackend.service.CacheKeyEvictor;
import com.touhouqing.grabteacherbackend.service.TeacherScheduleCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 调课服务实现类
 */
@Slf4j
@Service
public class RescheduleServiceImpl implements RescheduleService {

    @Autowired
    private RescheduleRequestMapper rescheduleRequestMapper;

    // 旧表已迁移，移除未使用的 ScheduleMapper

    @Autowired
    private CourseScheduleMapper courseScheduleMapper;

    @Autowired
    private CourseEnrollmentMapper courseEnrollmentMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private SubjectMapper subjectMapper;

    @Autowired
    private UserMapper userMapper;

    // 删除未使用的 teacherCacheWarmupService 以消除告警

    @Autowired
    private CacheKeyEvictor cacheKeyEvictor;

    @Autowired
    private TeacherScheduleCacheService teacherScheduleCacheService;

    @Autowired
    private BalanceTransactionMapper balanceTransactionMapper;

    @Autowired
    private HourDetailMapper hourDetailMapper;

    @Override
    @Transactional
    public RescheduleVO createRescheduleRequest(RescheduleApplyDTO request, Long studentUserId) {
        log.info("创建调课申请，学生用户ID: {}, 课程安排ID: {}", studentUserId, request.getScheduleId());

        // 获取学生信息
        Student student = studentMapper.findByUserId(studentUserId);
        if (student == null) {
            throw new RuntimeException("学生信息不存在");
        }

        // 检查用户本月调课次数是否足够
        User user = userMapper.selectById(studentUserId);
        if (user == null || user.getDeleted()) {
            throw new RuntimeException("用户不存在");
        }
        Integer adjustmentTimes = user.getAdjustmentTimes();
        boolean overQuota = (adjustmentTimes == null || adjustmentTimes <= 0);

        // 获取课程安排信息（新表）
        CourseSchedule schedule = courseScheduleMapper.findById(request.getScheduleId());
        if (schedule == null || Boolean.TRUE.equals(schedule.getDeleted())) {
            throw new RuntimeException("课程安排不存在");
        }

        CourseEnrollment enrollment = courseEnrollmentMapper.selectById(schedule.getEnrollmentId());
        if (enrollment == null || Boolean.TRUE.equals(enrollment.getDeleted())) {
            throw new RuntimeException("报名关系不存在");
        }

        // 验证学生权限
        if (!enrollment.getStudentId().equals(student.getId())) {
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

        // 单次调课需校验：距原定开课时间需≥4小时
        if ("single".equals(request.getRequestType())) {
            LocalDateTime scheduleDateTime = LocalDateTime.of(schedule.getScheduledDate(), schedule.getStartTime());
            if (!scheduleDateTime.isAfter(LocalDateTime.now().plusHours(4))) {
                throw new RuntimeException("单次调课需在开课前4小时之外发起");
            }
        }

        // 计算提前通知小时数
        int advanceNoticeHours = calculateAdvanceNoticeHours(schedule.getScheduledDate(), schedule.getStartTime());

        // 若超额且余额不足以扣除0.5h对应M豆，则不允许创建申请
        if (overQuota) {
            Course course = courseMapper.selectById(enrollment.getCourseId());
            if (course == null || course.getPrice() == null) {
                throw new RuntimeException("课程价格缺失，无法计算超额调课扣费");
            }
            BigDecimal halfHourBeans = course.getPrice().multiply(new BigDecimal("0.5"));
            BigDecimal currentBalance = student.getBalance() == null ? BigDecimal.ZERO : student.getBalance();
            if (currentBalance.compareTo(halfHourBeans) < 0) {
                throw new RuntimeException("学生余额不足，无法进行超额调课");
            }
        }

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
                .deleted(false)
                .build();

        rescheduleRequestMapper.insert(rescheduleRequest);

        // 扣减一次调课次数
        // 优先尝试扣减一次调课次数（若本月次数为0则不变更）
        userMapper.decrementAdjustmentTimes(studentUserId);

        // 若超额，按照规则扣补：学生扣0.5h对应M豆，教师+1h
        if (overQuota) {
            Course course = courseMapper.selectById(schedule.getCourseId());
            if (course != null && course.getPrice() != null) {
                BigDecimal halfHourBeans = course.getPrice().multiply(new BigDecimal("0.5"));
                // 学生扣除M豆并记录流水
                BigDecimal before = student.getBalance() == null ? BigDecimal.ZERO : student.getBalance();
                BigDecimal after = before.subtract(halfHourBeans);
                studentMapper.incrementBalance(student.getId(), halfHourBeans.negate());
                BalanceTransaction tx = BalanceTransaction.builder()
                        .userId(student.getUserId())
                        .name(student.getRealName())
                        .amount(halfHourBeans.negate())
                        .balanceBefore(before)
                        .balanceAfter(after)
                        .transactionType("DEDUCT")
                        .reason("超额调课扣除0.5小时M豆")
                        .bookingId(schedule.getBookingRequestId())
                        .createdAt(LocalDateTime.now())
                        .build();
                balanceTransactionMapper.insert(tx);
            }
            // 教师课时 +1h
            teacherMapper.incrementCurrentHours(schedule.getTeacherId(), BigDecimal.ONE);
            // 写入教师课时明细
            Teacher targetTeacher = teacherMapper.selectById(schedule.getTeacherId());
            if (targetTeacher != null) {
                HourDetail hd = HourDetail.builder()
                        .userId(targetTeacher.getUserId())
                        .name(targetTeacher.getRealName())
                        .hours(new BigDecimal("1"))
                        .transactionType(1)
                        .reason("学生超额调课补偿")
                        .bookingId(schedule.getId())
                        .createdAt(LocalDateTime.now())
                        .build();
                hourDetailMapper.insert(hd);
            }
        }

        log.info("调课申请创建成功，ID: {}", rescheduleRequest.getId());
        return convertToRescheduleResponseDTO(rescheduleRequest);
    }

    @Override
    @Transactional
    public RescheduleVO createTeacherRescheduleRequest(RescheduleApplyDTO request, Long teacherUserId) {
        log.info("教师创建调课申请，教师用户ID: {}, 课程安排ID: {}", teacherUserId, request.getScheduleId());

        // 获取教师信息
        Teacher teacher = teacherMapper.findByUserId(teacherUserId);
        if (teacher == null) {
            throw new RuntimeException("教师信息不存在");
        }

        // 检查用户本月调课次数是否足够
        User user = userMapper.selectById(teacherUserId);
        if (user == null || user.getDeleted()) {
            throw new RuntimeException("用户不存在");
        }
        Integer adjustmentTimes = user.getAdjustmentTimes();
        boolean overQuota = (adjustmentTimes == null || adjustmentTimes <= 0);

        // 获取课程安排信息
        CourseSchedule schedule = courseScheduleMapper.findById(request.getScheduleId());
        if (schedule == null || schedule.getDeleted()) {
            throw new RuntimeException("课程安排不存在");
        }

        // 验证教师权限
        if (!schedule.getTeacherId().equals(teacher.getId())) {
            throw new RuntimeException("无权限操作此课程安排");
        }

        // 检查是否已有待处理的调课申请
        QueryWrapper<RescheduleRequest> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("schedule_id", request.getScheduleId())
                   .eq("applicant_id", teacher.getId())
                   .eq("applicant_type", "teacher")
                   .eq("status", "pending")
                   .eq("is_deleted", false);
        
        RescheduleRequest existingRequest = rescheduleRequestMapper.selectOne(queryWrapper);
        if (existingRequest != null) {
            throw new RuntimeException("该课程已有待处理的调课申请，请等待审批结果");
        }

        // 单次调课需校验：距原定开课时间需≥4小时
        if ("single".equals(request.getRequestType())) {
            LocalDateTime scheduleDateTime = LocalDateTime.of(schedule.getScheduledDate(), schedule.getStartTime());
            if (!scheduleDateTime.isAfter(LocalDateTime.now().plusHours(4))) {
                throw new RuntimeException("单次调课需在开课前4小时之外发起");
            }
        }

        // 计算提前通知小时数
        int advanceNoticeHours = calculateAdvanceNoticeHours(schedule.getScheduledDate(), schedule.getStartTime());

        // 创建调课申请
        RescheduleRequest rescheduleRequest = RescheduleRequest.builder()
                .scheduleId(request.getScheduleId())
                .applicantId(teacher.getId())
                .applicantType("teacher")
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
                .deleted(false)
                .build();

        rescheduleRequestMapper.insert(rescheduleRequest);

        // 扣减一次调课次数
        // 优先尝试扣减一次调课次数（若本月次数为0则不变更）
        userMapper.decrementAdjustmentTimes(teacherUserId);

        // 若超额，按照规则扣补：教师扣1h、学生补偿0.5h对应M豆
        if (overQuota) {
            // 教师课时 -1h（不低于0）
            teacherMapper.decrementCurrentHours(teacher.getId(), BigDecimal.ONE);
            // 写入教师课时明细
            HourDetail hd = HourDetail.builder()
                    .userId(teacher.getUserId())
                    .name(teacher.getRealName())
                    .hours(new BigDecimal("-1"))
                    .transactionType(0)
                    .reason("教师超额调课扣减")
                    .bookingId(schedule.getId())
                    .createdAt(LocalDateTime.now())
                    .build();
            hourDetailMapper.insert(hd);
            // 学生补偿M豆并记录流水
            Course course = courseMapper.selectById(schedule.getCourseId());
            if (course != null && course.getPrice() != null) {
                BigDecimal halfHourBeans = course.getPrice().multiply(new BigDecimal("0.5"));
                Student targetStudent = studentMapper.selectById(schedule.getStudentId());
                if (targetStudent != null) {
                    BigDecimal before = targetStudent.getBalance() == null ? BigDecimal.ZERO : targetStudent.getBalance();
                    BigDecimal after = before.add(halfHourBeans);
                    studentMapper.incrementBalance(targetStudent.getId(), halfHourBeans);
                    BalanceTransaction tx = BalanceTransaction.builder()
                            .userId(targetStudent.getUserId())
                            .name(targetStudent.getRealName())
                            .amount(halfHourBeans)
                            .balanceBefore(before)
                            .balanceAfter(after)
                            .transactionType("REFUND")
                            .reason("教师超额调课补偿0.5小时M豆")
                            .bookingId(schedule.getBookingRequestId())
                            .createdAt(LocalDateTime.now())
                            .build();
                    balanceTransactionMapper.insert(tx);
                }
            }
        }

        log.info("教师调课申请创建成功，ID: {}", rescheduleRequest.getId());
        return convertToRescheduleResponseDTO(rescheduleRequest);
    }

    @Override
    @Transactional
    public RescheduleVO approveRescheduleRequest(Long rescheduleId, RescheduleApprovalDTO approval, Long teacherUserId) {
        log.info("教师审批调课申请，申请ID: {}, 教师用户ID: {}, 状态: {}", rescheduleId, teacherUserId, approval.getStatus());

        RescheduleRequest rescheduleRequest = rescheduleRequestMapper.selectById(rescheduleId);
        if (rescheduleRequest == null || rescheduleRequest.getDeleted()) {
            throw new RuntimeException("调课申请不存在");
        }

        // 获取教师信息
        Teacher teacher = teacherMapper.findByUserId(teacherUserId);
        if (teacher == null) {
            throw new RuntimeException("教师信息不存在");
        }

        // 获取课程安排信息验证权限
        CourseSchedule schedule = courseScheduleMapper.findById(rescheduleRequest.getScheduleId());
        if (schedule == null || !schedule.getTeacherId().equals(teacher.getId())) {
            throw new RuntimeException("无权限操作此调课申请");
        }

        if (!"pending".equals(rescheduleRequest.getStatus())) {
            throw new RuntimeException("该调课申请已被处理");
        }

        // 验证审批原因
        if (approval.getReviewNotes() == null || approval.getReviewNotes().trim().isEmpty()) {
            throw new RuntimeException("审批原因不能为空");
        }
        if (approval.getReviewNotes().trim().length() < 5) {
            throw new RuntimeException("审批原因至少需要5个字符");
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
        rescheduleRequest.setReviewNotes(approval.getReviewNotes().trim());
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
                Set<LocalDate> affectedDates = new HashSet<>();
                if ("single".equals(rescheduleRequest.getRequestType())) {
                    if (rescheduleRequest.getOriginalDate() != null) affectedDates.add(rescheduleRequest.getOriginalDate());
                    if (rescheduleRequest.getNewDate() != null) affectedDates.add(rescheduleRequest.getNewDate());
                } else if ("cancel".equals(rescheduleRequest.getRequestType())) {
                    if (schedule.getScheduledDate() != null) affectedDates.add(schedule.getScheduledDate());
                }
                cacheKeyEvictor.evictTeacherScheduleAndAvailability(teacherId, affectedDates);
                if (!affectedDates.isEmpty()) {
                    for (LocalDate d : affectedDates) {
                        List<CourseSchedule> dayAll = courseScheduleMapper.findByTeacherIdAndDateRange(teacherId, d, d);
                        List<String> slots = new ArrayList<>();
                        for (CourseSchedule s : dayAll) {
                            slots.add(s.getStartTime().toString() + "-" + s.getEndTime().toString());
                        }
                        teacherScheduleCacheService.putBusySlots(teacherId, d,
                            slots.isEmpty() ? Collections.emptyList() : slots);
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
        if (rescheduleRequest == null || rescheduleRequest.getDeleted()) {
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
    @Transactional
    public RescheduleVO adminApproveRescheduleRequest(Long rescheduleId, RescheduleApprovalDTO approval, Long adminUserId) {
        log.info("管理员审批调课申请，申请ID: {}, 管理员用户ID: {}, 状态: {}", rescheduleId, adminUserId, approval.getStatus());

        RescheduleRequest rescheduleRequest = rescheduleRequestMapper.selectById(rescheduleId);
        if (rescheduleRequest == null || rescheduleRequest.getDeleted()) {
            throw new RuntimeException("调课申请不存在");
        }

        if (!"pending".equals(rescheduleRequest.getStatus())) {
            throw new RuntimeException("该调课申请已被处理");
        }

        // 验证审批原因
        if (approval.getReviewNotes() == null || approval.getReviewNotes().trim().isEmpty()) {
            throw new RuntimeException("审批原因不能为空");
        }
        if (approval.getReviewNotes().trim().length() < 5) {
            throw new RuntimeException("审批原因至少需要5个字符");
        }

        // 获取课程安排信息
        CourseSchedule schedule = courseScheduleMapper.findById(rescheduleRequest.getScheduleId());
        if (schedule == null) {
            throw new RuntimeException("课程安排不存在");
        }

        // 如果是同意申请，需要检查时间冲突
        if ("approved".equals(approval.getStatus()) && "single".equals(rescheduleRequest.getRequestType())) {
            boolean hasConflict = hasTimeConflict(
                schedule.getTeacherId(),
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
        rescheduleRequest.setReviewerId(adminUserId);
        rescheduleRequest.setReviewerType("admin");
        rescheduleRequest.setReviewNotes(approval.getReviewNotes().trim());
        rescheduleRequest.setCompensationAmount(approval.getCompensationAmount());
        if (approval.getAffectsFutureSessions() != null) {
            rescheduleRequest.setAffectsFutureSessions(approval.getAffectsFutureSessions());
        }
        rescheduleRequest.setReviewedAt(LocalDateTime.now());
        rescheduleRequest.setUpdatedAt(LocalDateTime.now());

        rescheduleRequestMapper.updateById(rescheduleRequest);

        // 如果管理员拒绝，返还申请人一次调课次数
        if ("rejected".equals(approval.getStatus())) {
            Long applicantUserId = null;
            if ("student".equals(rescheduleRequest.getApplicantType())) {
                Student applicant = studentMapper.selectById(rescheduleRequest.getApplicantId());
                if (applicant != null) {
                    applicantUserId = applicant.getUserId();
                }
            } else if ("teacher".equals(rescheduleRequest.getApplicantType())) {
                Teacher applicant = teacherMapper.selectById(rescheduleRequest.getApplicantId());
                if (applicant != null) {
                    applicantUserId = applicant.getUserId();
                }
            }
            if (applicantUserId != null) {
                userMapper.incrementAdjustmentTimes(applicantUserId);
            }
        }

        // 如果审批通过，更新课程安排
        if ("approved".equals(approval.getStatus())) {
            updateScheduleAfterApproval(rescheduleRequest, schedule);

            // 课表变化后，精准清理 + 立即回填教师 busy 缓存
            try {
                Long teacherId = schedule.getTeacherId();
                Set<LocalDate> affectedDates = new HashSet<>();
                if ("single".equals(rescheduleRequest.getRequestType())) {
                    if (rescheduleRequest.getOriginalDate() != null) affectedDates.add(rescheduleRequest.getOriginalDate());
                    if (rescheduleRequest.getNewDate() != null) affectedDates.add(rescheduleRequest.getNewDate());
                } else if ("cancel".equals(rescheduleRequest.getRequestType())) {
                    if (schedule.getScheduledDate() != null) affectedDates.add(schedule.getScheduledDate());
                }
                cacheKeyEvictor.evictTeacherScheduleAndAvailability(teacherId, affectedDates);
                if (!affectedDates.isEmpty()) {
                    for (LocalDate d : affectedDates) {
                        List<CourseSchedule> dayAll = courseScheduleMapper.findByTeacherIdAndDateRange(teacherId, d, d);
                        List<String> slots = new ArrayList<>();
                        for (CourseSchedule s : dayAll) {
                            slots.add(s.getStartTime().toString() + "-" + s.getEndTime().toString());
                        }
                        teacherScheduleCacheService.putBusySlots(teacherId, d,
                            slots.isEmpty() ? Collections.emptyList() : slots);
                    }
                }
            } catch (Exception e) {
                log.warn("精准清理/回填教师缓存失败，但不影响主流程", e);
            }
        }

        log.info("管理员调课申请审批完成，状态: {}", approval.getStatus());
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

        // 批量装配，避免 N+1
        List<RescheduleRequest> records = resultPage.getRecords();
        List<Long> scheduleIds = records.stream().map(RescheduleRequest::getScheduleId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, CourseSchedule> scheduleMap = new HashMap<>();
        if (!scheduleIds.isEmpty()) {
            for (Long id : scheduleIds) {
                CourseSchedule cs = courseScheduleMapper.findById(id);
                if (cs != null) scheduleMap.put(id, cs);
            }
        }
        Map<Long, Course> courseMap = new HashMap<>();
        Map<Long, Subject> subjectMap = new HashMap<>();
        Map<Long, Teacher> teacherMap = new HashMap<>();
        Map<Long, Student> studentMap = new HashMap<>();
        if (!scheduleMap.isEmpty()) {
            List<Long> enrollmentIds = scheduleMap.values().stream().map(CourseSchedule::getEnrollmentId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            Map<Long, CourseEnrollment> enrollmentMap = new HashMap<>();
            for (Long eid : enrollmentIds) {
                CourseEnrollment ce = courseEnrollmentMapper.selectById(eid);
                if (ce != null) enrollmentMap.put(eid, ce);
            }

            List<Long> courseIds = enrollmentMap.values().stream().map(CourseEnrollment::getCourseId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            if (!courseIds.isEmpty()) {
                QueryWrapper<Course> cq = new QueryWrapper<>();
                cq.in("id", courseIds);
                for (Course c : courseMapper.selectList(cq)) {
                    courseMap.put(c.getId(), c);
                }
                List<Long> subjectIds = courseMap.values().stream().map(Course::getSubjectId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
                if (!subjectIds.isEmpty()) {
                    QueryWrapper<Subject> subjQ = new QueryWrapper<>();
                    subjQ.in("id", subjectIds);
                    for (Subject s : subjectMapper.selectList(subjQ)) subjectMap.put(s.getId(), s);
                }
            }
            List<Long> teacherIds = enrollmentMap.values().stream().map(CourseEnrollment::getTeacherId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            if (!teacherIds.isEmpty()) {
                QueryWrapper<Teacher> tq = new QueryWrapper<>();
                tq.in("id", teacherIds);
                for (Teacher t : teacherMapper.selectList(tq)) teacherMap.put(t.getId(), t);
            }
            List<Long> studentIds = enrollmentMap.values().stream().map(CourseEnrollment::getStudentId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            if (!studentIds.isEmpty()) {
                QueryWrapper<Student> stq = new QueryWrapper<>();
                stq.in("id", studentIds);
                for (Student s : studentMapper.selectList(stq)) studentMap.put(s.getId(), s);
            }
        }

        List<RescheduleVO> vos = new ArrayList<>();
        for (RescheduleRequest rr : records) {
            RescheduleVO vo = convertToRescheduleResponseDTO(rr);
            CourseSchedule s = scheduleMap.get(rr.getScheduleId());
            if (s != null) {
                CourseEnrollment ce = courseEnrollmentMapper.selectById(s.getEnrollmentId());
                Course c = ce != null ? courseMap.get(ce.getCourseId()) : null;
                if (c != null) {
                    vo.setCourseTitle(c.getTitle());
                    Subject subj = subjectMap.get(c.getSubjectId());
                    if (subj != null) vo.setSubjectName(subj.getName());
                }
                Teacher t = ce != null ? teacherMap.get(ce.getTeacherId()) : null;
                if (t != null) vo.setTeacherName(t.getRealName());
                Student stu = ce != null ? studentMap.get(ce.getStudentId()) : null;
                if (stu != null) vo.setStudentName(stu.getRealName());
            }
            vos.add(vo);
        }

        Page<RescheduleVO> responsePage = new Page<>(page, size);
        responsePage.setTotal(resultPage.getTotal());
        responsePage.setRecords(vos);
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

        // 批量装配，避免 N+1
        List<RescheduleRequest> records = resultPage.getRecords();
        List<Long> scheduleIds = records.stream().map(RescheduleRequest::getScheduleId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, CourseSchedule> scheduleMap = new HashMap<>();
        if (!scheduleIds.isEmpty()) {
            for (Long id : scheduleIds) {
                CourseSchedule cs = courseScheduleMapper.findById(id);
                if (cs != null) scheduleMap.put(id, cs);
            }
        }
        Map<Long, Course> courseMap = new HashMap<>();
        Map<Long, Subject> subjectMap = new HashMap<>();
        Map<Long, Teacher> teacherMap = new HashMap<>();
        Map<Long, Student> studentMap = new HashMap<>();
        if (!scheduleMap.isEmpty()) {
            List<Long> enrollmentIds = scheduleMap.values().stream().map(CourseSchedule::getEnrollmentId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            Map<Long, CourseEnrollment> enrollmentMap = new HashMap<>();
            for (Long eid : enrollmentIds) {
                CourseEnrollment ce = courseEnrollmentMapper.selectById(eid);
                if (ce != null) enrollmentMap.put(eid, ce);
            }

            List<Long> courseIds = enrollmentMap.values().stream().map(CourseEnrollment::getCourseId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            if (!courseIds.isEmpty()) {
                QueryWrapper<Course> cq = new QueryWrapper<>();
                cq.in("id", courseIds);
                for (Course c : courseMapper.selectList(cq)) {
                    courseMap.put(c.getId(), c);
                }
                List<Long> subjectIds = courseMap.values().stream().map(Course::getSubjectId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
                if (!subjectIds.isEmpty()) {
                    QueryWrapper<Subject> subjQ = new QueryWrapper<>();
                    subjQ.in("id", subjectIds);
                    for (Subject s : subjectMapper.selectList(subjQ)) subjectMap.put(s.getId(), s);
                }
            }
            List<Long> teacherIds = enrollmentMap.values().stream().map(CourseEnrollment::getTeacherId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            if (!teacherIds.isEmpty()) {
                QueryWrapper<Teacher> tq = new QueryWrapper<>();
                tq.in("id", teacherIds);
                for (Teacher t : teacherMapper.selectList(tq)) teacherMap.put(t.getId(), t);
            }
            List<Long> studentIds = enrollmentMap.values().stream().map(CourseEnrollment::getStudentId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            if (!studentIds.isEmpty()) {
                QueryWrapper<Student> stq = new QueryWrapper<>();
                stq.in("id", studentIds);
                for (Student s : studentMapper.selectList(stq)) studentMap.put(s.getId(), s);
            }
        }

        List<RescheduleVO> vos = new ArrayList<>();
        for (RescheduleRequest rr : records) {
            RescheduleVO vo = convertToRescheduleResponseDTO(rr);
            CourseSchedule s = scheduleMap.get(rr.getScheduleId());
            if (s != null) {
                CourseEnrollment ce = courseEnrollmentMapper.selectById(s.getEnrollmentId());
                Course c = ce != null ? courseMap.get(ce.getCourseId()) : null;
                if (c != null) {
                    vo.setCourseTitle(c.getTitle());
                    Subject subj = subjectMap.get(c.getSubjectId());
                    if (subj != null) vo.setSubjectName(subj.getName());
                }
                Teacher t = ce != null ? teacherMap.get(ce.getTeacherId()) : null;
                if (t != null) vo.setTeacherName(t.getRealName());
                Student stu = ce != null ? studentMap.get(ce.getStudentId()) : null;
                if (stu != null) vo.setStudentName(stu.getRealName());
            }
            vos.add(vo);
        }

        Page<RescheduleVO> responsePage = new Page<>(page, size);
        responsePage.setTotal(resultPage.getTotal());
        responsePage.setRecords(vos);
        return responsePage;
    }

    @Override
    public Page<RescheduleVO> getAllRescheduleRequests(int page, int size, String status) {
        log.info("管理员获取所有调课申请列表，页码: {}, 大小: {}, 状态: {}", page, size, status);

        Page<RescheduleRequest> requestPage = new Page<>(page, size);
        Page<RescheduleRequest> resultPage = rescheduleRequestMapper.findAllWithPage(requestPage, status);

        // 批量装配，避免 N+1
        List<RescheduleRequest> records = resultPage.getRecords();
        List<Long> scheduleIds = records.stream().map(RescheduleRequest::getScheduleId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, CourseSchedule> scheduleMap = new HashMap<>();
        if (!scheduleIds.isEmpty()) {
            for (Long id : scheduleIds) {
                CourseSchedule cs = courseScheduleMapper.findById(id);
                if (cs != null) scheduleMap.put(id, cs);
            }
        }
        Map<Long, Course> courseMap = new HashMap<>();
        Map<Long, Subject> subjectMap = new HashMap<>();
        Map<Long, Teacher> teacherMap = new HashMap<>();
        Map<Long, Student> studentMap = new HashMap<>();
        if (!scheduleMap.isEmpty()) {
            List<Long> enrollmentIds = scheduleMap.values().stream().map(CourseSchedule::getEnrollmentId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            Map<Long, CourseEnrollment> enrollmentMap = new HashMap<>();
            for (Long eid : enrollmentIds) {
                CourseEnrollment ce = courseEnrollmentMapper.selectById(eid);
                if (ce != null) enrollmentMap.put(eid, ce);
            }

            List<Long> courseIds = enrollmentMap.values().stream().map(CourseEnrollment::getCourseId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            if (!courseIds.isEmpty()) {
                QueryWrapper<Course> cq = new QueryWrapper<>();
                cq.in("id", courseIds);
                for (Course c : courseMapper.selectList(cq)) {
                    courseMap.put(c.getId(), c);
                }
                List<Long> subjectIds = courseMap.values().stream().map(Course::getSubjectId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
                if (!subjectIds.isEmpty()) {
                    QueryWrapper<Subject> subjQ = new QueryWrapper<>();
                    subjQ.in("id", subjectIds);
                    for (Subject s : subjectMapper.selectList(subjQ)) subjectMap.put(s.getId(), s);
                }
            }
            List<Long> teacherIds = enrollmentMap.values().stream().map(CourseEnrollment::getTeacherId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            if (!teacherIds.isEmpty()) {
                QueryWrapper<Teacher> tq = new QueryWrapper<>();
                tq.in("id", teacherIds);
                for (Teacher t : teacherMapper.selectList(tq)) teacherMap.put(t.getId(), t);
            }
            List<Long> studentIds = enrollmentMap.values().stream().map(CourseEnrollment::getStudentId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            if (!studentIds.isEmpty()) {
                QueryWrapper<Student> stq = new QueryWrapper<>();
                stq.in("id", studentIds);
                for (Student s : studentMapper.selectList(stq)) studentMap.put(s.getId(), s);
            }
        }

        List<RescheduleVO> vos = new ArrayList<>();
        for (RescheduleRequest rr : records) {
            RescheduleVO vo = convertToRescheduleResponseDTO(rr);
            CourseSchedule s = scheduleMap.get(rr.getScheduleId());
            if (s != null) {
                CourseEnrollment ce = courseEnrollmentMapper.selectById(s.getEnrollmentId());
                Course c = ce != null ? courseMap.get(ce.getCourseId()) : null;
                if (c != null) {
                    vo.setCourseTitle(c.getTitle());
                    Subject subj = subjectMap.get(c.getSubjectId());
                    if (subj != null) vo.setSubjectName(subj.getName());
                }
                Teacher t = ce != null ? teacherMap.get(ce.getTeacherId()) : null;
                if (t != null) vo.setTeacherName(t.getRealName());
                Student stu = ce != null ? studentMap.get(ce.getStudentId()) : null;
                if (stu != null) vo.setStudentName(stu.getRealName());
            }
            vos.add(vo);
        }

        Page<RescheduleVO> responsePage = new Page<>(page, size);
        responsePage.setTotal(resultPage.getTotal());
        responsePage.setRecords(vos);
        return responsePage;
    }

    @Override
    public RescheduleVO getRescheduleRequestById(Long rescheduleId, Long currentUserId, String userType) {
        log.info("获取调课申请详情，申请ID: {}, 用户ID: {}, 用户类型: {}", rescheduleId, currentUserId, userType);

        RescheduleRequest rescheduleRequest = rescheduleRequestMapper.selectById(rescheduleId);
        if (rescheduleRequest == null || rescheduleRequest.getDeleted()) {
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
        CourseSchedule schedule = courseScheduleMapper.findById(scheduleId);
        if (schedule == null || schedule.getDeleted()) {
            return false;
        }

        Student student = studentMapper.findByUserId(studentUserId);
        CourseEnrollment enrollment = courseEnrollmentMapper.selectById(schedule.getEnrollmentId());
        if (student == null || enrollment == null || !enrollment.getStudentId().equals(student.getId())) {
            return false;
        }

        // 检查课程状态
        if (!"scheduled".equals(schedule.getScheduleStatus())) {
            return false;
        }

        // 放宽：允许次数为0时也可申请（超额将触发扣补规则；若余额不足则不允许）
        try {
            User user = userMapper.selectById(studentUserId);
            if (user == null || user.getDeleted()) {
                return false;
            }
            // 余额校验：当本月调课次数为0时，需有足够余额承担0.5h对应M豆
            Integer times = user.getAdjustmentTimes();
            boolean overQuota = (times == null || times <= 0);
            if (overQuota) {
                // 需要课程信息
                Course course = enrollment.getCourseId() != null ? courseMapper.selectById(enrollment.getCourseId()) : null;
                if (course == null || course.getPrice() == null) {
                    return false;
                }
                Student stu = studentMapper.findByUserId(studentUserId);
                if (stu == null) {
                    return false;
                }
                BigDecimal halfHourBeans = course.getPrice().multiply(new BigDecimal("0.5"));
                BigDecimal bal = stu.getBalance() == null ? BigDecimal.ZERO : stu.getBalance();
                if (bal.compareTo(halfHourBeans) < 0) {
                    return false;
                }
            }
        } catch (Exception ignored) {
            return false;
        }

        // 检查是否在课程开始前（单次调课4小时窗口限制）
        LocalDateTime scheduleDateTime = LocalDateTime.of(schedule.getScheduledDate(), schedule.getStartTime());
        return !scheduleDateTime.isBefore(LocalDateTime.now().plusHours(4)); // 课程开始前4小时内不允许调课
    }

    @Override
    public boolean hasTimeConflict(Long teacherId, Long studentId, String newDate, String newStartTime, String newEndTime, Long excludeScheduleId) {
        try {
            LocalDate date = LocalDate.parse(newDate);
            LocalTime startTime = LocalTime.parse(newStartTime);
            LocalTime endTime = LocalTime.parse(newEndTime);

            // 教师冲突
            List<CourseSchedule> teacherSchedules =
                    courseScheduleMapper.findByTeacherIdAndDateRange(teacherId, date, date);
            for (CourseSchedule s : teacherSchedules) {
                if (excludeScheduleId != null && excludeScheduleId.equals(s.getId())) continue;
                if (!"cancelled".equals(s.getScheduleStatus()) && isTimeOverlap(startTime, endTime, s.getStartTime(), s.getEndTime())) {
                    return true;
                }
            }

            // 学生冲突
            List<CourseSchedule> studentSchedules =
                    courseScheduleMapper.findByStudentIdAndDateRange(studentId, date, date);
            for (CourseSchedule s : studentSchedules) {
                if (excludeScheduleId != null && excludeScheduleId.equals(s.getId())) continue;
                if (!"cancelled".equals(s.getScheduleStatus()) && isTimeOverlap(startTime, endTime, s.getStartTime(), s.getEndTime())) {
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

        return (int) Duration.between(now, scheduleDateTime).toHours();
    }

    /**
     * 构建周期性安排字符串
     */
    private String buildWeeklyScheduleString(List<Integer> weekdays, List<String> timeSlots) {
        if (weekdays == null || timeSlots == null || weekdays.isEmpty() || timeSlots.isEmpty()) {
            return null;
        }

        // 如果只有一个时间段，使用多星期单时间段格式
        if (timeSlots.size() == 1) {
            String[] weekdayNames = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
            StringBuilder sb = new StringBuilder();
            
            for (int i = 0; i < weekdays.size(); i++) {
                if (i > 0) sb.append("、");
                sb.append(weekdayNames[weekdays.get(i)]);
            }
            sb.append(" ").append(timeSlots.get(0));
            
            return sb.toString();
        }

        // 如果多个时间段，使用单星期多时间段格式
        String[] weekdayNames = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < weekdays.size(); i++) {
            if (i > 0) sb.append(";");
            sb.append(weekdayNames[weekdays.get(i)]);
            
            // 为每个星期分配所有时间段
            for (int j = 0; j < timeSlots.size(); j++) {
                if (j > 0) sb.append(",");
                sb.append(" ").append(timeSlots.get(j));
            }
        }

        return sb.toString();
    }

    /**
     * 审批通过后更新课程安排
     */
    private void updateScheduleAfterApproval(RescheduleRequest rescheduleRequest, CourseSchedule schedule) {
        if ("single".equals(rescheduleRequest.getRequestType())) {
            // 单次调课：更新课程安排时间
            schedule.setScheduledDate(rescheduleRequest.getNewDate());
            schedule.setStartTime(rescheduleRequest.getNewStartTime());
            schedule.setEndTime(rescheduleRequest.getNewEndTime());
            UpdateWrapper<CourseSchedule> uw1 = new UpdateWrapper<>();
            uw1.eq("id", schedule.getId()).set("schedule_status", "cancelled");
            courseScheduleMapper.update(null, uw1);

            // 重新计算同一预约申请下所有课程的session_number
            if (schedule.getBookingRequestId() != null) {
                recalculateSessionNumbers(schedule.getBookingRequestId());
            }
        } else if ("recurring".equals(rescheduleRequest.getRequestType())) {
            // 周期性调课：需要更新后续所有课程安排（迁移至新表）
            log.info("周期性调课审批通过，开始更新后续课程安排");
            
            try {
                // 获取同一预约申请下的所有未来课程安排（新表）
                List<CourseSchedule> all =
                        courseScheduleMapper.findByBookingRequestId(schedule.getBookingRequestId());
                List<CourseSchedule> futureSchedules = new ArrayList<>();
                LocalDate nowDate = LocalDate.now();
                for (CourseSchedule cs : all) {
                    if (cs.getScheduledDate() != null && cs.getScheduledDate().isAfter(nowDate) &&
                        (cs.getScheduleStatus() == null || !"cancelled".equals(cs.getScheduleStatus()))) {
                        futureSchedules.add(cs);
                    }
                }
                
                if (futureSchedules.isEmpty()) {
                    log.warn("没有找到需要更新的未来课程安排");
                    return;
                }
                
                log.info("找到 {} 个未来课程安排需要更新", futureSchedules.size());
                
                // 更新所有未来课程的周期性安排字段：写入reschedule_request_id/reschedule_reason
                for (CourseSchedule cs : futureSchedules) {
                    UpdateWrapper<CourseSchedule> uw = new UpdateWrapper<>();
                    uw.eq("id", cs.getId())
                      .set("reschedule_request_id", rescheduleRequest.getId())
                      .set("reschedule_reason", rescheduleRequest.getReason());
                    courseScheduleMapper.update(null, uw);
                }
                
                // TODO: 同步缓存（按新表日期聚合回填）
                
                log.info("周期性调课更新完成，共更新 {} 个课程安排", futureSchedules.size());
                
            } catch (Exception e) {
                log.error("周期性调课更新失败", e);
                throw new RuntimeException("周期性调课更新失败: " + e.getMessage());
            }
        } else if ("cancel".equals(rescheduleRequest.getRequestType())) {
            // 取消课程：更新课程状态
            schedule.setScheduleStatus("cancelled");
            UpdateWrapper<CourseSchedule> uw2 = new UpdateWrapper<>();
            uw2.eq("id", schedule.getId()).set("schedule_status", "rescheduled");
            courseScheduleMapper.update(null, uw2);
        }
    }

    /**
     * 重新计算指定预约申请下所有课程的session_number
     * 根据实际上课时间（日期+时间）重新排序
     */
    private void recalculateSessionNumbers(Long bookingRequestId) {
        log.info("重新计算预约申请 {} 下所有课程的session_number", bookingRequestId);

        // 获取该预约申请下的所有课程安排（新表），按时间排序
        List<CourseSchedule> schedules = courseScheduleMapper.findByBookingRequestId(bookingRequestId);

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
            CourseSchedule schedule = schedules.get(i);
            int newSessionNumber = i + 1;

            // 只有当session_number发生变化时才更新
            if (schedule.getSessionNumber() == null || !schedule.getSessionNumber().equals(newSessionNumber)) {
                UpdateWrapper<CourseSchedule> uw3 = new UpdateWrapper<>();
                uw3.eq("id", schedule.getId()).set("session_number", newSessionNumber);
                courseScheduleMapper.update(null, uw3);
                log.debug("更新课程安排 {} 的session_number: {} -> {}",
                         schedule.getId(), schedule.getSessionNumber(), newSessionNumber);
            }
        }

        log.info("预约申请 {} 下共 {} 个课程安排的session_number重新计算完成", bookingRequestId, schedules.size());
    }

    /**
     * 获取同一预约申请下所有未来课程安排（新表）
     */
    // 保留备用：获取同一预约申请下所有未来课程安排
    @SuppressWarnings("unused")
    private List<CourseSchedule> getFutureSchedulesForRecurringUpdate(CourseSchedule currentSchedule) {
        List<CourseSchedule> futureSchedules = new ArrayList<>();
        LocalDate currentDate = LocalDate.now();

        Long bookingRequestId = currentSchedule.getBookingRequestId();
        if (bookingRequestId == null) {
            log.warn("课程安排 {} 没有预约申请ID，无法查找相关课程", currentSchedule.getId());
            return futureSchedules;
        }

        List<CourseSchedule> allSchedules = courseScheduleMapper.findByBookingRequestId(bookingRequestId);
        log.info("预约申请 {} 下共有 {} 个课程安排", bookingRequestId, allSchedules.size());

        for (CourseSchedule s : allSchedules) {
            if (s.getScheduledDate() != null && s.getScheduledDate().isAfter(currentDate) &&
                (s.getScheduleStatus() == null || !"cancelled".equals(s.getScheduleStatus()))) {
                futureSchedules.add(s);
            }
        }

        futureSchedules.sort((s1, s2) -> {
            int dateCompare = s1.getScheduledDate().compareTo(s2.getScheduledDate());
            if (dateCompare != 0) return dateCompare;
            return s1.getStartTime().compareTo(s2.getStartTime());
        });

        log.info("找到 {} 个当前时间以后的进行中课程安排需要更新", futureSchedules.size());
        return futureSchedules;
    }
    
    /**
     * 解析中文星期名称
     */
    private Integer parseChineseWeekday(String weekdayName) {
        if (weekdayName == null || weekdayName.isEmpty()) {
            return null;
        }

        return switch (weekdayName.trim()) {
            case "周一" -> 1;
            case "周二" -> 2;
            case "周三" -> 3;
            case "周四" -> 4;
            case "周五" -> 5;
            case "周六" -> 6;
            case "周日" -> 7;
            default -> null;
        };
    }

    /**
     * 解析周期性安排字符串中的时间段
     */
    private List<String> parseTimeSlotsFromString(String weeklySchedule) {
        List<String> timeSlots = new ArrayList<>();
        if (weeklySchedule == null || weeklySchedule.isEmpty()) {
            return timeSlots;
        }

        log.debug("开始解析时间段，输入: {}", weeklySchedule);
        
        // 支持三种格式：
        // 1. 多星期单时间段：周一、周二、周四、周六 13:00-15:00
        // 2. 单星期多时间段：周六 13:00-15:00, 15:00-17:00, 17:00-19:00
        // 3. 混合格式：周二 10:00-12:00;周四 15:00-17:00;周五 19:00-21:00
        
        // 首先检查是否是多星期单时间段格式（包含顿号）
        if (weeklySchedule.contains("、")) {
            // 多星期单时间段格式：周一、周二、周四、周六 13:00-15:00
            String[] parts = weeklySchedule.split(" ");
            if (parts.length >= 2) {
                // 从第二个部分开始都是时间段
                StringBuilder timeSlot = new StringBuilder();
                for (int i = 1; i < parts.length; i++) {
                    if (i > 1) timeSlot.append(" ");
                    timeSlot.append(parts[i]);
                }
                String timeSlotStr = timeSlot.toString().trim();
                if (!timeSlotStr.isEmpty()) {
                    timeSlots.add(timeSlotStr);
                }
            }
        } else if (weeklySchedule.contains(";")) {
            // 混合格式：周二 10:00-12:00;周四 15:00-17:00;周五 19:00-21:00
            String[] parts = weeklySchedule.split(";");
            for (String part : parts) {
                part = part.trim();
                if (part.isEmpty()) continue;

                // 按空格分割，第一部分是星期，第二部分是时间
                String[] weekdayAndTime = part.split(" ");
                if (weekdayAndTime.length > 1) {
                    // 从第二个部分开始都是时间段
                    StringBuilder timeSlot = new StringBuilder();
                    for (int i = 1; i < weekdayAndTime.length; i++) {
                        if (i > 1) timeSlot.append(" ");
                        timeSlot.append(weekdayAndTime[i]);
                    }
                    String timeSlotStr = timeSlot.toString().trim();
                    if (!timeSlotStr.isEmpty()) {
                        timeSlots.add(timeSlotStr);
                    }
                }
            }
        } else {
            log.info("检测到单星期多时间段格式");
            // 单星期多时间段格式：周六 13:00-15:00, 15:00-17:00, 17:00-19:00
            // 按空格分割，第一部分是星期，第二部分开始都是时间段
            String[] parts = weeklySchedule.split(" ");
            log.info("按空格分割后的部分: {}", Arrays.toString(parts));
            if (parts.length > 1) {
                // 从第二个部分开始拼接所有时间段部分，然后按逗号分割
                StringBuilder allTimeSlots = new StringBuilder();
                for (int i = 1; i < parts.length; i++) {
                    if (i > 1) allTimeSlots.append(" ");
                    allTimeSlots.append(parts[i]);
                }
                String allTimeSlotsStr = allTimeSlots.toString().trim();
                log.info("拼接后的时间段字符串: '{}'", allTimeSlotsStr);
                
                if (allTimeSlotsStr.contains(",")) {
                    log.info("检测到逗号分隔，按逗号分割");
                    // 按逗号分割多个时间段
                    String[] timeSlotArray = allTimeSlotsStr.split(",");
                    log.info("按逗号分割后的时间段: {}", Arrays.toString(timeSlotArray));
                    for (String timeSlot : timeSlotArray) {
                        timeSlot = timeSlot.trim();
                        log.info("处理时间段: '{}'", timeSlot);
                        if (!timeSlot.isEmpty()) {
                            timeSlots.add(timeSlot);
                            log.info("添加时间段: '{}'", timeSlot);
                        }
                    }
                } else {
                    log.info("单个时间段，直接添加");
                    // 单个时间段
                    timeSlots.add(allTimeSlotsStr);
                }
            }
        }
        
        log.debug("解析时间段结果: {}", timeSlots);
        return timeSlots;
    }

    // 删除未使用的同步缓存私有方法

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

            CourseSchedule schedule = courseScheduleMapper.findById(rescheduleRequest.getScheduleId());
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
            CourseSchedule schedule = courseScheduleMapper.findById(rescheduleRequest.getScheduleId());
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
                } else if ("admin".equals(rescheduleRequest.getReviewerType())) {
                    User adminUser = userMapper.selectById(rescheduleRequest.getReviewerId());
                    if (adminUser != null) {
                        builder.reviewerName(adminUser.getUsername());
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
        return switch (status) {
            case "pending" -> "待审批";
            case "approved" -> "已同意";
            case "rejected" -> "已拒绝";
            case "cancelled" -> "已取消";
            default -> status;
        };
    }
}

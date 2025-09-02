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
import java.math.BigDecimal;
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
    private UserMapper userMapper;

    @Autowired
    private TeacherCacheWarmupService teacherCacheWarmupService;

    @Autowired
    private CacheKeyEvictor cacheKeyEvictor;

    @Autowired
    private TeacherScheduleCacheService teacherScheduleCacheService;

    @Autowired
    private BalanceTransactionMapper balanceTransactionMapper;

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

        // 获取课程安排信息
        Schedule schedule = scheduleMapper.selectById(request.getScheduleId());
        if (schedule == null || schedule.getDeleted()) {
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
            Course course = courseMapper.selectById(schedule.getCourseId());
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
        Schedule schedule = scheduleMapper.selectById(request.getScheduleId());
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
        Schedule schedule = scheduleMapper.selectById(rescheduleRequest.getScheduleId());
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
        Schedule schedule = scheduleMapper.selectById(rescheduleRequest.getScheduleId());
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
        java.util.List<RescheduleRequest> records = resultPage.getRecords();
        java.util.List<Long> scheduleIds = records.stream().map(RescheduleRequest::getScheduleId).filter(java.util.Objects::nonNull).distinct().collect(java.util.stream.Collectors.toList());
        java.util.Map<Long, Schedule> scheduleMap = new java.util.HashMap<>();
        if (!scheduleIds.isEmpty()) {
            QueryWrapper<Schedule> sq = new QueryWrapper<>();
            sq.in("id", scheduleIds);
            for (Schedule s : scheduleMapper.selectList(sq)) scheduleMap.put(s.getId(), s);
        }
        java.util.Map<Long, Course> courseMap = new java.util.HashMap<>();
        java.util.Map<Long, Subject> subjectMap = new java.util.HashMap<>();
        java.util.Map<Long, Teacher> teacherMap = new java.util.HashMap<>();
        java.util.Map<Long, Student> studentMap = new java.util.HashMap<>();
        if (!scheduleMap.isEmpty()) {
            java.util.List<Long> courseIds = scheduleMap.values().stream().map(Schedule::getCourseId).filter(java.util.Objects::nonNull).distinct().collect(java.util.stream.Collectors.toList());
            if (!courseIds.isEmpty()) {
                QueryWrapper<Course> cq = new QueryWrapper<>();
                cq.in("id", courseIds);
                for (Course c : courseMapper.selectList(cq)) {
                    courseMap.put(c.getId(), c);
                }
                java.util.List<Long> subjectIds = courseMap.values().stream().map(Course::getSubjectId).filter(java.util.Objects::nonNull).distinct().collect(java.util.stream.Collectors.toList());
                if (!subjectIds.isEmpty()) {
                    QueryWrapper<Subject> subjQ = new QueryWrapper<>();
                    subjQ.in("id", subjectIds);
                    for (Subject s : subjectMapper.selectList(subjQ)) subjectMap.put(s.getId(), s);
                }
            }
            java.util.List<Long> teacherIds = scheduleMap.values().stream().map(Schedule::getTeacherId).filter(java.util.Objects::nonNull).distinct().collect(java.util.stream.Collectors.toList());
            if (!teacherIds.isEmpty()) {
                QueryWrapper<Teacher> tq = new QueryWrapper<>();
                tq.in("id", teacherIds);
                for (Teacher t : teacherMapper.selectList(tq)) teacherMap.put(t.getId(), t);
            }
            java.util.List<Long> studentIds = scheduleMap.values().stream().map(Schedule::getStudentId).filter(java.util.Objects::nonNull).distinct().collect(java.util.stream.Collectors.toList());
            if (!studentIds.isEmpty()) {
                QueryWrapper<Student> stq = new QueryWrapper<>();
                stq.in("id", studentIds);
                for (Student s : studentMapper.selectList(stq)) studentMap.put(s.getId(), s);
            }
        }

        java.util.List<RescheduleVO> vos = new java.util.ArrayList<>();
        for (RescheduleRequest rr : records) {
            RescheduleVO vo = convertToRescheduleResponseDTO(rr);
            Schedule s = scheduleMap.get(rr.getScheduleId());
            if (s != null) {
                Course c = courseMap.get(s.getCourseId());
                if (c != null) {
                    vo.setCourseTitle(c.getTitle());
                    Subject subj = subjectMap.get(c.getSubjectId());
                    if (subj != null) vo.setSubjectName(subj.getName());
                }
                Teacher t = teacherMap.get(s.getTeacherId());
                if (t != null) vo.setTeacherName(t.getRealName());
                Student stu = studentMap.get(s.getStudentId());
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
        java.util.List<RescheduleRequest> records = resultPage.getRecords();
        java.util.List<Long> scheduleIds = records.stream().map(RescheduleRequest::getScheduleId).filter(java.util.Objects::nonNull).distinct().collect(java.util.stream.Collectors.toList());
        java.util.Map<Long, Schedule> scheduleMap = new java.util.HashMap<>();
        if (!scheduleIds.isEmpty()) {
            QueryWrapper<Schedule> sq = new QueryWrapper<>();
            sq.in("id", scheduleIds);
            for (Schedule s : scheduleMapper.selectList(sq)) scheduleMap.put(s.getId(), s);
        }
        java.util.Map<Long, Course> courseMap = new java.util.HashMap<>();
        java.util.Map<Long, Subject> subjectMap = new java.util.HashMap<>();
        java.util.Map<Long, Teacher> teacherMap = new java.util.HashMap<>();
        java.util.Map<Long, Student> studentMap = new java.util.HashMap<>();
        if (!scheduleMap.isEmpty()) {
            java.util.List<Long> courseIds = scheduleMap.values().stream().map(Schedule::getCourseId).filter(java.util.Objects::nonNull).distinct().collect(java.util.stream.Collectors.toList());
            if (!courseIds.isEmpty()) {
                QueryWrapper<Course> cq = new QueryWrapper<>();
                cq.in("id", courseIds);
                for (Course c : courseMapper.selectList(cq)) {
                    courseMap.put(c.getId(), c);
                }
                java.util.List<Long> subjectIds = courseMap.values().stream().map(Course::getSubjectId).filter(java.util.Objects::nonNull).distinct().collect(java.util.stream.Collectors.toList());
                if (!subjectIds.isEmpty()) {
                    QueryWrapper<Subject> subjQ = new QueryWrapper<>();
                    subjQ.in("id", subjectIds);
                    for (Subject s : subjectMapper.selectList(subjQ)) subjectMap.put(s.getId(), s);
                }
            }
            java.util.List<Long> teacherIds = scheduleMap.values().stream().map(Schedule::getTeacherId).filter(java.util.Objects::nonNull).distinct().collect(java.util.stream.Collectors.toList());
            if (!teacherIds.isEmpty()) {
                QueryWrapper<Teacher> tq = new QueryWrapper<>();
                tq.in("id", teacherIds);
                for (Teacher t : teacherMapper.selectList(tq)) teacherMap.put(t.getId(), t);
            }
            java.util.List<Long> studentIds = scheduleMap.values().stream().map(Schedule::getStudentId).filter(java.util.Objects::nonNull).distinct().collect(java.util.stream.Collectors.toList());
            if (!studentIds.isEmpty()) {
                QueryWrapper<Student> stq = new QueryWrapper<>();
                stq.in("id", studentIds);
                for (Student s : studentMapper.selectList(stq)) studentMap.put(s.getId(), s);
            }
        }

        java.util.List<RescheduleVO> vos = new java.util.ArrayList<>();
        for (RescheduleRequest rr : records) {
            RescheduleVO vo = convertToRescheduleResponseDTO(rr);
            Schedule s = scheduleMap.get(rr.getScheduleId());
            if (s != null) {
                Course c = courseMap.get(s.getCourseId());
                if (c != null) {
                    vo.setCourseTitle(c.getTitle());
                    Subject subj = subjectMap.get(c.getSubjectId());
                    if (subj != null) vo.setSubjectName(subj.getName());
                }
                Teacher t = teacherMap.get(s.getTeacherId());
                if (t != null) vo.setTeacherName(t.getRealName());
                Student stu = studentMap.get(s.getStudentId());
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
        java.util.List<RescheduleRequest> records = resultPage.getRecords();
        java.util.List<Long> scheduleIds = records.stream().map(RescheduleRequest::getScheduleId).filter(java.util.Objects::nonNull).distinct().collect(java.util.stream.Collectors.toList());
        java.util.Map<Long, Schedule> scheduleMap = new java.util.HashMap<>();
        if (!scheduleIds.isEmpty()) {
            QueryWrapper<Schedule> sq = new QueryWrapper<>();
            sq.in("id", scheduleIds);
            for (Schedule s : scheduleMapper.selectList(sq)) scheduleMap.put(s.getId(), s);
        }
        java.util.Map<Long, Course> courseMap = new java.util.HashMap<>();
        java.util.Map<Long, Subject> subjectMap = new java.util.HashMap<>();
        java.util.Map<Long, Teacher> teacherMap = new java.util.HashMap<>();
        java.util.Map<Long, Student> studentMap = new java.util.HashMap<>();
        if (!scheduleMap.isEmpty()) {
            java.util.List<Long> courseIds = scheduleMap.values().stream().map(Schedule::getCourseId).filter(java.util.Objects::nonNull).distinct().collect(java.util.stream.Collectors.toList());
            if (!courseIds.isEmpty()) {
                QueryWrapper<Course> cq = new QueryWrapper<>();
                cq.in("id", courseIds);
                for (Course c : courseMapper.selectList(cq)) {
                    courseMap.put(c.getId(), c);
                }
                java.util.List<Long> subjectIds = courseMap.values().stream().map(Course::getSubjectId).filter(java.util.Objects::nonNull).distinct().collect(java.util.stream.Collectors.toList());
                if (!subjectIds.isEmpty()) {
                    QueryWrapper<Subject> subjQ = new QueryWrapper<>();
                    subjQ.in("id", subjectIds);
                    for (Subject s : subjectMapper.selectList(subjQ)) subjectMap.put(s.getId(), s);
                }
            }
            java.util.List<Long> teacherIds = scheduleMap.values().stream().map(Schedule::getTeacherId).filter(java.util.Objects::nonNull).distinct().collect(java.util.stream.Collectors.toList());
            if (!teacherIds.isEmpty()) {
                QueryWrapper<Teacher> tq = new QueryWrapper<>();
                tq.in("id", teacherIds);
                for (Teacher t : teacherMapper.selectList(tq)) teacherMap.put(t.getId(), t);
            }
            java.util.List<Long> studentIds = scheduleMap.values().stream().map(Schedule::getStudentId).filter(java.util.Objects::nonNull).distinct().collect(java.util.stream.Collectors.toList());
            if (!studentIds.isEmpty()) {
                QueryWrapper<Student> stq = new QueryWrapper<>();
                stq.in("id", studentIds);
                for (Student s : studentMapper.selectList(stq)) studentMap.put(s.getId(), s);
            }
        }

        java.util.List<RescheduleVO> vos = new java.util.ArrayList<>();
        for (RescheduleRequest rr : records) {
            RescheduleVO vo = convertToRescheduleResponseDTO(rr);
            Schedule s = scheduleMap.get(rr.getScheduleId());
            if (s != null) {
                Course c = courseMap.get(s.getCourseId());
                if (c != null) {
                    vo.setCourseTitle(c.getTitle());
                    Subject subj = subjectMap.get(c.getSubjectId());
                    if (subj != null) vo.setSubjectName(subj.getName());
                }
                Teacher t = teacherMap.get(s.getTeacherId());
                if (t != null) vo.setTeacherName(t.getRealName());
                Student stu = studentMap.get(s.getStudentId());
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
        Schedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null || schedule.getDeleted()) {
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
                // 需要 schedule 和 course 信息
                if (schedule == null) {
                    return false;
                }
                Course course = courseMapper.selectById(schedule.getCourseId());
                if (course == null || course.getPrice() == null) {
                    return false;
                }
                Student stu = studentMapper.findByUserId(studentUserId);
                if (stu == null) {
                    return false;
                }
                java.math.BigDecimal halfHourBeans = course.getPrice().multiply(new java.math.BigDecimal("0.5"));
                java.math.BigDecimal bal = stu.getBalance() == null ? java.math.BigDecimal.ZERO : stu.getBalance();
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
            log.info("周期性调课审批通过，开始更新后续课程安排");
            
            try {
                // 获取同一预约申请下的所有未来课程安排
                List<Schedule> futureSchedules = getFutureSchedulesForRecurringUpdate(schedule);
                
                if (futureSchedules.isEmpty()) {
                    log.warn("没有找到需要更新的未来课程安排");
                    return;
                }
                
                log.info("找到 {} 个未来课程安排需要更新", futureSchedules.size());
                
                // 更新所有未来课程的周期性安排字段
                updateRecurringScheduleFields(futureSchedules, rescheduleRequest.getNewWeeklySchedule());
                
                // 同步缓存
                syncCacheAfterRecurringUpdate(schedule, futureSchedules);
                
                log.info("周期性调课更新完成，共更新 {} 个课程安排", futureSchedules.size());
                
            } catch (Exception e) {
                log.error("周期性调课更新失败", e);
                throw new RuntimeException("周期性调课更新失败: " + e.getMessage());
            }
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
     * 获取同一预约申请下所有未来课程安排
     */
    private List<Schedule> getFutureSchedulesForRecurringUpdate(Schedule currentSchedule) {
        List<Schedule> futureSchedules = new java.util.ArrayList<>();
        LocalDate currentDate = LocalDate.now();

        // 获取当前课程安排的预约申请ID
        Long bookingRequestId = currentSchedule.getBookingRequestId();
        if (bookingRequestId == null) {
            log.warn("课程安排 {} 没有预约申请ID，无法查找相关课程", currentSchedule.getId());
            return futureSchedules;
        }

        // 获取该预约申请下的所有课程安排
        List<Schedule> allSchedules = scheduleMapper.findByBookingRequestId(bookingRequestId);
        log.info("预约申请 {} 下共有 {} 个课程安排", bookingRequestId, allSchedules.size());

        if (allSchedules.isEmpty()) {
            log.warn("预约申请 {} 下没有找到任何课程安排", bookingRequestId);
            return futureSchedules;
        }

        // 过滤出当前时间以后的进行中课程安排（排除已取消和已完成的）
        for (Schedule schedule : allSchedules) {
            if (schedule != null && !schedule.getDeleted() && 
                "progressing".equals(schedule.getStatus()) &&
                schedule.getScheduledDate().isAfter(currentDate)) {
                futureSchedules.add(schedule);
            }
        }

        // 按实际上课时间排序（日期+时间）
        futureSchedules.sort((s1, s2) -> {
            int dateCompare = s1.getScheduledDate().compareTo(s2.getScheduledDate());
            if (dateCompare != 0) {
                return dateCompare;
            }
            return s1.getStartTime().compareTo(s2.getStartTime());
        });

        log.info("找到 {} 个当前时间以后的进行中课程安排需要更新", futureSchedules.size());
        return futureSchedules;
    }

    /**
     * 更新周期性课程安排的字段
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateRecurringScheduleFields(List<Schedule> futureSchedules, String newWeeklySchedule) {
        if (newWeeklySchedule == null || newWeeklySchedule.isEmpty()) {
            log.warn("新的周期性安排为空，无法更新");
            return;
        }

        log.info("开始批量更新 {} 个周期性课程安排，新安排: {}", futureSchedules.size(), newWeeklySchedule);
        
        // 保存原始数据用于回滚
        List<Schedule> originalSchedules = new java.util.ArrayList<>();
        for (Schedule schedule : futureSchedules) {
            Schedule original = new Schedule();
            original.setId(schedule.getId());
            original.setScheduledDate(schedule.getScheduledDate());
            original.setStartTime(schedule.getStartTime());
            original.setEndTime(schedule.getEndTime());
            original.setRecurringWeekdays(schedule.getRecurringWeekdays());
            original.setRecurringTimeSlots(schedule.getRecurringTimeSlots());
            originalSchedules.add(original);
        }
        
        try {
            // 解析新的周期性安排
            List<Integer> newWeekdays = parseWeekdaysFromString(newWeeklySchedule);
            List<String> newTimeSlots = parseTimeSlotsFromString(newWeeklySchedule);
            
            log.info("解析结果 - 星期: {}, 时间段: {}", newWeekdays, newTimeSlots);
            
            if (newWeekdays.isEmpty() || newTimeSlots.isEmpty()) {
                log.error("新的周期性安排格式无效: {}", newWeeklySchedule);
                throw new RuntimeException("新的周期性安排格式无效，无法解析星期或时间段");
            }

            int updatedCount = 0;
            
            // 对于周期性调课，我们需要为每个时间段创建对应的课程安排
            // 计算需要多少个周六来容纳所有课程
            int totalTimeSlots = newTimeSlots.size();
            int totalSchedules = futureSchedules.size();
            int weeksNeeded = (int) Math.ceil((double) totalSchedules / totalTimeSlots);
            
            log.info("周期性调课分析：总课程数={}, 时间段数={}, 需要周数={}", 
                     totalSchedules, totalTimeSlots, weeksNeeded);
            
            // 使用第一个课程的日期作为基准日期
            LocalDate baseDate = futureSchedules.get(0).getScheduledDate();
            
            // 判断是多星期单时间段还是单星期多时间段
            boolean isMultipleWeekdaysSingleTimeSlot = newWeekdays.size() > 1 && totalTimeSlots == 1;
            
            if (isMultipleWeekdaysSingleTimeSlot) {
                log.info("检测到多星期单时间段格式，将循环使用星期几列表");
            } else {
                log.info("检测到单星期多时间段格式，将按周数递增");
            }
            
            for (int i = 0; i < futureSchedules.size(); i++) {
                Schedule schedule = futureSchedules.get(i);
                try {
                    log.info("=== 开始处理课程安排 {} ===", schedule.getId());
                    
                    // 获取当前课程安排的星期几
                    int currentWeekday = schedule.getScheduledDate().getDayOfWeek().getValue();
                    log.info("课程安排 {} 的当前星期几: {} (将被改为新的星期几)", schedule.getId(), currentWeekday);
                    log.info("新的星期几列表: {}", newWeekdays);
                    
                    // 计算这个课程应该在第几周，以及对应的时间段
                    int weekIndex = i / totalTimeSlots;  // 第几周
                    int timeSlotIndex = i % totalTimeSlots;  // 该周的第几个时间段
                    String newTimeSlot = newTimeSlots.get(timeSlotIndex);
                    
                    // 对于多星期单时间段，循环使用星期几列表
                    int targetWeekday;
                    if (isMultipleWeekdaysSingleTimeSlot) {
                        targetWeekday = newWeekdays.get(i % newWeekdays.size());
                        log.info("课程安排 {} 分配：第{}周，星期几: {} (循环使用), 时间段: {}", 
                                 schedule.getId(), weekIndex + 1, targetWeekday, newTimeSlot);
                    } else {
                        targetWeekday = newWeekdays.get(0); // 使用第一个新的星期几
                        log.info("课程安排 {} 分配：第{}周，时间段{}: {}", 
                                 schedule.getId(), weekIndex + 1, timeSlotIndex + 1, newTimeSlot);
                    }
                    
                    // 解析新的时间段
                    LocalTime newStartTime = null;
                    LocalTime newEndTime = null;
                    
                    if (newTimeSlot.contains("-")) {
                        // 格式: "14:00-16:00"
                        String[] timeParts = newTimeSlot.split("-");
                        if (timeParts.length == 2) {
                            newStartTime = LocalTime.parse(timeParts[0].trim());
                            newEndTime = LocalTime.parse(timeParts[1].trim());
                        }
                    } else {
                        // 格式: "14:00" - 只有开始时间，假设课程时长为2小时
                        newStartTime = LocalTime.parse(newTimeSlot.trim());
                        newEndTime = newStartTime.plusHours(2);
                    }
                    
                    if (newStartTime == null || newEndTime == null) {
                        log.error("无法解析时间段: {}", newTimeSlot);
                        throw new RuntimeException("无法解析时间段: " + newTimeSlot);
                    }
                    
                    log.info("解析时间 - 开始: {}, 结束: {}", newStartTime, newEndTime);
                    
                    // 计算新的日期
                    LocalDate newDate;
                    if (isMultipleWeekdaysSingleTimeSlot) {
                        // 多星期单时间段：每个课程对应一个不同的星期几
                        newDate = calculateNextDateForWeekday(baseDate, targetWeekday);
                        // 如果这个星期几已经过了，就加一周
                        if (newDate.isBefore(baseDate)) {
                            newDate = newDate.plusWeeks(1);
                        }
                        log.info("多星期单时间段：课程 {} 分配到星期几 {}，日期: {}", 
                                 schedule.getId(), targetWeekday, newDate);
                    } else {
                        // 单星期多时间段：基于第一周日期计算每周的日期
                        LocalDate firstWeekDate = calculateNextDateForWeekday(baseDate, targetWeekday);
                        newDate = firstWeekDate.plusWeeks(weekIndex);
                        log.info("单星期多时间段：第{}周，日期: {}", weekIndex + 1, newDate);
                    }
                    
                    log.info("计算的新日期: {} (目标星期几: {}, 第{}周)", newDate, targetWeekday, weekIndex + 1);
                    
                    // 检查时间冲突
                    log.info("开始检查时间冲突...");
                    if (hasTimeConflict(schedule.getTeacherId(), schedule.getStudentId(), 
                                       newDate.toString(), newStartTime.toString(), newEndTime.toString(), 
                                       schedule.getId())) {
                        log.warn("课程安排 {} 的新时间存在冲突，跳过更新", schedule.getId());
                        continue;
                    }
                    log.info("时间冲突检查通过");
                    
                    // 更新Schedule实体的字段
                    log.info("开始更新课程安排 {} 的字段", schedule.getId());
                    schedule.setScheduledDate(newDate);           // 上课日期
                    schedule.setStartTime(newStartTime);          // 开始时间
                    schedule.setEndTime(newEndTime);              // 结束时间
                    schedule.setRecurringWeekdays(newWeekdays.stream().map(String::valueOf).collect(Collectors.joining(",")));  // 周期性预约的星期几
                    schedule.setRecurringTimeSlots(String.join(",", newTimeSlots));  // 周期性预约的时间段
                    
                    log.info("课程安排 {} 字段更新完成，准备保存到数据库", schedule.getId());
                    
                    // 保存到数据库
                    scheduleMapper.updateById(schedule);
                    updatedCount++;
                    
                    log.info("课程安排 {} 保存成功，已更新 {} 个课程", schedule.getId(), updatedCount);
                    
                    log.debug("更新课程安排 {} 的周期性安排: 日期={}, 时间={}-{}, 星期={}, 时间段={}",
                             schedule.getId(), newDate, newStartTime, newEndTime, 
                             schedule.getRecurringWeekdays(), schedule.getRecurringTimeSlots());
                } catch (Exception e) {
                    log.error("更新课程安排 {} 失败，开始回滚", schedule.getId(), e);
                    // 回滚已更新的课程安排
                    rollbackScheduleUpdates(futureSchedules, originalSchedules);
                    throw new RuntimeException("更新课程安排失败: " + e.getMessage());
                }
            }
            
            log.info("周期性调课批量更新完成，成功更新 {} 个课程安排", updatedCount);
            
        } catch (Exception e) {
            log.error("批量更新周期性课程安排失败，开始回滚", e);
            // 回滚所有更改
            rollbackScheduleUpdates(futureSchedules, originalSchedules);
            throw new RuntimeException("批量更新周期性课程安排失败: " + e.getMessage());
        }
    }

    /**
     * 回滚课程安排更新
     */
    private void rollbackScheduleUpdates(List<Schedule> currentSchedules, List<Schedule> originalSchedules) {
        log.info("开始回滚课程安排更新");
        try {
            for (int i = 0; i < currentSchedules.size() && i < originalSchedules.size(); i++) {
                Schedule current = currentSchedules.get(i);
                Schedule original = originalSchedules.get(i);
                
                if (current.getId().equals(original.getId())) {
                    current.setScheduledDate(original.getScheduledDate());
                    current.setStartTime(original.getStartTime());
                    current.setEndTime(original.getEndTime());
                    current.setRecurringWeekdays(original.getRecurringWeekdays());
                    current.setRecurringTimeSlots(original.getRecurringTimeSlots());
                    
                    scheduleMapper.updateById(current);
                    log.debug("回滚课程安排 {} 到原始状态", current.getId());
                }
            }
            log.info("课程安排回滚完成");
        } catch (Exception e) {
            log.error("回滚课程安排失败", e);
            // 回滚失败，记录错误但不抛出异常，避免无限循环
        }
    }

    /**
     * 计算下一个指定星期几的日期
     */
    private LocalDate calculateNextDateForWeekday(LocalDate currentDate, int targetWeekday) {
        LocalDate nextDate = currentDate;
        int currentDayOfWeek = currentDate.getDayOfWeek().getValue();

        if (currentDayOfWeek == targetWeekday) {
            return currentDate; // 如果当前日期就是目标星期几，则返回当前日期
        }

        int daysUntilTarget = targetWeekday - currentDayOfWeek;
        if (daysUntilTarget <= 0) {
            daysUntilTarget += 7; // 如果目标在当前日期之前，则加7天
        }
        return currentDate.plusDays(daysUntilTarget);
    }

    /**
     * 解析周期性安排字符串中的星期几
     */
    private List<Integer> parseWeekdaysFromString(String weeklySchedule) {
        List<Integer> weekdays = new java.util.ArrayList<>();
        if (weeklySchedule == null || weeklySchedule.isEmpty()) {
            return weekdays;
        }

        log.debug("开始解析星期，输入: {}", weeklySchedule);
        
        // 支持三种格式：
        // 1. 多星期单时间段：周一、周二、周四、周六 13:00-15:00
        // 2. 单星期多时间段：周六 13:00-15:00, 15:00-17:00, 17:00-19:00
        // 3. 混合格式：周二 10:00-12:00;周四 15:00-17:00;周五 19:00-21:00
        
        // 首先检查是否是多星期单时间段格式（包含顿号）
        if (weeklySchedule.contains("、")) {
            // 多星期单时间段格式：周一、周二、周四、周六 13:00-15:00
            String[] parts = weeklySchedule.split(" ");
            if (parts.length >= 2) {
                String weekdaysPart = parts[0]; // 周一、周二、周四、周六
                String[] weekdayArray = weekdaysPart.split("、");
                
                for (String weekdayStr : weekdayArray) {
                    weekdayStr = weekdayStr.trim();
                    if (weekdayStr.isEmpty()) continue;
                    
                    // 尝试解析中文星期名称
                    Integer weekday = parseChineseWeekday(weekdayStr);
                    if (weekday != null) {
                        weekdays.add(weekday);
                        continue;
                    }
                    
                    // 尝试解析数字星期
                    try {
                        int weekdayNum = Integer.parseInt(weekdayStr);
                        if (weekdayNum >= 1 && weekdayNum <= 7) {
                            weekdays.add(weekdayNum);
                        } else {
                            log.warn("无效的星期数: {}", weekdayStr);
                        }
                    } catch (NumberFormatException e) {
                        log.warn("无法解析星期数: {}", weekdayStr);
                    }
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
                if (weekdayAndTime.length > 0) {
                    String weekdayStr = weekdayAndTime[0].trim();
                    
                    // 尝试解析中文星期名称
                    Integer weekday = parseChineseWeekday(weekdayStr);
                    if (weekday != null) {
                        weekdays.add(weekday);
                        continue;
                    }
                    
                    // 尝试解析数字星期
                    try {
                        int weekdayNum = Integer.parseInt(weekdayStr);
                        if (weekdayNum >= 1 && weekdayNum <= 7) {
                            weekdays.add(weekdayNum);
                        } else {
                            log.warn("无效的星期数: {}", weekdayStr);
                        }
                    } catch (NumberFormatException e) {
                        log.warn("无法解析星期数: {}", weekdayStr);
                    }
                }
            }
        } else {
            // 单星期多时间段格式：周六 13:00-15:00, 15:00-17:00, 17:00-19:00
            // 按空格分割，第一部分是星期
            String[] parts = weeklySchedule.split(" ");
            if (parts.length > 0) {
                String weekdayStr = parts[0].trim();
                
                // 尝试解析中文星期名称
                Integer weekday = parseChineseWeekday(weekdayStr);
                if (weekday != null) {
                    weekdays.add(weekday);
                } else {
                    // 尝试解析数字星期
                    try {
                        int weekdayNum = Integer.parseInt(weekdayStr);
                        if (weekdayNum >= 1 && weekdayNum <= 7) {
                            weekdays.add(weekdayNum);
                        } else {
                            log.warn("无效的星期数: {}", weekdayStr);
                        }
                    } catch (NumberFormatException e) {
                        log.warn("无法解析星期数: {}", weekdayStr);
                    }
                }
            }
        }
        
        log.debug("解析星期结果: {}", weekdays);
        return weekdays;
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
        List<String> timeSlots = new java.util.ArrayList<>();
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
            log.info("按空格分割后的部分: {}", java.util.Arrays.toString(parts));
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
                    log.info("按逗号分割后的时间段: {}", java.util.Arrays.toString(timeSlotArray));
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

    /**
     * 同步缓存，确保周期性调课后的缓存一致性
     */
    private void syncCacheAfterRecurringUpdate(Schedule currentSchedule, List<Schedule> futureSchedules) {
        Long teacherId = currentSchedule.getTeacherId();
        java.util.Set<java.time.LocalDate> affectedDates = new java.util.HashSet<>();

        // 添加当前课程安排的日期
        if (currentSchedule.getScheduledDate() != null) {
            affectedDates.add(currentSchedule.getScheduledDate());
        }

        // 添加所有未来课程安排的日期
        for (Schedule schedule : futureSchedules) {
            if (schedule.getScheduledDate() != null) {
                affectedDates.add(schedule.getScheduledDate());
            }
        }

        // 清理缓存
        cacheKeyEvictor.evictTeacherScheduleAndAvailability(teacherId, affectedDates);

        // 重新回填缓存
        for (java.time.LocalDate d : affectedDates) {
            java.util.List<Schedule> dayAll = scheduleMapper.findByTeacherIdAndDate(teacherId, d);
            java.util.List<String> slots = new java.util.ArrayList<>();
            for (Schedule s : dayAll) {
                slots.add(s.getStartTime().toString() + "-" + s.getEndTime().toString());
            }
            teacherScheduleCacheService.putBusySlots(teacherId, d,
                slots.isEmpty() ? java.util.Collections.emptyList() : slots);
        }
        log.info("周期性调课更新后缓存同步完成，受影响日期: {}", affectedDates);
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

package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.model.entity.*;
import com.touhouqing.grabteacherbackend.model.vo.BookingVO;
import com.touhouqing.grabteacherbackend.mapper.*;
import com.touhouqing.grabteacherbackend.model.dto.BookingApplyDTO;
import com.touhouqing.grabteacherbackend.model.dto.BookingApprovalDTO;
import com.touhouqing.grabteacherbackend.model.dto.TimeSlotDTO;
import com.touhouqing.grabteacherbackend.service.BookingService;
import com.touhouqing.grabteacherbackend.service.DistributedLockService;
import com.touhouqing.grabteacherbackend.service.CacheKeyEvictor;
import com.touhouqing.grabteacherbackend.service.TeacherScheduleCacheService;
import com.touhouqing.grabteacherbackend.service.StudentService;
import com.touhouqing.grabteacherbackend.util.TimeSlotUtil;
import com.touhouqing.grabteacherbackend.model.vo.ScheduleVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.math.BigDecimal;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    private BookingRequestMapper bookingRequestMapper;

    // 旧表已迁移，移除未使用的 ScheduleMapper

    @Autowired
    private CourseEnrollmentMapper courseEnrollmentMapper;

    @Autowired
    private CourseScheduleMapper courseScheduleMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private SubjectMapper subjectMapper;


    @Autowired
    private TeachingLocationMapper teachingLocationMapper;

    @Autowired
    private DistributedLockService distributedLockService;

    // 未直接使用，避免未使用警告

    @Autowired
    private CacheKeyEvictor cacheKeyEvictor;

    @Autowired
    private TeacherScheduleCacheService teacherScheduleCacheService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private RescheduleRequestMapper rescheduleRequestMapper;

    @Autowired
    private com.touhouqing.grabteacherbackend.mapper.TeacherDailyAvailabilityMapper teacherDailyAvailabilityMapper;


    @Override
    @Transactional
    public BookingVO createBookingRequest(BookingApplyDTO request, Long studentUserId) {
        log.info("创建预约申请，学生用户ID: {}, 教师ID: {}", studentUserId, request.getTeacherId());

        // 获取学生信息
        Student student = studentMapper.findByUserId(studentUserId);
        if (student == null) {
            throw new RuntimeException("学生信息不存在");
        }

        // 获取教师信息
        Teacher teacher = teacherMapper.selectById(request.getTeacherId());
        if (teacher == null) {
            throw new RuntimeException("教师信息不存在");
        }

        // 检查是否为试听课申请
        boolean isTrial = request.getIsTrial() != null && request.getIsTrial();
        if (isTrial) {
            // 检查用户是否可以使用免费试听
            if (!canUseFreeTrial(studentUserId)) {
                throw new RuntimeException("您的试听课次数不足，无法申请试听课");
            }

            // 申请试听课时立即标记为已使用
            markTrialUsed(studentUserId);
            log.info("学生申请试听课，立即标记为已使用，用户ID: {}", studentUserId);

            // 试听课固定30分钟，强制设置时长
            request.setTrialDurationMinutes(30);

            // 如果是试听课且为单次预约，需要调整结束时间确保为30分钟
            if ("single".equals(request.getBookingType()) && request.getRequestedStartTime() != null) {
                LocalTime startTime = request.getRequestedStartTime();
                LocalTime endTime = startTime.plusMinutes(30);
                request.setRequestedEndTime(endTime);
                log.info("试听课时间已调整为30分钟: {} - {}", startTime, endTime);
            }
        }



        // 验证授课地点选择并归一化
        Long chosenLocationId = null;
        String chosenLocationName = null;
        List<Long> allowedOfflineIds = new ArrayList<>();
        if (StringUtils.hasText(teacher.getTeachingLocations())) {
            for (String s : teacher.getTeachingLocations().split(",")) {
                if (StringUtils.hasText(s)) {
                    try { allowedOfflineIds.add(Long.parseLong(s.trim())); } catch (NumberFormatException ignore) {}
                }
            }
        }
        boolean teacherSupportsOnline = Boolean.TRUE.equals(teacher.getSupportsOnline());

        if (request.getTeachingLocationId() != null) {
            Long locId = request.getTeachingLocationId();
            if (!allowedOfflineIds.contains(locId)) {
                throw new RuntimeException("请选择该教师支持的线下授课地点");
            }
            TeachingLocation loc = teachingLocationMapper.selectById(locId);
            if (loc == null || !Boolean.TRUE.equals(loc.getIsActive())) {
                throw new RuntimeException("所选线下授课地点不存在或已停用");
            }
            chosenLocationId = loc.getId();
            chosenLocationName = loc.getName();
        } else if (StringUtils.hasText(request.getTeachingLocation())) {
            String locText = request.getTeachingLocation().trim();
            if ("线上".equals(locText)) {
                if (!teacherSupportsOnline) {
                    throw new RuntimeException("该教师不支持线上授课");
                }
                chosenLocationName = "线上";
            } else {
                throw new RuntimeException("无效的授课地点选择");
            }
        } else {
            throw new RuntimeException("请选择授课地点");
        }


        // 验证预约时间
        validateBookingTime(request, student.getId(), request.getTeacherId());

        // 先创建预约申请获取ID
        BookingRequest bookingRequest = BookingRequest.builder()
                .studentId(student.getId())
                .teacherId(request.getTeacherId())
                .courseId(request.getCourseId())
                .subjectId(request.getSubjectId())
                .bookingType(request.getBookingType())
                .requestedDate(request.getRequestedDate())
                .requestedStartTime(request.getRequestedStartTime())
                .requestedEndTime(request.getRequestedEndTime())
                .recurringWeekdays(convertListToString(request.getRecurringWeekdays()))
                .recurringTimeSlots(convertListToString(request.getRecurringTimeSlots()))
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalTimes(request.getTotalTimes())
                .studentRequirements(request.getStudentRequirements())
                .teachingLocationId(chosenLocationId)
                .teachingLocation(chosenLocationName)
                .grade(request.getGrade())
                .selectedSessionsJson(sessionsToJson(request.getSelectedSessions()))
                .isTrial(isTrial)
                .trialDurationMinutes(request.getTrialDurationMinutes())
                .status(isTrial ? "approved" : "pending")
                .build();

        bookingRequestMapper.insert(bookingRequest);
        log.info("预约申请创建成功，预约ID: {}", bookingRequest.getId());

        // 若为试听课：直接审批并生成课表（无需管理员确认）
        if (isTrial) {
            // 设置审批时间
            bookingRequest.setApprovedAt(LocalDateTime.now());
            bookingRequestMapper.updateById(bookingRequest);

            // 使用与管理员审批相同的分布式锁粒度，避免并发冲突
            String dateStr = bookingRequest.getRequestedDate() != null ? bookingRequest.getRequestedDate().toString() : "any";
            String startStr = bookingRequest.getRequestedStartTime() != null ? bookingRequest.getRequestedStartTime().toString() : "any";
            String endStr = bookingRequest.getRequestedEndTime() != null ? bookingRequest.getRequestedEndTime().toString() : "any";
            String lockKey = String.format("grabTeacher:lock:booking:teacher:%d:%s:%s-%s", bookingRequest.getTeacherId(), dateStr, startStr, endStr);
            String token = java.util.UUID.randomUUID().toString();
            boolean locked = distributedLockService.tryLock(lockKey, token, java.time.Duration.ofSeconds(10), 50, java.time.Duration.ofMillis(100));
            if (!locked) {
                throw new RuntimeException("系统繁忙，请稍后再试");
            }
            try {
                // 审批即时生成单次课程安排
                generateSingleSchedule(bookingRequest);
            } finally {
                distributedLockService.unlock(lockKey, token);
            }

            // 精准清理并回填缓存，确保前端可用性即时更新
            try {
                java.util.Set<LocalDate> affectedDates = new java.util.HashSet<>();
                if (bookingRequest.getRequestedDate() != null) {
                    affectedDates.add(bookingRequest.getRequestedDate());
                }
                cacheKeyEvictor.evictTeacherScheduleAndAvailability(bookingRequest.getTeacherId(), affectedDates);
                if (!affectedDates.isEmpty()) {
                    for (LocalDate d : affectedDates) {
                        java.util.List<CourseSchedule> dayAll =
                                courseScheduleMapper.findByTeacherIdAndDateRange(bookingRequest.getTeacherId(), d, d);
                        java.util.List<String> slots = new java.util.ArrayList<>();
                        for (CourseSchedule s : dayAll) {
                            String slot = s.getStartTime().toString() + "-" + s.getEndTime().toString();
                            slots.add(slot);
                        }
                        teacherScheduleCacheService.putBusySlots(bookingRequest.getTeacherId(), d,
                                slots.isEmpty() ? java.util.Collections.emptyList() : slots);
                    }
                }
            } catch (Exception e) {
                log.warn("试听课审批后的缓存同步失败，但不影响主流程", e);
            }
        }

        // 计算预约费用并扣除余额（试听课和恢复预约免费）
        BigDecimal totalCost = BigDecimal.ZERO;
        boolean isResume = request.getResume() != null && request.getResume();
        if (!isTrial && !isResume) {
            totalCost = calculateBookingCost(request);

            // 检查余额是否足够
            if (!studentService.checkBalance(studentUserId, totalCost)) {
                throw new RuntimeException("余额不足，需要 " + totalCost + "M豆，请联系管理员充值");
            }

            // 扣除余额，关联预约ID
            boolean deductSuccess = studentService.updateStudentBalance(
                    studentUserId,
                    totalCost.negate(), // 负数表示扣费
                    "预约课程费用",
                    bookingRequest.getId(), // 关联预约ID
                    null // 学生自主预约，无操作员
            );

            if (!deductSuccess) {
                throw new RuntimeException("余额扣除失败，请稍后重试");
            }

            log.info("已扣除学生余额，用户ID: {}, 扣除金额: {}M豆, 预约ID: {}", studentUserId, totalCost, bookingRequest.getId());
        } else if (isResume) {
            log.info("恢复课程预约，跳过费用计算与扣费，用户ID: {}, 预约ID: {}", studentUserId, bookingRequest.getId());
        }

        log.info("预约申请创建完成，ID: {}", bookingRequest.getId());
        return convertToBookingResponseDTO(bookingRequest);
    }

    @Override
    @Transactional
    public BookingVO approveBookingRequest(Long bookingId, BookingApprovalDTO approval, Long adminUserId) {
        log.info("管理员审批预约申请，预约ID: {}, 管理员用户ID: {}, 状态: {}", bookingId, adminUserId, approval.getStatus());

        BookingRequest bookingRequest = bookingRequestMapper.selectById(bookingId);
        if (bookingRequest == null) {
            throw new RuntimeException("预约申请不存在");
        }

        // 更新预约申请状态
        bookingRequest.setStatus(approval.getStatus());
        bookingRequest.setAdminNotes(approval.getAdminNotes());
        bookingRequest.setUpdatedAt(LocalDateTime.now());

        if ("approved".equals(approval.getStatus())) {
            bookingRequest.setApprovedAt(LocalDateTime.now());

            // 审核通过时，试听课使用记录保持不变（申请时已标记）
            if (bookingRequest.getIsTrial() != null && bookingRequest.getIsTrial()) {
                log.info("试听课审核通过，保持已使用状态，预约ID: {}", bookingId);
            }

            // 使用分布式锁串行化相同教师同一时间段的排课生成，避免高并发踩踏
            String dateStr = bookingRequest.getRequestedDate() != null ? bookingRequest.getRequestedDate().toString() : "any";
            String startStr = bookingRequest.getRequestedStartTime() != null ? bookingRequest.getRequestedStartTime().toString() : "any";
            String endStr = bookingRequest.getRequestedEndTime() != null ? bookingRequest.getRequestedEndTime().toString() : "any";
            String lockKey;
            if ("single".equals(bookingRequest.getBookingType())) {
                lockKey = String.format("grabTeacher:lock:booking:teacher:%d:%s:%s-%s", bookingRequest.getTeacherId(), dateStr, startStr, endStr);
            } else {
                // 周期性预约涉及多日期，使用教师级别锁
                lockKey = String.format("grabTeacher:lock:booking:teacher:%d", bookingRequest.getTeacherId());
            }
            String token = java.util.UUID.randomUUID().toString();
            boolean locked = distributedLockService.tryLock(lockKey, token, java.time.Duration.ofSeconds(10), 50, java.time.Duration.ofMillis(100));
            if (!locked) {
                // 双检：可能已有其他请求正在处理审批，尝试读取最新状态
                BookingRequest latest = bookingRequestMapper.selectById(bookingId);
                if (latest != null && "approved".equals(latest.getStatus())) {
                    log.info("预约ID:{} 已由其他请求审批通过，直接返回成功", bookingId);
                    return convertToBookingResponseDTO(latest);
                }
                throw new RuntimeException("系统繁忙，请稍后再试");
            }
            try {
                // 生成课程安排
                if ("single".equals(bookingRequest.getBookingType())) {
                    generateSingleSchedule(bookingRequest);
                } else if ("calendar".equals(bookingRequest.getBookingType())) {
                    generateCalendarSchedules(bookingRequest);
                } else if ("recurring".equals(bookingRequest.getBookingType())) {
                    throw new RuntimeException("系统已不支持按星期的周期性预约，请让用户改用“日历预约”");
                }
                // 审批通过后：若绑定了课程ID且为一对一课程，则报名人数+1并视人数限制置满
                if (bookingRequest.getCourseId() != null) {
                    try {
                        Course c = courseMapper.selectById(bookingRequest.getCourseId());
                        if (c != null && "one_on_one".equalsIgnoreCase(c.getCourseType())) {
                            int affected = courseMapper.incrementEnrollmentAndSetFullIfNeeded(bookingRequest.getCourseId());
                            if (affected <= 0) {
                                log.warn("课程报名人数未更新，可能已满或不可报名，courseId={}", bookingRequest.getCourseId());
                            }
                        }
                    } catch (Exception ex) {
                        log.error("更新课程报名人数失败，courseId={}", bookingRequest.getCourseId(), ex);
                    }
                }
            } finally {
                distributedLockService.unlock(lockKey, token);
            }

            // 课表变化后，精准清理并“更新”教师 busy 缓存 + 清理 teacherSchedule/teacherAvailability
            try {
                Set<LocalDate> affectedDates = new HashSet<>();
                if ("single".equals(bookingRequest.getBookingType())) {
                    if (bookingRequest.getRequestedDate() != null) affectedDates.add(bookingRequest.getRequestedDate());
                } else {
                    java.util.List<CourseSchedule> schedules = courseScheduleMapper.findByBookingRequestId(bookingRequest.getId());
                    for (CourseSchedule s : schedules) {
                        if (s.getScheduledDate() != null) affectedDates.add(s.getScheduledDate());
                    }
                }
                // 清 teacherSchedule/availability + busy:date Key
                cacheKeyEvictor.evictTeacherScheduleAndAvailability(bookingRequest.getTeacherId(), affectedDates);
                // 立即回填 busy slots（避免下一次读 miss）
                if (!affectedDates.isEmpty()) {
                    for (LocalDate d : affectedDates) {
                        java.util.List<CourseSchedule> dayAll =
                                courseScheduleMapper.findByTeacherIdAndDateRange(bookingRequest.getTeacherId(), d, d);
                        List<String> slots = new ArrayList<>();
                        for (CourseSchedule s : dayAll) {
                            String slot = s.getStartTime().toString() + "-" + s.getEndTime().toString();
                            slots.add(slot);
                        }
                        teacherScheduleCacheService.putBusySlots(bookingRequest.getTeacherId(), d,
                                slots.isEmpty() ? java.util.Collections.emptyList() : slots);
                    }
                }
            } catch (Exception e) {
                log.warn("精准清理/回填教师缓存失败，但不影响主流程", e);
            }
        } else if ("rejected".equals(approval.getStatus())) {
            // 如果是试听课且审核被拒绝，恢复试听课使用记录
            if (bookingRequest.getIsTrial() != null && bookingRequest.getIsTrial()) {
                Student student = studentMapper.selectById(bookingRequest.getStudentId());
                if (student != null) {
                    resetTrialUsage(student.getUserId());
                    log.info("试听课审核拒绝，恢复试听课使用记录，用户ID: {}", student.getUserId());
                }
            }

            // 退回预约费用（试听课免费不需要退费）
            if (bookingRequest.getIsTrial() == null || !bookingRequest.getIsTrial()) {
                Student student = studentMapper.selectById(bookingRequest.getStudentId());
                if (student != null) {
                    try {
                        // 重新计算费用并退回
                        BookingApplyDTO refundRequest = createRefundRequest(bookingRequest);
                        BigDecimal refundAmount = calculateBookingCost(refundRequest);

                        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                            boolean refundSuccess = studentService.updateStudentBalance(
                                    student.getUserId(),
                                    refundAmount, // 正数表示退费
                                    "预约被拒绝退费，预约ID: " + bookingId,
                                    bookingId, // 关联预约ID
                                    null // 系统自动退费，无特定操作员
                            );

                            if (refundSuccess) {
                                log.info("预约被拒绝，已退回学生余额，用户ID: {}, 退回金额: {}M豆",
                                        student.getUserId(), refundAmount);
                            } else {
                                log.error("预约被拒绝，退费失败，用户ID: {}, 应退金额: {}M豆",
                                        student.getUserId(), refundAmount);
                            }
                        }
                    } catch (Exception e) {
                        log.error("预约被拒绝，退费处理失败，预约ID: {}", bookingId, e);
                    }
                }
            }
        }

        bookingRequestMapper.updateById(bookingRequest);

        log.info("管理员预约申请审批完成，ID: {}, 状态: {}", bookingId, approval.getStatus());
        return convertToBookingResponseDTO(bookingRequest);
    }

    @Override
    public BookingVO cancelBookingRequest(Long bookingId, Long studentUserId) {
        log.info("学生取消预约申请，预约ID: {}, 学生用户ID: {}", bookingId, studentUserId);

        BookingRequest bookingRequest = bookingRequestMapper.selectById(bookingId);
        if (bookingRequest == null) {
            throw new RuntimeException("预约申请不存在");
        }

        // 验证学生权限
        Student student = studentMapper.findByUserId(studentUserId);
        if (student == null || !student.getId().equals(bookingRequest.getStudentId())) {
            throw new RuntimeException("无权限操作此预约申请");
        }

        // 只有待处理状态的申请可以取消
        if (!"pending".equals(bookingRequest.getStatus())) {
            throw new RuntimeException("只有待处理状态的申请可以取消");
        }

        // 学生取消试听课申请时，不恢复试听课使用记录
        // 只有管理员拒绝时才恢复试听课次数
        if (bookingRequest.getIsTrial() != null && bookingRequest.getIsTrial()) {
            log.info("学生取消试听课申请，试听课次数已消耗，不恢复，用户ID: {}", studentUserId);
        }

        // 退回预约费用（试听课免费不需要退费）
        if (bookingRequest.getIsTrial() == null || !bookingRequest.getIsTrial()) {
            try {
                // 重新计算费用并退回
                BookingApplyDTO refundRequest = createRefundRequest(bookingRequest);
                BigDecimal refundAmount = calculateBookingCost(refundRequest);

                if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                    boolean refundSuccess = studentService.updateStudentBalance(
                            studentUserId,
                            refundAmount, // 正数表示退费
                            "学生取消预约退费，预约ID: " + bookingId,
                            bookingId, // 关联预约ID
                            null // 学生自主取消，无操作员
                    );

                    if (refundSuccess) {
                        log.info("学生取消预约，已退回余额，用户ID: {}, 退回金额: {}M豆",
                                studentUserId, refundAmount);
                    } else {
                        log.error("学生取消预约，退费失败，用户ID: {}, 应退金额: {}M豆",
                                studentUserId, refundAmount);
                    }
                }
            } catch (Exception e) {
                log.error("学生取消预约，退费处理失败，预约ID: {}", bookingId, e);
            }
        }

        bookingRequest.setStatus("cancelled");
        bookingRequest.setUpdatedAt(LocalDateTime.now());
        bookingRequestMapper.updateById(bookingRequest);

        log.info("预约申请取消成功，ID: {}", bookingId);
        return convertToBookingResponseDTO(bookingRequest);
    }

    @Override
    public BookingVO getBookingRequestById(Long bookingId) {
        BookingRequest bookingRequest = bookingRequestMapper.selectById(bookingId);
        if (bookingRequest == null) {
            throw new RuntimeException("预约申请不存在");
        }
        return convertToBookingResponseDTO(bookingRequest);
    }

    @Override
    public Page<BookingVO> getStudentBookingRequests(Long studentUserId, int page, int size, String status) {
        Student student = studentMapper.findByUserId(studentUserId);
        if (student == null) {
            throw new RuntimeException("学生信息不存在");
        }

        QueryWrapper<BookingRequest> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("student_id", student.getId())
                .eq("is_deleted", false);

        if (StringUtils.hasText(status)) {
            queryWrapper.eq("status", status);
        }

        queryWrapper.orderByDesc("created_at");

        Page<BookingRequest> bookingPage = bookingRequestMapper.selectPage(new Page<>(page, size), queryWrapper);

        // 批量装配，避免 N+1
        List<BookingRequest> records = bookingPage.getRecords();
        List<Long> studentIds = records.stream().map(BookingRequest::getStudentId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> teacherIds = records.stream().map(BookingRequest::getTeacherId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> courseIds = records.stream().map(BookingRequest::getCourseId).filter(Objects::nonNull).distinct().collect(Collectors.toList());

        Map<Long, Student> studentMap = new HashMap<>();
        if (!studentIds.isEmpty()) {
            QueryWrapper<Student> sq = new QueryWrapper<>();
            sq.in("id", studentIds);
            for (Student s : studentMapper.selectList(sq)) studentMap.put(s.getId(), s);
        }
        Map<Long, Teacher> teacherMap = new HashMap<>();
        if (!teacherIds.isEmpty()) {
            QueryWrapper<Teacher> tq = new QueryWrapper<>();
            tq.in("id", teacherIds);
            for (Teacher t : teacherMapper.selectList(tq)) teacherMap.put(t.getId(), t);
        }
        Map<Long, Course> courseMap = new HashMap<>();
        if (!courseIds.isEmpty()) {
            QueryWrapper<Course> cq = new QueryWrapper<>();
            cq.in("id", courseIds);
            for (Course c : courseMapper.selectList(cq)) courseMap.put(c.getId(), c);
        }
        Map<Long, Subject> subjectMap = new HashMap<>();
        if (!courseMap.isEmpty()) {
            List<Long> subjectIds = courseMap.values().stream().map(Course::getSubjectId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            if (!subjectIds.isEmpty()) {
                QueryWrapper<Subject> subjQ = new QueryWrapper<>();
                subjQ.in("id", subjectIds);
                for (Subject s : subjectMapper.selectList(subjQ)) subjectMap.put(s.getId(), s);
            }
        }

        List<BookingVO> vos = new ArrayList<>();
        for (BookingRequest br : records) {
            BookingVO vo = convertToBookingResponseDTO(br);
            Student s = studentMap.get(br.getStudentId());
            if (s != null) vo.setStudentName(s.getRealName());
            Teacher t = teacherMap.get(br.getTeacherId());
            if (t != null) vo.setTeacherName(t.getRealName());
            Course c = courseMap.get(br.getCourseId());
            if (c != null) {
                vo.setCourseTitle(c.getTitle());
                Subject subj = subjectMap.get(c.getSubjectId());
                if (subj != null) vo.setSubjectName(subj.getName());
                vo.setCourseDurationMinutes(c.getDurationMinutes());
            }
            vos.add(vo);
        }

        Page<BookingVO> responsePage = new Page<>(page, size);
        responsePage.setTotal(bookingPage.getTotal());
        responsePage.setRecords(vos);
        return responsePage;
    }

    @Override
    public Page<BookingVO> getTeacherBookingRequests(Long teacherUserId, int page, int size, String status) {
        Teacher teacher = teacherMapper.findByUserId(teacherUserId);
        if (teacher == null) {
            throw new RuntimeException("教师信息不存在");
        }

        QueryWrapper<BookingRequest> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teacher_id", teacher.getId())
                .eq("is_deleted", false);

        if (StringUtils.hasText(status)) {
            queryWrapper.eq("status", status);
        }

        queryWrapper.orderByDesc("created_at");

        Page<BookingRequest> bookingPage = bookingRequestMapper.selectPage(new Page<>(page, size), queryWrapper);

        // 批量装配，避免 N+1
        List<BookingRequest> records = bookingPage.getRecords();
        List<Long> studentIds = records.stream().map(BookingRequest::getStudentId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> teacherIds = records.stream().map(BookingRequest::getTeacherId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> courseIds = records.stream().map(BookingRequest::getCourseId).filter(Objects::nonNull).distinct().collect(Collectors.toList());

        Map<Long, Student> studentMap = new HashMap<>();
        if (!studentIds.isEmpty()) {
            QueryWrapper<Student> sq = new QueryWrapper<>();
            sq.in("id", studentIds);
            for (Student s : studentMapper.selectList(sq)) studentMap.put(s.getId(), s);
        }
        Map<Long, Teacher> teacherMap = new HashMap<>();
        if (!teacherIds.isEmpty()) {
            QueryWrapper<Teacher> tq = new QueryWrapper<>();
            tq.in("id", teacherIds);
            for (Teacher t : teacherMapper.selectList(tq)) teacherMap.put(t.getId(), t);
        }
        Map<Long, Course> courseMap = new HashMap<>();
        if (!courseIds.isEmpty()) {
            QueryWrapper<Course> cq = new QueryWrapper<>();
            cq.in("id", courseIds);
            for (Course c : courseMapper.selectList(cq)) courseMap.put(c.getId(), c);
        }
        Map<Long, Subject> subjectMap = new HashMap<>();
        if (!courseMap.isEmpty()) {
            List<Long> subjectIds = courseMap.values().stream().map(Course::getSubjectId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            if (!subjectIds.isEmpty()) {
                QueryWrapper<Subject> subjQ = new QueryWrapper<>();
                subjQ.in("id", subjectIds);
                for (Subject s : subjectMapper.selectList(subjQ)) subjectMap.put(s.getId(), s);
            }
        }

        List<BookingVO> vos = new ArrayList<>();
        for (BookingRequest br : records) {
            BookingVO vo = convertToBookingResponseDTO(br);
            Student s = studentMap.get(br.getStudentId());
            if (s != null) vo.setStudentName(s.getRealName());
            Teacher t = teacherMap.get(br.getTeacherId());
            if (t != null) vo.setTeacherName(t.getRealName());
            Course c = courseMap.get(br.getCourseId());
            if (c != null) {
                vo.setCourseTitle(c.getTitle());
                Subject subj = subjectMap.get(c.getSubjectId());
                if (subj != null) vo.setSubjectName(subj.getName());
                vo.setCourseDurationMinutes(c.getDurationMinutes());
            }
            vos.add(vo);
        }

        Page<BookingVO> responsePage = new Page<>(page, size);
        responsePage.setTotal(bookingPage.getTotal());
        responsePage.setRecords(vos);
        return responsePage;
    }

    @Override
    public List<ScheduleVO> getTeacherSchedules(Long teacherUserId, LocalDate startDate, LocalDate endDate) {
        Teacher teacher = teacherMapper.findByUserId(teacherUserId);
        if (teacher == null) {
            throw new RuntimeException("教师信息不存在");
        }

        java.util.List<CourseSchedule> schedules =
                courseScheduleMapper.findByTeacherIdAndDateRange(teacher.getId(), startDate, endDate);
        return schedules.stream()
                .map(this::convertToScheduleVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScheduleVO> getStudentSchedules(Long studentUserId, LocalDate startDate, LocalDate endDate) {
        Student student = studentMapper.findByUserId(studentUserId);
        if (student == null) {
            throw new RuntimeException("学生信息不存在");
        }

        java.util.List<CourseSchedule> schedules =
                courseScheduleMapper.findByStudentIdAndDateRange(student.getId(), startDate, endDate);
        return schedules.stream()
                .map(cs -> convertToScheduleVO(cs))
                .collect(Collectors.toList());
    }

    @Override
    public List<ScheduleVO> getAllStudentSchedules(Long studentUserId) {
        log.info("获取学生所有课程安排，用户ID: {}", studentUserId);

        Student student = studentMapper.findByUserId(studentUserId);
        if (student == null) {
            log.warn("学生信息不存在，用户ID: {}", studentUserId);
            throw new RuntimeException("学生信息不存在，请先完善学生资料");
        }

        log.info("找到学生信息，学生ID: {}, 学生姓名: {}", student.getId(), student.getRealName());

        List<CourseSchedule> schedules =
                courseScheduleMapper.findByStudentId(student.getId());

        log.info("查询到课程安排数量: {}", schedules.size());

        List<ScheduleVO> result = schedules.stream()
                .map(cs -> convertToScheduleVO(cs))
                .collect(Collectors.toList());

        log.info("转换后的课程安排数量: {}", result.size());
        return result;
    }

    /**
     * 将CourseSchedule实体转换为ScheduleVO
     */
    private ScheduleVO convertToScheduleVO(CourseSchedule cs) {
        ScheduleVO vo = new ScheduleVO();
        vo.setId(cs.getId());
        vo.setTeacherId(cs.getTeacherId());
        vo.setStudentId(cs.getStudentId());
        vo.setCourseId(cs.getCourseId());
        vo.setScheduledDate(cs.getScheduledDate());
        vo.setStartTime(cs.getStartTime());
        vo.setEndTime(cs.getEndTime());
        vo.setStatus(mapScheduleStatusToLegacy(cs.getScheduleStatus()));
        vo.setTeacherNotes(cs.getTeacherNotes());
        vo.setStudentFeedback(cs.getStudentFeedback());
        vo.setCreatedAt(cs.getCreatedAt());
        vo.setBookingRequestId(cs.getBookingRequestId());
        vo.setEnrollmentId(cs.getEnrollmentId());
        vo.setSessionNumber(cs.getSessionNumber());

        // 计算课程时长
        if (cs.getStartTime() != null && cs.getEndTime() != null) {
            long durationMinutes = java.time.Duration.between(cs.getStartTime(), cs.getEndTime()).toMinutes();
            vo.setDurationMinutes((int) durationMinutes);
        }

        // 优先使用JOIN结果，减少额外查询
        if (cs.getTeacherName() != null) {
            vo.setTeacherName(cs.getTeacherName());
        } else if (cs.getTeacherId() != null) {
            Teacher teacher = teacherMapper.selectById(cs.getTeacherId());
            if (teacher != null) {
                vo.setTeacherName(teacher.getRealName());
            }
        }

        if (cs.getStudentName() != null) {
            vo.setStudentName(cs.getStudentName());
        } else if (cs.getStudentId() != null) {
            Student student = studentMapper.selectById(cs.getStudentId());
            if (student != null) {
                vo.setStudentName(student.getRealName());
            }
        }

        if (cs.getCourseTitle() != null) {
            vo.setCourseTitle(cs.getCourseTitle());
            if (cs.getCourseType() != null) vo.setCourseType(cs.getCourseType());
            if (cs.getSubjectName() != null) vo.setSubjectName(cs.getSubjectName());
        } else if (cs.getCourseId() != null) {
            Course course = courseMapper.selectById(cs.getCourseId());
            if (course != null) {
                vo.setCourseTitle(course.getTitle());
                vo.setCourseType(course.getCourseType());
                if (course.getSubjectId() != null) {
                    Subject subject = subjectMapper.selectById(course.getSubjectId());
                    if (subject != null) {
                        vo.setSubjectName(subject.getName());
                    }
                }
            }
        }

        // JOIN 已提供时长/试听/类型时，优先使用
        if (cs.getDurationMinutes() != null) {
            vo.setDurationMinutes(cs.getDurationMinutes());
        }
        if (vo.getTrial() == null && cs.getTrial() != null) {
            vo.setTrial(cs.getTrial());
        }
        if (cs.getCourseType() != null) {
            vo.setCourseType(cs.getCourseType());
        }

        // 获取报名信息（并补全一对一课程默认标题）
        CourseEnrollment enrollment = null;
        if (cs.getEnrollmentId() != null) {
            enrollment = courseEnrollmentMapper.selectById(cs.getEnrollmentId());
            if (enrollment != null) {
                vo.setTrial(enrollment.getTrial());
                if (enrollment.getTotalSessions() != null) {
                    vo.setTotalTimes(enrollment.getTotalSessions());
                }
                // 优先使用报名关系中的课程时长
                if (enrollment.getDurationMinutes() != null) {
                    vo.setDurationMinutes(enrollment.getDurationMinutes());
                }
            }
        }

        // 若为一对一课程（courseId 为空 或 enrollmentType=one_on_one），按规则自动生成课程标题：姓名+科目+年级+一对一课程
        boolean isOneOnOne = (cs.getCourseId() == null) || (enrollment != null && "one_on_one".equalsIgnoreCase(enrollment.getEnrollmentType()));
        if (isOneOnOne) {
            // 学生姓名
            String studentName = vo.getStudentName();
            if (studentName == null && cs.getStudentId() != null) {
                Student s = studentMapper.selectById(cs.getStudentId());
                if (s != null) studentName = s.getRealName();
            }
            studentName = studentName == null ? "" : studentName;

            // 年级（优先报名关系中的年级）
            String grade = null;
            if (enrollment != null) {
                grade = enrollment.getGrade();
            }
            grade = grade == null ? "" : grade;

            // 科目名称（优先已有 subjectName；否则尝试从预约备注中解析，仅在必要时做一次查询）
            String subjectName = vo.getSubjectName();
            if ((subjectName == null || subjectName.isEmpty()) && cs.getBookingRequestId() != null) {
                BookingRequest br = bookingRequestMapper.selectById(cs.getBookingRequestId());
                if (br != null && br.getStudentRequirements() != null) {
                    String req = br.getStudentRequirements();
                    int idx1 = req.indexOf("进行");
                    int idx2 = req.indexOf("科目", idx1 + 2);
                    if (idx1 >= 0 && idx2 > idx1) {
                        String sub = req.substring(idx1 + 2, idx2).trim();
                        if (!sub.isEmpty()) subjectName = sub;
                    }
                }
            }
            if (subjectName == null) subjectName = "";


            // 生成并设置标题（严格使用库中年级，不做学段识别）：姓名+科目+年级+一对一课程
            if (vo.getCourseTitle() == null || vo.getCourseTitle().isEmpty()) {
                StringBuilder titleBuilder = new StringBuilder();
                titleBuilder.append(studentName);
                titleBuilder.append(subjectName);
                titleBuilder.append(grade);
                titleBuilder.append("一对一课程");
                vo.setCourseTitle(titleBuilder.toString());
            }

            // 也回填 subjectName，便于前端展示
            if ((vo.getSubjectName() == null || vo.getSubjectName().isEmpty()) && !subjectName.isEmpty()) {
                vo.setSubjectName(subjectName);
            }

            // 明确标记类型
            vo.setCourseType("one_on_one");
        }

        return vo;
    }

    /**
     * 兼容前端旧状态：scheduled -> progressing
     */
    private String mapScheduleStatusToLegacy(String scheduleStatus) {
        if (scheduleStatus == null) return null;
        if ("scheduled".equals(scheduleStatus)) return "progressing";
        return scheduleStatus; // completed/cancelled/rescheduled 原样
    }

    @Override
    public boolean canUseFreeTrial(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null && user.getTrialTimes() != null && user.getTrialTimes() > 0;
    }

    @Override
    @Transactional
    public void markTrialUsed(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null && user.getTrialTimes() != null && user.getTrialTimes() > 0) {
            // 记录原始次数，用于后续可能的恢复
            user.setTrialTimes(user.getTrialTimes() - 1);
            userMapper.updateById(user);
            log.info("标记用户已使用免费试听，用户ID: {}, 剩余次数: {}", userId, user.getTrialTimes());
        }
    }

    @Override
    @Transactional
    public void resetTrialUsage(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            // 拒绝试听申请时，恢复试听次数+1（即恢复到申请前的状态）
            user.setTrialTimes(user.getTrialTimes() + 1);
            userMapper.updateById(user);
            log.info("试听课申请被拒绝，恢复试听次数+1，用户ID: {}, 当前次数: {}", userId, user.getTrialTimes());
        }
    }

    @Override
    public boolean hasTeacherTimeConflict(Long teacherId, LocalDate date, String startTime, String endTime) {
        LocalTime start = LocalTime.parse(startTime);
        LocalTime end = LocalTime.parse(endTime);

        // 检查常规课程冲突
        int conflictCount = courseScheduleMapper.countTeacherConflicts(teacherId, date, start, end);
        if (conflictCount > 0) {
            return true;
        }

        // 检查试听课预约是否会影响基础2小时区间的可用性
        // 如果预约的是2小时课程，需要检查是否与试听课冲突
        if (isTwoHourCourse(start, end)) {
            // 将时间段映射到基础2小时区间
            String baseSlot = mapToBaseTimeSlot(startTime, endTime);
            if (baseSlot != null) {
                String[] baseTimes = baseSlot.split("-");
                if (baseTimes.length == 2) {
                    // 已排程试听与待处理试听都将占用基础2小时段
                    boolean scheduledTrialConflict = hasTrialConflictInBaseSlot(teacherId, date, baseTimes[0], baseTimes[1]);
                    boolean pendingTrialConflict = hasPendingTrialConflictInBaseSlot(teacherId, date, baseTimes[0], baseTimes[1]);
                    return scheduledTrialConflict || pendingTrialConflict;
                }
            }
        }

        return false;
    }

    /**
     * 判断是否为2小时课程
     */
    private boolean isTwoHourCourse(LocalTime start, LocalTime end) {
        return java.time.Duration.between(start, end).toMinutes() == 120;
    }

    /**
     * 将时间段映射到基础2小时区间
     */
    private String mapToBaseTimeSlot(String startTime, String endTime) {
        // 基础2小时时间段：08:00-10:00, 10:00-12:00, 13:00-15:00, 15:00-17:00, 17:00-19:00, 19:00-21:00
        String[] baseSlots = {
            "08:00-10:00", "10:00-12:00", "13:00-15:00",
            "15:00-17:00", "17:00-19:00", "19:00-21:00"
        };

        LocalTime start = LocalTime.parse(startTime);
        LocalTime end = LocalTime.parse(endTime);

        for (String baseSlot : baseSlots) {
            String[] times = baseSlot.split("-");
            LocalTime baseStart = LocalTime.parse(times[0]);
            LocalTime baseEnd = LocalTime.parse(times[1]);

            // 检查时间段是否在基础区间内
            if ((start.isAfter(baseStart) || start.equals(baseStart)) &&
                (end.isBefore(baseEnd) || end.equals(baseEnd))) {
                return baseSlot;
            }
        }

        return null;
    }

    @Override
    public boolean hasStudentTimeConflict(Long studentId, LocalDate date, String startTime, String endTime) {
        LocalTime start = LocalTime.parse(startTime);
        LocalTime end = LocalTime.parse(endTime);
        // 改为查询新表
        int conflictCount = courseScheduleMapper.countStudentConflicts(studentId, date, start, end);
        return conflictCount > 0;
    }

    @Override
    public boolean hasTrialConflictInBaseSlot(Long teacherId, LocalDate date, String baseStartTime, String baseEndTime) {
        LocalTime baseStart = LocalTime.parse(baseStartTime);
        LocalTime baseEnd = LocalTime.parse(baseEndTime);
        int conflictCount = courseScheduleMapper.countTrialConflictsInBaseSlot(teacherId, date, baseStart, baseEnd);
        return conflictCount > 0;
    }


    @Override
    public boolean hasTrialTimeConflict(Long teacherId, LocalDate date, String startTime, String endTime) {
        LocalTime start = LocalTime.parse(startTime);
        LocalTime end = LocalTime.parse(endTime);

        // 试听课之间可以共存，但需要检查是否会影响基础2小时区间的可用性
        // 如果8:00-8:30有试听课，那么8:00-10:00的基础区间应该被标记为不可用（用于正式课）
        // 但是8:30-10:00之间还可以预约试听课

        // 检查是否有完全相同的30分钟试听课时间段
        int exactConflictCount = courseScheduleMapper.countTrialTimeConflicts(teacherId, date, start, end);
        if (exactConflictCount > 0) {
            return true; // 完全相同的时间段不能重复预约
        }

        // 检查是否会影响基础2小时区间的可用性
        String baseSlot = mapToBaseTimeSlot(startTime, endTime);
        if (baseSlot != null) {
            String[] baseTimes = baseSlot.split("-");
            if (baseTimes.length == 2) {
                // 检查该基础区间是否已经被试听课占用
                boolean hasTrialInBase = hasTrialConflictInBaseSlot(teacherId, date, baseTimes[0], baseTimes[1]);
                if (hasTrialInBase) {
                    // 如果基础区间已被试听课占用，检查当前30分钟段是否在占用范围内
                    if (isTrialSlotInOccupiedBase(startTime, endTime, baseSlot)) {
                        return true; // 该时间段会影响基础区间的可用性
                    }
                }
            }
        }

        return false;
    }

    /**
     * 检查30分钟试听课时间段是否在已被占用的基础区间内
     */
    private boolean isTrialSlotInOccupiedBase(String startTime, String endTime, String baseSlot) {
        String[] baseTimes = baseSlot.split("-");

        LocalTime trialStart = LocalTime.parse(startTime);
        LocalTime baseStart = LocalTime.parse(baseTimes[0]);

        // 如果试听课的开始时间等于基础区间的开始时间，则认为该基础区间被占用
        // 例如：8:00-8:30的试听课会占用8:00-10:00的基础区间
        if (trialStart.equals(baseStart)) {
            return true;
        }

        return false;
    }


    /**
     * 检查“待处理的试听申请”是否占用基础2小时区间（提交即占位，管理员拒绝/学生取消才释放）
     */
    private boolean hasPendingTrialConflictInBaseSlot(Long teacherId, LocalDate date, String baseStartTime, String baseEndTime) {
        LocalTime baseStart = LocalTime.parse(baseStartTime);
        LocalTime baseEnd = LocalTime.parse(baseEndTime);
        int cnt = bookingRequestMapper.countPendingTrialConflictsInBaseSlot(teacherId, date, baseStart, baseEnd);
        return cnt > 0;
    }

    @Override
    @Transactional
    public int generateRecurringSchedules(BookingRequest bookingRequest) {
        log.info("生成周期性课程安排，预约申请ID: {}, 总次数: {}, 开始日期: {}, 结束日期: {}",
                bookingRequest.getId(), bookingRequest.getTotalTimes(),
                bookingRequest.getStartDate(), bookingRequest.getEndDate());

        if (!"recurring".equals(bookingRequest.getBookingType())) {
            throw new RuntimeException("只能为周期性预约生成课程安排");
        }

        List<Integer> weekdays = convertStringToIntegerList(bookingRequest.getRecurringWeekdays());
        List<String> timeSlots = convertStringToList(bookingRequest.getRecurringTimeSlots());

        if (weekdays.isEmpty() || timeSlots.isEmpty()) {
            throw new RuntimeException("周期性预约的星期和时间段不能为空");
        }

        log.info("预约配置 - 星期几: {}, 时间段: {}", weekdays, timeSlots);

        // 获取课程时长信息
        Integer courseDurationMinutes = null;
        if (bookingRequest.getCourseId() != null) {
            Course course = courseMapper.selectById(bookingRequest.getCourseId());
            if (course != null) {
                courseDurationMinutes = course.getDurationMinutes();
            }
        }
        // 如果课程没有设置时长，使用默认值120分钟
        if (courseDurationMinutes == null) {
            courseDurationMinutes = 120;
        }

        // 1) 先创建报名关系
        CourseEnrollment enrollment =
                CourseEnrollment.builder()
                        .studentId(bookingRequest.getStudentId())
                        .teacherId(bookingRequest.getTeacherId())
                        .courseId(bookingRequest.getCourseId())
                        .enrollmentType(bookingRequest.getCourseId() == null ? "one_on_one" : "large_class")
                        .totalSessions(bookingRequest.getTotalTimes())
                        .completedSessions(0)
                        .enrollmentStatus("active")
                        .enrollmentDate(java.time.LocalDate.now())
                        .startDate(bookingRequest.getStartDate())
                        .endDate(bookingRequest.getEndDate())
                        .trial(Boolean.TRUE.equals(bookingRequest.getIsTrial()))
                        .recurringSchedule(buildRecurringScheduleJson(weekdays, timeSlots))
                        .durationMinutes(courseDurationMinutes)
                        .teachingLocationId(bookingRequest.getTeachingLocationId())
                        .teachingLocation(bookingRequest.getTeachingLocation())
                        .grade(bookingRequest.getGrade())
                        .bookingRequestId(bookingRequest.getId())
                        .deleted(false)
                        .build();
        courseEnrollmentMapper.insert(enrollment);

        int generatedCount = 0;
        int sessionNumber = 1;
        LocalDate currentDate = bookingRequest.getStartDate();
        LocalDate endDate = bookingRequest.getEndDate();

        // 如果没有设置总次数，按原逻辑处理
        if (bookingRequest.getTotalTimes() == null) {
            return generateRecurringSchedulesWithoutLimit(bookingRequest, weekdays, timeSlots, currentDate, endDate);
        }

        int targetTotalTimes = bookingRequest.getTotalTimes();
        int maxCycles = 10; // 最多循环10轮，防止无限循环
        int currentCycle = 0;

        // 持续循环直到生成足够的课程数量
        while (generatedCount < targetTotalTimes && currentCycle < maxCycles) {
            currentCycle++;

            log.debug("开始第 {} 轮课程生成，当前已生成: {} 节，目标: {} 节", currentCycle, generatedCount, targetTotalTimes);

            while ((currentDate.isBefore(endDate) || currentDate.isEqual(endDate)) && generatedCount < targetTotalTimes) {
                int dayOfWeek = currentDate.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday
                int weekdayValue = dayOfWeek == 7 ? 0 : dayOfWeek; // 转换为0=Sunday, 1=Monday格式

                if (weekdays.contains(weekdayValue)) {
                    for (String timeSlot : timeSlots) {
                        if (generatedCount >= targetTotalTimes) {
                            break; // 已达到目标数量
                        }

                        String[] times = timeSlot.split("-");
                        if (times.length == 2) {
                            LocalTime startTime = LocalTime.parse(times[0]);
                            LocalTime endTime = LocalTime.parse(times[1]);

                            // 检查时间冲突（使用新表）
                            if (!hasTeacherTimeConflict(bookingRequest.getTeacherId(), currentDate, times[0], times[1]) &&
                                !hasStudentTimeConflict(bookingRequest.getStudentId(), currentDate, times[0], times[1])) {

                                    CourseSchedule cs =
                                        CourseSchedule.builder()
                                                .enrollmentId(enrollment.getId())
                                                .scheduledDate(currentDate)
                                                .startTime(startTime)
                                                .endTime(endTime)
                                                .sessionNumber(sessionNumber)
                                                .scheduleStatus("scheduled")
                                                .deleted(false)
                                                .build();

                                courseScheduleMapper.insert(cs);
                                generatedCount++;
                                sessionNumber++;

                                log.debug("成功生成第 {} 节课程安排，日期: {}, 时间: {}-{}",
                                        sessionNumber - 1, currentDate, times[0], times[1]);
                            } else {
                                log.debug("跳过冲突时间段，日期: {}, 时间: {}-{}", currentDate, times[0], times[1]);
                            }
                        }
                    }
                }
                currentDate = currentDate.plusDays(1);
            }

            // 如果还没生成足够的课程，扩展结束日期继续生成
            if (generatedCount < targetTotalTimes && currentCycle < maxCycles) {
                endDate = endDate.plusMonths(1); // 扩展一个月
                log.info("课程数量不足，扩展结束日期到: {}，继续生成剩余 {} 节课",
                        endDate, targetTotalTimes - generatedCount);
            }
        }

        log.info("周期性课程安排生成完成，预约申请ID: {}, 预期总次数: {}, 实际生成: {} 个安排",
                bookingRequest.getId(), targetTotalTimes, generatedCount);

        // 如果经过多轮循环仍然无法生成足够的课程，记录错误
        if (generatedCount < targetTotalTimes) {
            log.error("无法生成足够的课程！预约申请ID: {}, 预期: {} 节课, 实际生成: {} 节课, 可能原因：时间冲突过多或配置不合理",
                    bookingRequest.getId(), targetTotalTimes, generatedCount);
        }

        return generatedCount;
    }

    /**
     * 生成周期性课程安排（无总次数限制）
     * 用于处理没有设置总次数的情况，按原有逻辑生成
     */
    private int generateRecurringSchedulesWithoutLimit(BookingRequest bookingRequest,
                                                      List<Integer> weekdays,
                                                      List<String> timeSlots,
                                                      LocalDate currentDate,
                                                      LocalDate endDate) {
        // 获取课程时长信息
        Integer courseDurationMinutes = null;
        if (bookingRequest.getCourseId() != null) {
            Course course = courseMapper.selectById(bookingRequest.getCourseId());
            if (course != null) {
                courseDurationMinutes = course.getDurationMinutes();
            }
        }
        // 如果课程没有设置时长，使用默认值120分钟
        if (courseDurationMinutes == null) {
            courseDurationMinutes = 120;
        }

        // 与有限次逻辑保持一致：先创建报名，再生成安排
        CourseEnrollment enrollment =
                CourseEnrollment.builder()
                        .studentId(bookingRequest.getStudentId())
                        .teacherId(bookingRequest.getTeacherId())
                        .courseId(bookingRequest.getCourseId())
                        .enrollmentType(bookingRequest.getCourseId() == null ? "one_on_one" : "large_class")
                        .totalSessions(null)
                        .completedSessions(0)
                        .enrollmentStatus("active")
                        .enrollmentDate(java.time.LocalDate.now())
                        .startDate(bookingRequest.getStartDate())
                        .endDate(bookingRequest.getEndDate())
                        .trial(Boolean.TRUE.equals(bookingRequest.getIsTrial()))
                        .recurringSchedule(buildRecurringScheduleJson(weekdays, timeSlots))
                        .durationMinutes(courseDurationMinutes)
                        .teachingLocationId(bookingRequest.getTeachingLocationId())
                        .teachingLocation(bookingRequest.getTeachingLocation())
                        .grade(bookingRequest.getGrade())
                        .bookingRequestId(bookingRequest.getId())
                        .deleted(false)
                        .build();
        courseEnrollmentMapper.insert(enrollment);

        int generatedCount = 0;
        int sessionNumber = 1;

        while (currentDate.isBefore(endDate) || currentDate.isEqual(endDate)) {
            int dayOfWeek = currentDate.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday
            int weekdayValue = dayOfWeek == 7 ? 0 : dayOfWeek; // 转换为0=Sunday, 1=Monday格式

            if (weekdays.contains(weekdayValue)) {
                for (String timeSlot : timeSlots) {
                    String[] times = timeSlot.split("-");
                    if (times.length == 2) {
                        LocalTime startTime = LocalTime.parse(times[0]);
                        LocalTime endTime = LocalTime.parse(times[1]);

                        // 检查时间冲突（使用新表）
                        if (!hasTeacherTimeConflict(bookingRequest.getTeacherId(), currentDate, times[0], times[1]) &&
                            !hasStudentTimeConflict(bookingRequest.getStudentId(), currentDate, times[0], times[1])) {

                            CourseSchedule cs =
                                    CourseSchedule.builder()
                                            .enrollmentId(enrollment.getId())
                                            .scheduledDate(currentDate)
                                            .startTime(startTime)
                                            .endTime(endTime)
                                            .sessionNumber(sessionNumber)
                                            .scheduleStatus("scheduled")
                                            .deleted(false)
                                            .build();

                            courseScheduleMapper.insert(cs);
                            generatedCount++;
                            sessionNumber++;

                            log.debug("成功生成第 {} 节课程安排，日期: {}, 时间: {}-{}",
                                    sessionNumber - 1, currentDate, times[0], times[1]);
                        } else {
                            log.debug("跳过冲突时间段，日期: {}, 时间: {}-{}", currentDate, times[0], times[1]);
                        }
                    }
                }
            }
            currentDate = currentDate.plusDays(1);
        }

        return generatedCount;
    }

    @Override
    public Page<BookingVO> getAdminBookingRequests(int page, int size, String status, String keyword) {
        QueryWrapper<BookingRequest> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_deleted", false);

        if (StringUtils.hasText(status)) {
            queryWrapper.eq("status", status);
        }

        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like("student_requirements", keyword)
                    .or().like("teacher_reply", keyword)
                    .or().like("admin_notes", keyword));
        }

        queryWrapper.orderByDesc("created_at");

        Page<BookingRequest> bookingPage = bookingRequestMapper.selectPage(new Page<>(page, size), queryWrapper);

        // 批量装配，避免 N+1
        List<BookingRequest> records = bookingPage.getRecords();
        List<Long> studentIds = records.stream().map(BookingRequest::getStudentId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> teacherIds = records.stream().map(BookingRequest::getTeacherId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> courseIds = records.stream().map(BookingRequest::getCourseId).filter(Objects::nonNull).distinct().collect(Collectors.toList());

        Map<Long, Student> studentMap = new HashMap<>();
        if (!studentIds.isEmpty()) {
            QueryWrapper<Student> sq = new QueryWrapper<>();
            sq.in("id", studentIds);
            for (Student s : studentMapper.selectList(sq)) studentMap.put(s.getId(), s);
        }
        Map<Long, Teacher> teacherMap = new HashMap<>();
        if (!teacherIds.isEmpty()) {
            QueryWrapper<Teacher> tq = new QueryWrapper<>();
            tq.in("id", teacherIds);
            for (Teacher t : teacherMapper.selectList(tq)) teacherMap.put(t.getId(), t);
        }
        Map<Long, Course> courseMap = new HashMap<>();
        if (!courseIds.isEmpty()) {
            QueryWrapper<Course> cq = new QueryWrapper<>();
            cq.in("id", courseIds);
            for (Course c : courseMapper.selectList(cq)) courseMap.put(c.getId(), c);
        }
        Map<Long, Subject> subjectMap = new HashMap<>();
        if (!courseMap.isEmpty()) {
            List<Long> subjectIds = courseMap.values().stream().map(Course::getSubjectId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            if (!subjectIds.isEmpty()) {
                QueryWrapper<Subject> subjQ = new QueryWrapper<>();
                subjQ.in("id", subjectIds);
                for (Subject s : subjectMapper.selectList(subjQ)) subjectMap.put(s.getId(), s);
            }
        }

        List<BookingVO> vos = new ArrayList<>();
        for (BookingRequest br : records) {
            BookingVO vo = convertToBookingResponseDTO(br);
            Student s = studentMap.get(br.getStudentId());
            if (s != null) vo.setStudentName(s.getRealName());
            Teacher t = teacherMap.get(br.getTeacherId());
            if (t != null) vo.setTeacherName(t.getRealName());
            Course c = courseMap.get(br.getCourseId());
            if (c != null) {
                vo.setCourseTitle(c.getTitle());
                Subject subj = subjectMap.get(c.getSubjectId());
                if (subj != null) vo.setSubjectName(subj.getName());
                vo.setCourseDurationMinutes(c.getDurationMinutes());
            }
            vos.add(vo);
        }

        Page<BookingVO> responsePage = new Page<>(page, size);
        responsePage.setTotal(bookingPage.getTotal());
        responsePage.setRecords(vos);
        return responsePage;
    }



    // ==================== 私有辅助方法 ====================

    /**
     * 验证预约时间
     */
    private void validateBookingTime(BookingApplyDTO request, Long studentId, Long teacherId) {
        if ("single".equals(request.getBookingType())) {
            // 单次预约验证
            if (request.getRequestedDate() == null || request.getRequestedStartTime() == null || request.getRequestedEndTime() == null) {
                throw new RuntimeException("单次预约的日期和时间不能为空");
            }

            // 检查时间冲突
            String startTime = request.getRequestedStartTime().toString();
            String endTime = request.getRequestedEndTime().toString();

            // 如果是试听课，使用特殊的冲突检查逻辑
            if (request.getIsTrial() != null && request.getIsTrial()) {
                if (hasTrialTimeConflict(teacherId, request.getRequestedDate(), startTime, endTime)) {
                    throw new RuntimeException("该试听课时间段不可用，可能与其他试听课冲突或影响基础时间段");
                }
            } else {
                if (hasTeacherTimeConflict(teacherId, request.getRequestedDate(), startTime, endTime)) {
                    throw new RuntimeException("教师在该时间段已有其他安排");
                }
            }

            if (hasStudentTimeConflict(studentId, request.getRequestedDate(), startTime, endTime)) {
                throw new RuntimeException("学生在该时间段已有其他安排");
            }

            // 验证单次预约时间是否在教师可预约时间范围内
            validateSingleBookingTime(request, teacherId);

            // 额外：对该日期的“待处理单次调课”做占用检查（避免与调课申请抢占）
            java.util.List<RescheduleRequest> pendingReschedules =
                    rescheduleRequestMapper.findPendingSingleByTeacherAndDate(teacherId, request.getRequestedDate());
            if (pendingReschedules != null && !pendingReschedules.isEmpty()) {
                for (RescheduleRequest rr : pendingReschedules) {
                    if (isOverlap(request.getRequestedStartTime(), request.getRequestedEndTime(), rr.getNewStartTime(), rr.getNewEndTime())) {
                        throw new RuntimeException("教师该时间段已有待处理调课占用，暂不可选择");
                    }
                }
            }
        } else if ("recurring".equals(request.getBookingType())) {
            // 已移除按星期的周期性预约，请使用按日历预约
            throw new RuntimeException("系统已改为按月日历预约，不再支持按星期的周期性预约，请改用“日历预约”方式");
        } else if ("calendar".equals(request.getBookingType())) {
            // 日历多次预约验证
            if (request.getIsTrial() != null && request.getIsTrial()) {
                throw new RuntimeException("试听课不支持日历批量预约，请使用单次预约并选择30分钟段");
            }
            if (request.getSelectedSessions() == null || request.getSelectedSessions().isEmpty()) {
                throw new RuntimeException("calendar预约缺少会话列表");
            }
            validateCalendarBookingTime(request, studentId, teacherId);
        }
    }

    /**
     * 生成单次课程安排
     */
    private void generateSingleSchedule(BookingRequest bookingRequest) {
        log.info("生成单次课程安排，预约申请ID: {}", bookingRequest.getId());

        // 记录试听课信息
        if (bookingRequest.getIsTrial() != null && bookingRequest.getIsTrial()) {
            log.info("生成试听课安排，固定30分钟时长");
        }

        // 审批时再次校验该时间段是否仍然可用（防止创建和审批之间产生新冲突）
        String startStr = bookingRequest.getRequestedStartTime() != null ? bookingRequest.getRequestedStartTime().toString() : null;
        String endStr = bookingRequest.getRequestedEndTime() != null ? bookingRequest.getRequestedEndTime().toString() : null;
        if (bookingRequest.getRequestedDate() != null && startStr != null && endStr != null) {
            if (hasTeacherTimeConflict(bookingRequest.getTeacherId(), bookingRequest.getRequestedDate(), startStr, endStr)) {
                throw new RuntimeException("教师该时间段已被占用，请选择其他时间");
            }
            if (hasStudentTimeConflict(bookingRequest.getStudentId(), bookingRequest.getRequestedDate(), startStr, endStr)) {
                throw new RuntimeException("学生该时间段已存在课程，请选择其他时间");
            }
        }

        // 获取课程时长信息
        Integer courseDurationMinutes = null;
        if (bookingRequest.getCourseId() != null) {
            Course course = courseMapper.selectById(bookingRequest.getCourseId());
            if (course != null) {
                courseDurationMinutes = course.getDurationMinutes();
            }
        }
        // 如果课程没有设置时长，使用默认值120分钟
        if (courseDurationMinutes == null) {
            courseDurationMinutes = 120;
        }

        // 1) 创建报名关系（单次）
        CourseEnrollment enrollment =
                CourseEnrollment.builder()
                        .studentId(bookingRequest.getStudentId())
                        .teacherId(bookingRequest.getTeacherId())
                        .courseId(bookingRequest.getCourseId())
                        .enrollmentType(bookingRequest.getCourseId() == null ? "one_on_one" : "large_class")
                        .totalSessions(1)
                        .completedSessions(0)
                        .enrollmentStatus("active")
                        .enrollmentDate(java.time.LocalDate.now())
                        .startDate(bookingRequest.getRequestedDate())
                        .endDate(bookingRequest.getRequestedDate())
                        .trial(Boolean.TRUE.equals(bookingRequest.getIsTrial()))
                        .durationMinutes(courseDurationMinutes)
                        .teachingLocationId(bookingRequest.getTeachingLocationId())
                        .teachingLocation(bookingRequest.getTeachingLocation())
                        .grade(bookingRequest.getGrade())
                        .bookingRequestId(bookingRequest.getId())
                        .deleted(false)
                        .build();
        courseEnrollmentMapper.insert(enrollment);

        // 2) 创建单次课程安排
        CourseSchedule cs =
                CourseSchedule.builder()
                        .enrollmentId(enrollment.getId())
                        .scheduledDate(bookingRequest.getRequestedDate())
                        .startTime(bookingRequest.getRequestedStartTime())
                        .endTime(bookingRequest.getRequestedEndTime())
                        .sessionNumber(1)
                        .scheduleStatus("scheduled")
                        .deleted(false)
                        .build();
        courseScheduleMapper.insert(cs);

        // 计算并记录时长信息
        if (bookingRequest.getRequestedStartTime() != null && bookingRequest.getRequestedEndTime() != null) {
            LocalTime startTime = bookingRequest.getRequestedStartTime();
            LocalTime endTime = bookingRequest.getRequestedEndTime();
            int durationMinutes = (int) java.time.Duration.between(startTime, endTime).toMinutes();
            log.info("单次课程安排生成完成，安排ID: {}，时长: {}分钟", cs.getId(), durationMinutes);
        } else {
            log.info("单次课程安排生成完成，安排ID: {}", cs.getId());
        }
    }

    /**
     * 生成按日历选择的多个课程安排
     */
    private int generateCalendarSchedules(BookingRequest bookingRequest) {
        if (!"calendar".equals(bookingRequest.getBookingType())) {
            throw new RuntimeException("只能为calendar预约生成课程安排");
        }
        java.util.List<CalendarSession> sessions = parseSelectedSessions(bookingRequest.getSelectedSessionsJson());
        if (sessions == null || sessions.isEmpty()) {
            throw new RuntimeException("calendar预约为空，无法生成课程安排");
        }
        // 按日期+开始时间排序
        sessions.sort(java.util.Comparator.comparing((CalendarSession s) -> s.date).thenComparing(s -> s.start));

        // 获取课程时长信息
        Integer courseDurationMinutes = null;
        if (bookingRequest.getCourseId() != null) {
            Course course = courseMapper.selectById(bookingRequest.getCourseId());
            if (course != null) courseDurationMinutes = course.getDurationMinutes();
        }
        if (courseDurationMinutes == null || courseDurationMinutes <= 0) {
            // 以第一个会话时长为准
            CalendarSession first = sessions.get(0);
            courseDurationMinutes = (int) java.time.Duration.between(first.start, first.end).toMinutes();
        }

        // 创建报名关系
        LocalDate startDate = sessions.get(0).date;
        LocalDate endDate = sessions.get(sessions.size() - 1).date;
        CourseEnrollment enrollment = CourseEnrollment.builder()
                .studentId(bookingRequest.getStudentId())
                .teacherId(bookingRequest.getTeacherId())
                .courseId(bookingRequest.getCourseId())
                .enrollmentType(bookingRequest.getCourseId() == null ? "one_on_one" : "large_class")
                .totalSessions(sessions.size())
                .completedSessions(0)
                .enrollmentStatus("active")
                .enrollmentDate(java.time.LocalDate.now())
                .startDate(startDate)
                .endDate(endDate)
                .trial(Boolean.TRUE.equals(bookingRequest.getIsTrial()))
                .durationMinutes(courseDurationMinutes)
                .teachingLocationId(bookingRequest.getTeachingLocationId())
                .teachingLocation(bookingRequest.getTeachingLocation())
                .grade(bookingRequest.getGrade())
                .bookingRequestId(bookingRequest.getId())
                .deleted(false)
                .build();
        courseEnrollmentMapper.insert(enrollment);

        int sessionNumber = 1;
        for (CalendarSession s : sessions) {
            CourseSchedule cs = CourseSchedule.builder()
                    .enrollmentId(enrollment.getId())
                    .scheduledDate(s.date)
                    .startTime(s.start)
                    .endTime(s.end)
                    .sessionNumber(sessionNumber++)
                    .scheduleStatus("scheduled")
                    .deleted(false)
                    .build();
            courseScheduleMapper.insert(cs);
        }

        log.info("calendar课程安排生成完成，预约ID:{}，共{}节，日期范围:{}~{}", bookingRequest.getId(), sessions.size(), startDate, endDate);
        return sessions.size();
    }


    /**
     * 将List转换为逗号分隔的字符串
     */
    private String convertListToString(List<?> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));
    }

    /**
     * 将逗号分隔的字符串转换为字符串List
     */
    private List<String> convertStringToList(String str) {
        if (!StringUtils.hasText(str)) {
            return new ArrayList<>();
        }
        return Arrays.asList(str.split(","));
    }

    /**
     * 将逗号分隔的字符串转换为整数List
     */
    private List<Integer> convertStringToIntegerList(String str) {
        if (!StringUtils.hasText(str)) {
            return new ArrayList<>();
        }
        return Arrays.stream(str.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    private String buildRecurringScheduleJson(List<Integer> weekdays, List<String> timeSlots) {
        // 简单JSON拼装，保持与SQL文件示例一致
        String weekdayArray = weekdays == null || weekdays.isEmpty() ? "[]" : weekdays.toString();
        String timeArray = timeSlots == null || timeSlots.isEmpty() ? "[]" :
                ("[" + timeSlots.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(",")) + "]");
        return "{\"weekdays\":" + weekdayArray + ",\"timeSlots\":" + timeArray + "}";
    }

    /**
     * 将BookingRequest转换为BookingResponseDTO
     */
    private BookingVO convertToBookingResponseDTO(BookingRequest bookingRequest) {
        // 获取学生信息
        Student student = studentMapper.selectById(bookingRequest.getStudentId());
        String studentName = student != null ? student.getRealName() : "未知学生";

        // 获取教师信息
        Teacher teacher = teacherMapper.selectById(bookingRequest.getTeacherId());
        String teacherName = teacher != null ? teacher.getRealName() : "未知教师";

        // 获取课程信息
        String courseTitle = null;
        String subjectName = null;
        Integer courseDurationMinutes = null;
        if (bookingRequest.getCourseId() != null) {
            Course course = courseMapper.selectById(bookingRequest.getCourseId());
            if (course != null) {
                courseTitle = course.getTitle();
                courseDurationMinutes = course.getDurationMinutes();
                // 获取科目信息
                Subject subject = subjectMapper.selectById(course.getSubjectId());
                subjectName = subject != null ? subject.getName() : null;
            }

        }

        // 若课程未绑定或未获取到科目，则根据预约保存的 subjectId 回填（支持试听课）
        if (subjectName == null && bookingRequest.getSubjectId() != null) {
            Subject subject = subjectMapper.selectById(bookingRequest.getSubjectId());
            if (subject != null) {
                subjectName = subject.getName();
            }
        }
        // 兼容历史数据：如果仍为空且为试听课，尝试从学生需求中解析科目名称
        if (subjectName == null && Boolean.TRUE.equals(bookingRequest.getIsTrial())) {
            String req = bookingRequest.getStudentRequirements();
            if (req != null) {
                int idx1 = req.indexOf("进行");
                int idx2 = req.indexOf("科目", idx1 + 2);
                if (idx1 >= 0 && idx2 > idx1) {
                    String sub = req.substring(idx1 + 2, idx2).trim();
                    if (!sub.isEmpty()) subjectName = sub;
                }
            }
        }

        String grade = bookingRequest.getGrade();
        if (courseTitle == null || courseTitle.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(studentName);
            if (subjectName != null) sb.append(subjectName);
            if (grade != null) sb.append(grade);
            sb.append("一对一课程");
            courseTitle = sb.toString();
        }


        return BookingVO.builder()
                .id(bookingRequest.getId())
                .studentId(bookingRequest.getStudentId())
                .studentName(studentName)
                .teacherId(bookingRequest.getTeacherId())
                .teacherName(teacherName)
                .courseId(bookingRequest.getCourseId())
                .courseTitle(courseTitle)
                .subjectName(subjectName)
                .courseDurationMinutes(courseDurationMinutes)
                .grade(grade)
                .bookingType(bookingRequest.getBookingType())
                .requestedDate(bookingRequest.getRequestedDate())
                .requestedStartTime(bookingRequest.getRequestedStartTime())
                .requestedEndTime(bookingRequest.getRequestedEndTime())
                .recurringWeekdays(convertStringToIntegerList(bookingRequest.getRecurringWeekdays()))
                .recurringTimeSlots(convertStringToList(bookingRequest.getRecurringTimeSlots()))
                .startDate(bookingRequest.getStartDate())
                .endDate(bookingRequest.getEndDate())
                .totalTimes(bookingRequest.getTotalTimes())
                .studentRequirements(bookingRequest.getStudentRequirements())
                .status(bookingRequest.getStatus())
                .teacherReply(bookingRequest.getTeacherReply())
                .adminNotes(bookingRequest.getAdminNotes())
                .teachingLocationId(bookingRequest.getTeachingLocationId())
                .teachingLocation(bookingRequest.getTeachingLocation())
                .createdAt(bookingRequest.getCreatedAt())
                .updatedAt(bookingRequest.getUpdatedAt())
                .approvedAt(bookingRequest.getApprovedAt())
                .trial(bookingRequest.getIsTrial())
                .trialDurationMinutes(bookingRequest.getTrialDurationMinutes())
                .build();
    }

    // 删除未使用的 convertToScheduleResponseDTO(Schedule)

    /**
     * 验证单次预约时间是否在教师可预约时间范围内
     */
    private void validateSingleBookingTime(BookingApplyDTO request, Long teacherId) {
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            throw new RuntimeException("教师信息不存在");
        }

        // 先检查“按日历设置”的可用性（若该日已设置，则以日历为准）
        String requestedTimeSlot = request.getRequestedStartTime() + "-" + request.getRequestedEndTime();
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.touhouqing.grabteacherbackend.model.entity.TeacherDailyAvailability> dq =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        dq.eq("teacher_id", teacherId).eq("available_date", request.getRequestedDate());
        com.touhouqing.grabteacherbackend.model.entity.TeacherDailyAvailability daily =
                teacherDailyAvailabilityMapper.selectOne(dq);
        if (daily != null) {
            try {
                java.util.List<String> slots = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(daily.getTimeSlotsJson(), new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>(){});
                boolean ok = slots != null && slots.stream().anyMatch(av -> isTimeSlotContained(requestedTimeSlot, av));
                if (!ok) {
                    throw new RuntimeException("该日期的时间段不在教师可预约范围内，请选择其他时间");
                }
            } catch (Exception e) {
                throw new RuntimeException("教师日历可预约数据异常，请稍后再试");
            }
            // 日历校验通过，直接进入“待处理预约占用”校验
        } else {
            // 不再回退到按周模板：若该日未设置日历可预约，则视为不可预约
            throw new RuntimeException("教师未在该日开放可预约时间，请选择其他日期或联系教师");
        }

        // 额外校验：该时间段是否已被其他学生的“待处理”预约占用
        // 单次预约仅校验当天
        java.time.LocalDate date = request.getRequestedDate();
        java.util.List<BookingRequest> pendings = bookingRequestMapper.findPendingByTeacherAndDateRange(teacherId, date, date);
        if (pendings != null && !pendings.isEmpty()) {
            for (BookingRequest br : pendings) {
                if ("single".equals(br.getBookingType())) {
                    if (br.getRequestedDate() != null && br.getRequestedDate().isEqual(date)) {
                        if (isOverlap(request.getRequestedStartTime(), request.getRequestedEndTime(), br.getRequestedStartTime(), br.getRequestedEndTime())) {
                            throw new RuntimeException("教师该时间段已有待处理预约，暂不可选择");
                        }
                    }
                } else if ("recurring".equals(br.getBookingType())) {
                    // 周期性待处理：若星期相同且时间段重叠则视为冲突
                    java.util.List<Integer> brWeekdays = convertStringToIntegerList(br.getRecurringWeekdays());
                    java.util.List<String> brSlots = convertStringToList(br.getRecurringTimeSlots());
                    int wd = request.getRequestedDate().getDayOfWeek().getValue();
                    int weekdayValue = wd == 7 ? 0 : wd;
                    if (brWeekdays != null && brWeekdays.contains(weekdayValue) && brSlots != null) {
                        for (String slot : brSlots) {
                            String[] times = slot.split("-");
                            if (times.length == 2) {
                                java.time.LocalTime brStart = java.time.LocalTime.parse(times[0]);
                                java.time.LocalTime brEnd = java.time.LocalTime.parse(times[1]);
                                if (isOverlap(request.getRequestedStartTime(), request.getRequestedEndTime(), brStart, brEnd)) {
                                    throw new RuntimeException("教师该时间段已有待处理预约，暂不可选择");
                                }
                            }
                        }
                    }
                }
            }
        }

        int _wd = request.getRequestedDate().getDayOfWeek().getValue();
        log.info("单次预约时间验证通过，教师ID: {}, 时间: {} {}", teacherId, getWeekdayName(_wd), requestedTimeSlot);
    }

    /**
     * 验证周期性预约时间是否在教师可预约时间范围内
     */
    private void validateRecurringBookingTime(BookingApplyDTO request, Long teacherId) {
        // 已全面改为按日历预约，不再支持“按星期”的周期性预约
        throw new RuntimeException("系统已不支持按星期的周期性预约，请改用\u201c日历预约\u201d");
    }

    /**
     * 获取星期几的中文名称
     */
    private String getWeekdayName(int weekday) {
        String[] weekdays = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        return weekday >= 1 && weekday <= 7 ? weekdays[weekday] : "未知";
    }

    /**
     * 验证按日历选择的多个会话是否可用且无冲突
     */
    private void validateCalendarBookingTime(BookingApplyDTO request, Long studentId, Long teacherId) {
        for (var ses : request.getSelectedSessions()) {
            if (ses.getDate() == null || ses.getStartTime() == null || ses.getEndTime() == null) {
                throw new RuntimeException("calendar预约存在无效的时间配置");
            }
            int minutes = (int) java.time.Duration.between(ses.getStartTime(), ses.getEndTime()).toMinutes();
            if (minutes != 90 && minutes != 120) {
                throw new RuntimeException("calendar预约仅支持1.5小时或2小时");
            }

            // 使用单次预约校验，复用“日历优先/周模板兜底”的可预约范围检查
            BookingApplyDTO singleReq = new BookingApplyDTO();
            singleReq.setBookingType("single");
            singleReq.setRequestedDate(ses.getDate());
            singleReq.setRequestedStartTime(ses.getStartTime());
            singleReq.setRequestedEndTime(ses.getEndTime());
            singleReq.setIsTrial(false);
            validateSingleBookingTime(singleReq, teacherId);

            String startStr = ses.getStartTime().toString();
            String endStr = ses.getEndTime().toString();
            if (hasTeacherTimeConflict(teacherId, ses.getDate(), startStr, endStr)) {
                throw new RuntimeException("教师在" + ses.getDate() + " " + startStr + "-" + endStr + "已有其他安排");
            }
            if (hasStudentTimeConflict(studentId, ses.getDate(), startStr, endStr)) {
                throw new RuntimeException("学生在" + ses.getDate() + " " + startStr + "-" + endStr + "已有其他安排");
            }

            // 待处理单次调课占用检查
            java.util.List<RescheduleRequest> pendingReschedules =
                    rescheduleRequestMapper.findPendingSingleByTeacherAndDate(teacherId, ses.getDate());
            if (pendingReschedules != null && !pendingReschedules.isEmpty()) {
                for (RescheduleRequest rr : pendingReschedules) {
                    if (isOverlap(ses.getStartTime(), ses.getEndTime(), rr.getNewStartTime(), rr.getNewEndTime())) {
                        throw new RuntimeException("教师该时间段已有待处理调课占用，暂不可选择");
                    }
                }
            }
        }
    }


    /**
     * 检查请求的时间段是否在教师可预约时间段内
     * @param requestedTimeSlot 请求的时间段，格式：HH:mm-HH:mm
     * @param availableTimeSlot 教师可预约时间段，格式：HH:mm-HH:mm
     * @return 如果请求时间段完全在可预约时间段内，返回true
     */
    private boolean isTimeSlotContained(String requestedTimeSlot, String availableTimeSlot) {
        if (!TimeSlotUtil.isValidTimeSlot(requestedTimeSlot) || !TimeSlotUtil.isValidTimeSlot(availableTimeSlot)) {
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
            // 例如：08:00-09:30 应该在 08:00-10:00 内
            boolean isContained = !requestedStart.isBefore(availableStart) && !requestedEnd.isAfter(availableEnd);

            log.debug("时间段包含检查: requested={}, available={}, isContained={}",
                     requestedTimeSlot, availableTimeSlot, isContained);

            return isContained;
        } catch (Exception e) {
            log.error("时间段比较失败: requested={}, available={}", requestedTimeSlot, availableTimeSlot, e);
            return false;
        }
    }

    /**
     * 判断两个时间段是否重叠
     */
    private boolean isOverlap(java.time.LocalTime aStart, java.time.LocalTime aEnd,
                              java.time.LocalTime bStart, java.time.LocalTime bEnd) {
        if (aStart == null || aEnd == null || bStart == null || bEnd == null) return false;
        // 存在任意重叠：aStart < bEnd 且 bStart < aEnd
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    /**
     * 计算预约费用
     */
    private BigDecimal calculateBookingCost(BookingApplyDTO request) {
        try {
            // 获取课程信息
            Course course = courseMapper.selectById(request.getCourseId());
            if (course == null) {
                throw new RuntimeException("课程信息不存在");
            }

            // 如果课程没有设置价格，返回0（表示面议）
            if (course.getPrice() == null) {
                log.warn("课程ID {} 没有设置价格，预约费用为0", request.getCourseId());
                return BigDecimal.ZERO;
            }

            BigDecimal pricePerHour = course.getPrice(); // 每小时价格
            Integer durationMinutes = course.getDurationMinutes(); // 每次课时长（分钟）

            // 如果课程没有设置时长，使用学生选择的时长
            if (durationMinutes == null || durationMinutes <= 0) {
                durationMinutes = request.getSelectedDurationMinutes();
                if (durationMinutes == null || durationMinutes <= 0) {
                    throw new RuntimeException("课程未设置时长且学生未选择时长，无法计算费用");
                }
            }

            // 计算每次课的费用
            BigDecimal hoursPerSession = BigDecimal.valueOf(durationMinutes).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
            BigDecimal pricePerSession = pricePerHour.multiply(hoursPerSession);

            // 根据预约类型计算总费用
            BigDecimal totalCost = BigDecimal.ZERO;
            int times = 1;
            if ("single".equals(request.getBookingType())) {
                totalCost = pricePerSession;
                times = 1;
            } else if ("recurring".equals(request.getBookingType())) {
                Integer totalTimes = request.getTotalTimes();
                if (totalTimes == null || totalTimes <= 0) {
                    throw new RuntimeException("周期性预约次数设置异常");
                }
                times = totalTimes;
                totalCost = pricePerSession.multiply(BigDecimal.valueOf(times));
            } else if ("calendar".equals(request.getBookingType())) {
                if (request.getSelectedSessions() == null || request.getSelectedSessions().isEmpty()) {
                    throw new RuntimeException("calendar预约缺少会话列表");
                }
                // 以第一个会话的时长为准，并校验一致性
                java.time.LocalTime s0 = request.getSelectedSessions().get(0).getStartTime();
                java.time.LocalTime e0 = request.getSelectedSessions().get(0).getEndTime();
                int d0 = (int) java.time.Duration.between(s0, e0).toMinutes();
                if (d0 != 90 && d0 != 120) {
                    throw new RuntimeException("calendar预约仅支持1.5小时或2小时");
                }
                // 若课程未设置时长，以上述d0为准
                if (course.getDurationMinutes() == null || course.getDurationMinutes() <= 0) {
                    durationMinutes = d0;
                    hoursPerSession = BigDecimal.valueOf(durationMinutes).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
                    pricePerSession = pricePerHour.multiply(hoursPerSession);
                }
                for (var ses : request.getSelectedSessions()) {
                    int d = (int) java.time.Duration.between(ses.getStartTime(), ses.getEndTime()).toMinutes();
                    if (d != d0) {
                        throw new RuntimeException("calendar预约内的所有会话时长必须一致");
                    }
                }
                times = request.getSelectedSessions().size();
                totalCost = pricePerSession.multiply(BigDecimal.valueOf(times));
            } else {
                throw new RuntimeException("不支持的预约类型：" + request.getBookingType());


            }

            log.info("计算预约费用: 课程ID={}, 每小时价格={}M豆, 每次课时长={}分钟, 预约次数={}, 总费用={}M豆",
                    request.getCourseId(), pricePerHour, durationMinutes,
                    times,
                    totalCost);

            return totalCost;
        } catch (Exception e) {
            log.error("计算预约费用失败: {}", e.getMessage(), e);
            throw new RuntimeException("计算预约费用失败：" + e.getMessage());
        }
    }



    /**
     * 从预约记录创建退费请求对象
     */
    private BookingApplyDTO createRefundRequest(BookingRequest bookingRequest) {
        BookingApplyDTO refundRequest = new BookingApplyDTO();
        refundRequest.setCourseId(bookingRequest.getCourseId());
        refundRequest.setBookingType(bookingRequest.getBookingType());
        refundRequest.setTotalTimes(bookingRequest.getTotalTimes());
        return refundRequest;
    }

    // ==== Calendar sessions helpers ====
    private String sessionsToJson(java.util.List<com.touhouqing.grabteacherbackend.model.dto.SelectedSessionDTO> sessions) {
        if (sessions == null || sessions.isEmpty()) return null;
        try {
            java.util.List<java.util.Map<String, String>> arr = new java.util.ArrayList<>();
            for (var s : sessions) {
                if (s == null || s.getDate() == null || s.getStartTime() == null || s.getEndTime() == null) continue;
                java.util.Map<String, String> m = new java.util.HashMap<>();
                m.put("date", s.getDate().toString());
                m.put("start", s.getStartTime().toString());
                m.put("end", s.getEndTime().toString());
                arr.add(m);
            }
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(arr);
        } catch (Exception e) {
            throw new RuntimeException("所选时段序列化失败");
        }
    }

    private static class CalendarSession {
        java.time.LocalDate date;
        java.time.LocalTime start;
        java.time.LocalTime end;
    }

    private java.util.List<CalendarSession> parseSelectedSessions(String json) {
        java.util.List<CalendarSession> list = new java.util.ArrayList<>();
        if (json == null || json.isEmpty()) return list;
        try {
            var om = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.List<java.util.Map<String, String>> arr = om.readValue(json,
                    new com.fasterxml.jackson.core.type.TypeReference<java.util.List<java.util.Map<String, String>>>(){});
            if (arr != null) {
                for (var m : arr) {
                    CalendarSession cs = new CalendarSession();
                    cs.date = java.time.LocalDate.parse(m.get("date"));
                    cs.start = java.time.LocalTime.parse(m.get("start"));
                    cs.end = java.time.LocalTime.parse(m.get("end"));
                    list.add(cs);
                }
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("预约记录解析失败，请稍后重试");
        }
    }

}

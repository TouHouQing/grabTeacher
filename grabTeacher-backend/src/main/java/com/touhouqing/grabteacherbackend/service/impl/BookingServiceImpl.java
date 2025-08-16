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
import com.touhouqing.grabteacherbackend.service.TeacherCacheWarmupService;
import com.touhouqing.grabteacherbackend.service.CacheKeyEvictor;
import com.touhouqing.grabteacherbackend.service.TeacherScheduleCacheService;
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

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    private BookingRequestMapper bookingRequestMapper;

    @Autowired
    private ScheduleMapper scheduleMapper;

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
    private DistributedLockService distributedLockService;

    @Autowired
    private TeacherCacheWarmupService teacherCacheWarmupService;

    @Autowired
    private CacheKeyEvictor cacheKeyEvictor;

    @Autowired
    private TeacherScheduleCacheService teacherScheduleCacheService;

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
        boolean isTrial = request.getTrial() != null && request.getTrial();
        if (isTrial) {
            // 检查用户是否可以使用免费试听
            if (!canUseFreeTrial(studentUserId)) {
                throw new RuntimeException("您已使用过免费试听课，无法再次申请");
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

        // 验证预约时间
        validateBookingTime(request, student.getId(), request.getTeacherId());

        // 创建预约申请
        BookingRequest bookingRequest = BookingRequest.builder()
                .studentId(student.getId())
                .teacherId(request.getTeacherId())
                .courseId(request.getCourseId())
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
                .trial(isTrial)
                .trialDurationMinutes(request.getTrialDurationMinutes())
                .status("pending")
                .build();

        bookingRequestMapper.insert(bookingRequest);

        log.info("预约申请创建成功，ID: {}", bookingRequest.getId());
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
            if (bookingRequest.getTrial() != null && bookingRequest.getTrial()) {
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
                } else if ("recurring".equals(bookingRequest.getBookingType())) {
                    generateRecurringSchedules(bookingRequest);
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
                    List<Schedule> schedules = scheduleMapper.findByBookingRequestId(bookingRequest.getId());
                    for (Schedule s : schedules) {
                        if (s.getScheduledDate() != null) affectedDates.add(s.getScheduledDate());
                    }
                }
                // 清 teacherSchedule/availability + busy:date Key
                cacheKeyEvictor.evictTeacherScheduleAndAvailability(bookingRequest.getTeacherId(), affectedDates);
                // 立即回填 busy slots（避免下一次读 miss）
                if (!affectedDates.isEmpty()) {
                    for (LocalDate d : affectedDates) {
                        List<Schedule> dayAll = scheduleMapper.findByTeacherIdAndDate(bookingRequest.getTeacherId(), d);
                        List<String> slots = new ArrayList<>();
                        for (Schedule s : dayAll) {
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
            if (bookingRequest.getTrial() != null && bookingRequest.getTrial()) {
                Student student = studentMapper.selectById(bookingRequest.getStudentId());
                if (student != null) {
                    resetTrialUsage(student.getUserId());
                    log.info("试听课审核拒绝，恢复试听课使用记录，用户ID: {}", student.getUserId());
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

        // 如果是试听课申请被取消，恢复试听课使用记录
        if (bookingRequest.getTrial() != null && bookingRequest.getTrial()) {
            resetTrialUsage(studentUserId);
            log.info("试听课申请取消，恢复试听课使用记录，用户ID: {}", studentUserId);
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

        List<Schedule> schedules = scheduleMapper.findByTeacherIdAndDateRange(teacher.getId(), startDate, endDate);
        return schedules.stream()
                .map(this::convertToScheduleResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScheduleVO> getStudentSchedules(Long studentUserId, LocalDate startDate, LocalDate endDate) {
        Student student = studentMapper.findByUserId(studentUserId);
        if (student == null) {
            throw new RuntimeException("学生信息不存在");
        }

        List<Schedule> schedules = scheduleMapper.findByStudentIdAndDateRange(student.getId(), startDate, endDate);
        return schedules.stream()
                .map(this::convertToScheduleResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScheduleVO> getAllStudentSchedules(Long studentUserId) {
        Student student = studentMapper.findByUserId(studentUserId);
        if (student == null) {
            throw new RuntimeException("学生信息不存在");
        }

        List<Schedule> schedules = scheduleMapper.findByStudentId(student.getId());
        return schedules.stream()
                .map(this::convertToScheduleResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean canUseFreeTrial(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null && (user.getHasUsedTrial() == null || !user.getHasUsedTrial());
    }

    @Override
    @Transactional
    public void markTrialUsed(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setHasUsedTrial(true);
            user.setTrialUsedAt(LocalDateTime.now());
            userMapper.updateById(user);
            log.info("标记用户已使用免费试听，用户ID: {}", userId);
        }
    }

    @Override
    @Transactional
    public void resetTrialUsage(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setHasUsedTrial(false);
            user.setTrialUsedAt(null);
            userMapper.updateById(user);
            log.info("恢复用户试听课使用记录，用户ID: {}", userId);
        }
    }

    @Override
    public boolean hasTeacherTimeConflict(Long teacherId, LocalDate date, String startTime, String endTime) {
        LocalTime start = LocalTime.parse(startTime);
        LocalTime end = LocalTime.parse(endTime);
        int conflictCount = scheduleMapper.countConflictingSchedules(teacherId, date, start, end);
        return conflictCount > 0;
    }

    @Override
    public boolean hasStudentTimeConflict(Long studentId, LocalDate date, String startTime, String endTime) {
        LocalTime start = LocalTime.parse(startTime);
        LocalTime end = LocalTime.parse(endTime);
        int conflictCount = scheduleMapper.countStudentConflictingSchedules(studentId, date, start, end);
        return conflictCount > 0;
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

                            // 检查时间冲突
                            if (!hasTeacherTimeConflict(bookingRequest.getTeacherId(), currentDate, times[0], times[1]) &&
                                !hasStudentTimeConflict(bookingRequest.getStudentId(), currentDate, times[0], times[1])) {

                                Schedule schedule = Schedule.builder()
                                        .teacherId(bookingRequest.getTeacherId())
                                        .studentId(bookingRequest.getStudentId())
                                        .courseId(bookingRequest.getCourseId())
                                        .scheduledDate(currentDate)
                                        .startTime(startTime)
                                        .endTime(endTime)
                                        .totalTimes(bookingRequest.getTotalTimes())
                                        .status("progressing")
                                        .bookingRequestId(bookingRequest.getId())
                                        .bookingSource("request")
                                        .trial(bookingRequest.getTrial())
                                        .sessionNumber(sessionNumber)
                                        .recurringWeekdays(bookingRequest.getRecurringWeekdays())
                                        .recurringTimeSlots(bookingRequest.getRecurringTimeSlots())
                                        .build();

                                scheduleMapper.insert(schedule);
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

                        // 检查时间冲突
                        if (!hasTeacherTimeConflict(bookingRequest.getTeacherId(), currentDate, times[0], times[1]) &&
                            !hasStudentTimeConflict(bookingRequest.getStudentId(), currentDate, times[0], times[1])) {

                            Schedule schedule = Schedule.builder()
                                    .teacherId(bookingRequest.getTeacherId())
                                    .studentId(bookingRequest.getStudentId())
                                    .courseId(bookingRequest.getCourseId())
                                    .scheduledDate(currentDate)
                                    .startTime(startTime)
                                    .endTime(endTime)
                                    .totalTimes(bookingRequest.getTotalTimes())
                                    .status("progressing")
                                    .bookingRequestId(bookingRequest.getId())
                                    .bookingSource("request")
                                    .trial(bookingRequest.getTrial())
                                    .sessionNumber(sessionNumber)
                                    .recurringWeekdays(bookingRequest.getRecurringWeekdays())
                                    .recurringTimeSlots(bookingRequest.getRecurringTimeSlots())
                                    .build();

                            scheduleMapper.insert(schedule);
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

            if (hasTeacherTimeConflict(teacherId, request.getRequestedDate(), startTime, endTime)) {
                throw new RuntimeException("教师在该时间段已有其他安排");
            }

            if (hasStudentTimeConflict(studentId, request.getRequestedDate(), startTime, endTime)) {
                throw new RuntimeException("学生在该时间段已有其他安排");
            }

            // 验证单次预约时间是否在教师可预约时间范围内
            validateSingleBookingTime(request, teacherId);
        } else if ("recurring".equals(request.getBookingType())) {
            // 周期性预约验证
            if (request.getRecurringWeekdays() == null || request.getRecurringWeekdays().isEmpty() ||
                request.getRecurringTimeSlots() == null || request.getRecurringTimeSlots().isEmpty() ||
                request.getStartDate() == null || request.getEndDate() == null) {
                throw new RuntimeException("周期性预约的星期、时间段、开始日期和结束日期不能为空");
            }

            if (request.getStartDate().isAfter(request.getEndDate())) {
                throw new RuntimeException("开始日期不能晚于结束日期");
            }

            // 验证周期性预约时间是否在教师可预约时间范围内
            validateRecurringBookingTime(request, teacherId);
        }
    }

    /**
     * 生成单次课程安排
     */
    private void generateSingleSchedule(BookingRequest bookingRequest) {
        log.info("生成单次课程安排，预约申请ID: {}", bookingRequest.getId());

        // 记录试听课信息
        if (bookingRequest.getTrial() != null && bookingRequest.getTrial()) {
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

        Schedule schedule = Schedule.builder()
                .teacherId(bookingRequest.getTeacherId())
                .studentId(bookingRequest.getStudentId())
                .courseId(bookingRequest.getCourseId())
                .scheduledDate(bookingRequest.getRequestedDate())
                .startTime(bookingRequest.getRequestedStartTime())
                .endTime(bookingRequest.getRequestedEndTime())
                .totalTimes(1)
                .status("progressing")
                .bookingRequestId(bookingRequest.getId())
                .bookingSource("request")
                .trial(bookingRequest.getTrial())
                .sessionNumber(1)
                .build();

        scheduleMapper.insert(schedule);

        // 计算并记录时长信息
        if (bookingRequest.getRequestedStartTime() != null && bookingRequest.getRequestedEndTime() != null) {
            LocalTime startTime = bookingRequest.getRequestedStartTime();
            LocalTime endTime = bookingRequest.getRequestedEndTime();
            int durationMinutes = (int) java.time.Duration.between(startTime, endTime).toMinutes();
            log.info("单次课程安排生成完成，安排ID: {}，时长: {}分钟", schedule.getId(), durationMinutes);
        } else {
            log.info("单次课程安排生成完成，安排ID: {}", schedule.getId());
        }
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
                .createdAt(bookingRequest.getCreatedAt())
                .updatedAt(bookingRequest.getUpdatedAt())
                .approvedAt(bookingRequest.getApprovedAt())
                .trial(bookingRequest.getTrial())
                .trialDurationMinutes(bookingRequest.getTrialDurationMinutes())
                .build();
    }

    /**
     * 将Schedule转换为ScheduleResponseDTO
     */
    private ScheduleVO convertToScheduleResponseDTO(Schedule schedule) {
        // 获取学生信息
        Student student = studentMapper.selectById(schedule.getStudentId());
        String studentName = student != null ? student.getRealName() : "未知学生";

        // 获取教师信息
        Teacher teacher = teacherMapper.selectById(schedule.getTeacherId());
        String teacherName = teacher != null ? teacher.getRealName() : "未知教师";

        // 获取课程信息
        Course course = courseMapper.selectById(schedule.getCourseId());
        String courseTitle = course != null ? course.getTitle() : "未知课程";
        String courseType = course != null ? course.getCourseType() : null;

        // 获取科目信息
        String subjectName = null;
        if (course != null && course.getSubjectId() != null) {
            Subject subject = subjectMapper.selectById(course.getSubjectId());
            subjectName = subject != null ? subject.getName() : null;
        }

        // 计算课程时长
        Integer durationMinutes = null;
        if (schedule.getStartTime() != null && schedule.getEndTime() != null) {
            durationMinutes = (int) java.time.Duration.between(schedule.getStartTime(), schedule.getEndTime()).toMinutes();
        }

        return ScheduleVO.builder()
                .id(schedule.getId())
                .teacherId(schedule.getTeacherId())
                .teacherName(teacherName)
                .studentId(schedule.getStudentId())
                .studentName(studentName)
                .courseId(schedule.getCourseId())
                .courseTitle(courseTitle)
                .subjectName(subjectName)
                .scheduledDate(schedule.getScheduledDate())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .durationMinutes(durationMinutes)
                .totalTimes(schedule.getTotalTimes())
                .status(schedule.getStatus())
                .teacherNotes(schedule.getTeacherNotes())
                .studentFeedback(schedule.getStudentFeedback())
                .createdAt(schedule.getCreatedAt())
                .bookingRequestId(schedule.getBookingRequestId())
                .bookingSource(schedule.getBookingSource())
                .trial(schedule.getTrial())
                .sessionNumber(schedule.getSessionNumber())
                .courseType(courseType)
                .build();
    }

    /**
     * 验证单次预约时间是否在教师可预约时间范围内
     */
    private void validateSingleBookingTime(BookingApplyDTO request, Long teacherId) {
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            throw new RuntimeException("教师信息不存在");
        }

        // 如果教师没有设置可预约时间，则默认所有时间都可以预约
        if (teacher.getAvailableTimeSlots() == null || teacher.getAvailableTimeSlots().trim().isEmpty()) {
            log.info("教师未设置可预约时间限制，允许所有时间预约，教师ID: {}", teacherId);
            return;
        }

        List<TimeSlotDTO> teacherAvailableSlots = TimeSlotUtil.fromJsonString(teacher.getAvailableTimeSlots());
        if (teacherAvailableSlots == null || teacherAvailableSlots.isEmpty()) {
            log.info("教师可预约时间为空，允许所有时间预约，教师ID: {}", teacherId);
            return;
        }

        // 获取预约日期对应的星期几 (1=周一, 7=周日)
        int weekday = request.getRequestedDate().getDayOfWeek().getValue();
        String requestedTimeSlot = request.getRequestedStartTime() + "-" + request.getRequestedEndTime();

        // 检查该星期几是否有可预约时间
        boolean isAvailable = teacherAvailableSlots.stream()
                .anyMatch(slot -> weekday == slot.getWeekday() &&
                         slot.getTimeSlots() != null &&
                         slot.getTimeSlots().stream().anyMatch(time ->
                             isTimeSlotContained(requestedTimeSlot, time)));

        if (!isAvailable) {
            String weekdayName = getWeekdayName(weekday);
            throw new RuntimeException(String.format("教师在%s %s时间段不可预约，请选择其他时间",
                    weekdayName, requestedTimeSlot));
        }

        log.info("单次预约时间验证通过，教师ID: {}, 时间: {} {}", teacherId, getWeekdayName(weekday), requestedTimeSlot);
    }

    /**
     * 验证周期性预约时间是否在教师可预约时间范围内
     */
    private void validateRecurringBookingTime(BookingApplyDTO request, Long teacherId) {
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            throw new RuntimeException("教师信息不存在");
        }

        // 如果教师没有设置可预约时间，则默认所有时间都可以预约
        if (teacher.getAvailableTimeSlots() == null || teacher.getAvailableTimeSlots().trim().isEmpty()) {
            log.info("教师未设置可预约时间限制，允许所有时间预约，教师ID: {}", teacherId);
            return;
        }

        List<TimeSlotDTO> teacherAvailableSlots = TimeSlotUtil.fromJsonString(teacher.getAvailableTimeSlots());
        if (teacherAvailableSlots == null || teacherAvailableSlots.isEmpty()) {
            log.info("教师可预约时间为空，允许所有时间预约，教师ID: {}", teacherId);
            return;
        }

        List<String> invalidCombinations = new ArrayList<>();

        // 验证每个星期几和时间段的组合
        for (Integer weekday : request.getRecurringWeekdays()) {
            for (String timeSlot : request.getRecurringTimeSlots()) {
                boolean isAvailable = teacherAvailableSlots.stream()
                        .anyMatch(slot -> weekday.equals(slot.getWeekday()) &&
                                 slot.getTimeSlots() != null &&
                                 slot.getTimeSlots().contains(timeSlot));

                if (!isAvailable) {
                    String weekdayName = getWeekdayName(weekday);
                    invalidCombinations.add(String.format("%s %s", weekdayName, timeSlot));
                }
            }
        }

        if (!invalidCombinations.isEmpty()) {
            throw new RuntimeException(String.format("以下时间段教师不可预约：%s，请重新选择时间",
                    String.join("、", invalidCombinations)));
        }

        log.info("周期性预约时间验证通过，教师ID: {}, 星期: {}, 时间段: {}",
                teacherId, request.getRecurringWeekdays(), request.getRecurringTimeSlots());
    }

    /**
     * 获取星期几的中文名称
     */
    private String getWeekdayName(int weekday) {
        String[] weekdays = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        return weekday >= 1 && weekday <= 7 ? weekdays[weekday] : "未知";
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
            return !requestedStart.isBefore(availableStart) && !requestedEnd.isAfter(availableEnd);
        } catch (Exception e) {
            log.error("时间段比较失败: requested={}, available={}", requestedTimeSlot, availableTimeSlot, e);
            return false;
        }
    }
}

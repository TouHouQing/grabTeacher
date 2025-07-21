package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.dto.*;
import com.touhouqing.grabteacherbackend.entity.*;
import com.touhouqing.grabteacherbackend.mapper.*;
import com.touhouqing.grabteacherbackend.service.BookingService;
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

    @Override
    @Transactional
    public BookingResponseDTO createBookingRequest(BookingRequestDTO request, Long studentUserId) {
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
                throw new RuntimeException("您已使用过免费试听课，无法再次申请");
            }
            // 试听课固定30分钟
            request.setTrialDurationMinutes(30);
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
                .isTrial(isTrial)
                .trialDurationMinutes(request.getTrialDurationMinutes())
                .status("pending")
                .build();

        bookingRequestMapper.insert(bookingRequest);

        log.info("预约申请创建成功，ID: {}", bookingRequest.getId());
        return convertToBookingResponseDTO(bookingRequest);
    }

    @Override
    @Transactional
    public BookingResponseDTO approveBookingRequest(Long bookingId, BookingApprovalDTO approval, Long teacherUserId) {
        log.info("教师审批预约申请，预约ID: {}, 教师用户ID: {}, 状态: {}", bookingId, teacherUserId, approval.getStatus());

        BookingRequest bookingRequest = bookingRequestMapper.selectById(bookingId);
        if (bookingRequest == null) {
            throw new RuntimeException("预约申请不存在");
        }

        // 验证教师权限
        Teacher teacher = teacherMapper.findByUserId(teacherUserId);
        if (teacher == null || !teacher.getId().equals(bookingRequest.getTeacherId())) {
            throw new RuntimeException("无权限操作此预约申请");
        }

        // 更新预约申请状态
        bookingRequest.setStatus(approval.getStatus());
        bookingRequest.setTeacherReply(approval.getTeacherReply());
        bookingRequest.setUpdatedAt(LocalDateTime.now());

        if ("approved".equals(approval.getStatus())) {
            bookingRequest.setApprovedAt(LocalDateTime.now());
            
            // 如果是试听课，标记用户已使用试听
            if (bookingRequest.getIsTrial() != null && bookingRequest.getIsTrial()) {
                Student student = studentMapper.selectById(bookingRequest.getStudentId());
                if (student != null) {
                    markTrialUsed(student.getUserId());
                }
            }
            
            // 生成课程安排
            if ("single".equals(bookingRequest.getBookingType())) {
                generateSingleSchedule(bookingRequest);
            } else if ("recurring".equals(bookingRequest.getBookingType())) {
                generateRecurringSchedules(bookingRequest);
            }
        }

        bookingRequestMapper.updateById(bookingRequest);

        log.info("预约申请审批完成，ID: {}, 状态: {}", bookingId, approval.getStatus());
        return convertToBookingResponseDTO(bookingRequest);
    }

    @Override
    public BookingResponseDTO cancelBookingRequest(Long bookingId, Long studentUserId) {
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

        bookingRequest.setStatus("cancelled");
        bookingRequest.setUpdatedAt(LocalDateTime.now());
        bookingRequestMapper.updateById(bookingRequest);

        log.info("预约申请取消成功，ID: {}", bookingId);
        return convertToBookingResponseDTO(bookingRequest);
    }

    @Override
    public BookingResponseDTO getBookingRequestById(Long bookingId) {
        BookingRequest bookingRequest = bookingRequestMapper.selectById(bookingId);
        if (bookingRequest == null) {
            throw new RuntimeException("预约申请不存在");
        }
        return convertToBookingResponseDTO(bookingRequest);
    }

    @Override
    public Page<BookingResponseDTO> getStudentBookingRequests(Long studentUserId, int page, int size, String status) {
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
        
        Page<BookingResponseDTO> responsePage = new Page<>(page, size);
        responsePage.setTotal(bookingPage.getTotal());
        responsePage.setRecords(bookingPage.getRecords().stream()
                .map(this::convertToBookingResponseDTO)
                .collect(Collectors.toList()));

        return responsePage;
    }

    @Override
    public Page<BookingResponseDTO> getTeacherBookingRequests(Long teacherUserId, int page, int size, String status) {
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
        
        Page<BookingResponseDTO> responsePage = new Page<>(page, size);
        responsePage.setTotal(bookingPage.getTotal());
        responsePage.setRecords(bookingPage.getRecords().stream()
                .map(this::convertToBookingResponseDTO)
                .collect(Collectors.toList()));

        return responsePage;
    }

    @Override
    public List<ScheduleResponseDTO> getTeacherSchedules(Long teacherUserId, LocalDate startDate, LocalDate endDate) {
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
    public List<ScheduleResponseDTO> getStudentSchedules(Long studentUserId, LocalDate startDate, LocalDate endDate) {
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
        log.info("生成周期性课程安排，预约申请ID: {}", bookingRequest.getId());

        if (!"recurring".equals(bookingRequest.getBookingType())) {
            throw new RuntimeException("只能为周期性预约生成课程安排");
        }

        List<Integer> weekdays = convertStringToIntegerList(bookingRequest.getRecurringWeekdays());
        List<String> timeSlots = convertStringToList(bookingRequest.getRecurringTimeSlots());

        if (weekdays.isEmpty() || timeSlots.isEmpty()) {
            throw new RuntimeException("周期性预约的星期和时间段不能为空");
        }

        int generatedCount = 0;
        LocalDate currentDate = bookingRequest.getStartDate();
        LocalDate endDate = bookingRequest.getEndDate();
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
                                    .isTrial(bookingRequest.getIsTrial())
                                    .sessionNumber(sessionNumber)
                                    .recurringWeekdays(bookingRequest.getRecurringWeekdays())
                                    .recurringTimeSlots(bookingRequest.getRecurringTimeSlots())
                                    .build();

                            scheduleMapper.insert(schedule);
                            generatedCount++;
                            sessionNumber++;

                            // 如果达到总次数限制，停止生成
                            if (bookingRequest.getTotalTimes() != null && sessionNumber > bookingRequest.getTotalTimes()) {
                                log.info("已达到总次数限制，停止生成课程安排");
                                return generatedCount;
                            }
                        }
                    }
                }
            }
            currentDate = currentDate.plusDays(1);
        }

        log.info("周期性课程安排生成完成，共生成 {} 个安排", generatedCount);
        return generatedCount;
    }

    @Override
    public Page<BookingResponseDTO> getAdminBookingRequests(int page, int size, String status, String keyword) {
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

        Page<BookingResponseDTO> responsePage = new Page<>(page, size);
        responsePage.setTotal(bookingPage.getTotal());
        responsePage.setRecords(bookingPage.getRecords().stream()
                .map(this::convertToBookingResponseDTO)
                .collect(Collectors.toList()));

        return responsePage;
    }

    @Override
    @Transactional
    public BookingResponseDTO adminApproveBookingRequest(Long bookingId, BookingApprovalDTO approval, Long adminUserId) {
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

            // 如果是试听课，标记用户已使用试听
            if (bookingRequest.getIsTrial() != null && bookingRequest.getIsTrial()) {
                Student student = studentMapper.selectById(bookingRequest.getStudentId());
                if (student != null) {
                    markTrialUsed(student.getUserId());
                }
            }

            // 生成课程安排
            if ("single".equals(bookingRequest.getBookingType())) {
                generateSingleSchedule(bookingRequest);
            } else if ("recurring".equals(bookingRequest.getBookingType())) {
                generateRecurringSchedules(bookingRequest);
            }
        }

        bookingRequestMapper.updateById(bookingRequest);

        log.info("管理员预约申请审批完成，ID: {}, 状态: {}", bookingId, approval.getStatus());
        return convertToBookingResponseDTO(bookingRequest);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 验证预约时间
     */
    private void validateBookingTime(BookingRequestDTO request, Long studentId, Long teacherId) {
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
        }
    }

    /**
     * 生成单次课程安排
     */
    private void generateSingleSchedule(BookingRequest bookingRequest) {
        log.info("生成单次课程安排，预约申请ID: {}", bookingRequest.getId());

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
                .isTrial(bookingRequest.getIsTrial())
                .sessionNumber(1)
                .build();

        scheduleMapper.insert(schedule);
        log.info("单次课程安排生成完成，安排ID: {}", schedule.getId());
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
    private BookingResponseDTO convertToBookingResponseDTO(BookingRequest bookingRequest) {
        // 获取学生信息
        Student student = studentMapper.selectById(bookingRequest.getStudentId());
        String studentName = student != null ? student.getRealName() : "未知学生";

        // 获取教师信息
        Teacher teacher = teacherMapper.selectById(bookingRequest.getTeacherId());
        String teacherName = teacher != null ? teacher.getRealName() : "未知教师";

        // 获取课程信息
        String courseTitle = null;
        if (bookingRequest.getCourseId() != null) {
            Course course = courseMapper.selectById(bookingRequest.getCourseId());
            courseTitle = course != null ? course.getTitle() : null;
        }

        return BookingResponseDTO.builder()
                .id(bookingRequest.getId())
                .studentId(bookingRequest.getStudentId())
                .studentName(studentName)
                .teacherId(bookingRequest.getTeacherId())
                .teacherName(teacherName)
                .courseId(bookingRequest.getCourseId())
                .courseTitle(courseTitle)
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
                .isTrial(bookingRequest.getIsTrial())
                .trialDurationMinutes(bookingRequest.getTrialDurationMinutes())
                .build();
    }

    /**
     * 将Schedule转换为ScheduleResponseDTO
     */
    private ScheduleResponseDTO convertToScheduleResponseDTO(Schedule schedule) {
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

        return ScheduleResponseDTO.builder()
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
                .isTrial(schedule.getIsTrial())
                .sessionNumber(schedule.getSessionNumber())
                .courseType(courseType)
                .build();
    }
}

package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.dto.TeacherInfoRequest;
import com.touhouqing.grabteacherbackend.dto.TeacherMatchRequest;
import com.touhouqing.grabteacherbackend.dto.TeacherMatchResponse;
import com.touhouqing.grabteacherbackend.dto.TeacherScheduleResponse;
import com.touhouqing.grabteacherbackend.dto.TimeSlotAvailability;
import com.touhouqing.grabteacherbackend.entity.Course;
import com.touhouqing.grabteacherbackend.entity.Schedule;
import com.touhouqing.grabteacherbackend.entity.Student;
import com.touhouqing.grabteacherbackend.entity.Subject;
import com.touhouqing.grabteacherbackend.entity.Teacher;
import com.touhouqing.grabteacherbackend.mapper.CourseMapper;
import com.touhouqing.grabteacherbackend.mapper.ScheduleMapper;
import com.touhouqing.grabteacherbackend.mapper.StudentMapper;
import com.touhouqing.grabteacherbackend.mapper.TeacherMapper;
import com.touhouqing.grabteacherbackend.service.SubjectService;
import com.touhouqing.grabteacherbackend.service.TeacherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {

    private final TeacherMapper teacherMapper;
    private final SubjectService subjectService;
    private final CourseMapper courseMapper;
    private final ScheduleMapper scheduleMapper;
    private final StudentMapper studentMapper;

    /**
     * 根据用户ID获取教师信息
     */
    @Override
    public Teacher getTeacherByUserId(Long userId) {
        QueryWrapper<Teacher> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("is_deleted", false);
        return teacherMapper.selectOne(queryWrapper);
    }

    /**
     * 根据ID获取教师信息
     */
    @Override
    public Teacher getTeacherById(Long teacherId) {
        QueryWrapper<Teacher> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", teacherId);
        queryWrapper.eq("is_deleted", false);
        return teacherMapper.selectOne(queryWrapper);
    }

    /**
     * 获取教师列表
     */
    @Override
    public List<Teacher> getTeacherList(int page, int size, String subject, String keyword) {
        Page<Teacher> pageParam = new Page<>(page, size);
        QueryWrapper<Teacher> queryWrapper = new QueryWrapper<>();
        
        queryWrapper.eq("is_deleted", false);
        queryWrapper.eq("is_verified", true); // 只显示已认证的教师
        
        if (StringUtils.hasText(subject)) {
            queryWrapper.like("subjects", subject);
        }
        
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like("real_name", keyword)
                    .or()
                    .like("specialties", keyword)
                    .or()
                    .like("introduction", keyword)
            );
        }
        
        queryWrapper.orderByDesc("created_at");
        
        Page<Teacher> result = teacherMapper.selectPage(pageParam, queryWrapper);
        return result.getRecords();
    }

    /**
     * 更新教师信息
     */
    @Override
    @Transactional
    public Teacher updateTeacherInfo(Long userId, TeacherInfoRequest request) {
        Teacher teacher = getTeacherByUserId(userId);
        if (teacher == null) {
            throw new RuntimeException("教师信息不存在");
        }

        // 更新教师信息
        if (request.getRealName() != null) {
            teacher.setRealName(request.getRealName());
        }
        if (request.getEducationBackground() != null) {
            teacher.setEducationBackground(request.getEducationBackground());
        }
        if (request.getTeachingExperience() != null) {
            teacher.setTeachingExperience(request.getTeachingExperience());
        }
        if (request.getSpecialties() != null) {
            teacher.setSpecialties(request.getSpecialties());
        }
        if (request.getSubjects() != null) {
            teacher.setSubjects(request.getSubjects());
        }
        if (request.getHourlyRate() != null) {
            teacher.setHourlyRate(request.getHourlyRate());
        }
        if (request.getIntroduction() != null) {
            teacher.setIntroduction(request.getIntroduction());
        }
        if (request.getVideoIntroUrl() != null) {
            teacher.setVideoIntroUrl(request.getVideoIntroUrl());
        }
        if (request.getGender() != null) {
            teacher.setGender(request.getGender());
        }

        teacherMapper.updateById(teacher);
        log.info("更新教师信息成功: userId={}", userId);

        return teacher;
    }

    /**
     * 匹配教师
     */
    @Override
    public List<TeacherMatchResponse> matchTeachers(TeacherMatchRequest request) {
        log.info("开始匹配教师，请求参数: {}", request);

        // 构建查询条件
        QueryWrapper<Teacher> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_deleted", false);
        queryWrapper.eq("is_verified", true); // 只匹配已认证的教师

        // 科目匹配 - 通过科目名称匹配
        if (StringUtils.hasText(request.getSubject())) {
            // 从科目表中查找科目名称，然后匹配教师的科目字段
            List<Subject> subjects = subjectService.getAllActiveSubjects();
            Subject matchedSubject = subjects.stream()
                    .filter(subject -> subject.getName().equals(request.getSubject()))
                    .findFirst()
                    .orElse(null);

            if (matchedSubject != null) {
                // 使用科目名称进行匹配
                queryWrapper.like("subjects", matchedSubject.getName());
            } else {
                // 如果科目不存在，返回空结果
                queryWrapper.eq("id", -1); // 强制返回空结果
            }
        }

        // 按教学经验排序
        queryWrapper.orderByDesc("teaching_experience");

        List<Teacher> teachers = teacherMapper.selectList(queryWrapper);

        // 如果指定了年级，进一步过滤
        if (StringUtils.hasText(request.getGrade())) {
            teachers = teachers.stream()
                    .filter(teacher -> hasMatchingGrade(teacher.getId(), request.getGrade()))
                    .collect(Collectors.toList());
        }

        // 转换为响应DTO并计算匹配分数
        List<TeacherMatchResponse> responses = teachers.stream()
                .map(teacher -> convertToMatchResponse(teacher, request))
                .sorted(Comparator.comparing(TeacherMatchResponse::getMatchScore).reversed())
                .limit(request.getLimit() != null ? request.getLimit() : 3)
                .collect(Collectors.toList());

        log.info("匹配到 {} 位教师", responses.size());
        return responses;
    }

    /**
     * 检查教师是否有匹配的年级课程
     */
    private boolean hasMatchingGrade(Long teacherId, String grade) {
        QueryWrapper<Course> courseQuery = new QueryWrapper<>();
        courseQuery.eq("teacher_id", teacherId);
        courseQuery.eq("is_deleted", false);
        courseQuery.eq("status", "active");
        courseQuery.like("grade", grade); // 修正：应该查询grade字段而不是gender字段

        List<Course> courses = courseMapper.selectList(courseQuery);
        return !courses.isEmpty();
    }

    /**
     * 将Teacher实体转换为TeacherMatchResponse并计算匹配分数
     */
    private TeacherMatchResponse convertToMatchResponse(Teacher teacher, TeacherMatchRequest request) {
        TeacherMatchResponse response = TeacherMatchResponse.builder()
                .id(teacher.getId())
                .name(teacher.getRealName())
                .subject(getFirstSubject(teacher.getSubjects()))
                .grade(getTeacherGrades(teacher.getId()))
                .experience(teacher.getTeachingExperience() != null ? teacher.getTeachingExperience() : 0)
                .description(teacher.getIntroduction())
                .avatar(null) // 当前Teacher实体没有avatar字段
                .tags(parseTagsToList(teacher.getSpecialties()))
                .schedule(Arrays.asList("周一 18:00-20:00", "周三 18:00-20:00", "周六 10:00-12:00"))
                .hourlyRate(teacher.getHourlyRate())
                .educationBackground(teacher.getEducationBackground())
                .specialties(teacher.getSpecialties())
                .isVerified(teacher.getIsVerified())
                .build();

        // 计算匹配分数
        int matchScore = calculateMatchScore(teacher, request);
        response.setMatchScore(matchScore);

        return response;
    }

    /**
     * 计算匹配分数
     */
    private int calculateMatchScore(Teacher teacher, TeacherMatchRequest request) {
        int score = 85; // 基础分数

        // 科目匹配加分
        if (StringUtils.hasText(request.getSubject()) &&
            StringUtils.hasText(teacher.getSubjects()) &&
            teacher.getSubjects().contains(request.getSubject())) {
            score += 10;
        }

        // 年级匹配加分
        if (StringUtils.hasText(request.getGrade()) &&
            hasMatchingGrade(teacher.getId(), request.getGrade())) {
            score += 8;
        }

        // 教学经验加分
        if (teacher.getTeachingExperience() != null) {
            score += Math.min(teacher.getTeachingExperience(), 10);
        }

        // 限制最高分为99
        return Math.min(score, 99);
    }

    /**
     * 获取第一个科目
     */
    private String getFirstSubject(String subjects) {
        if (!StringUtils.hasText(subjects)) {
            return "";
        }
        return subjects.split(",")[0].trim();
    }

    /**
     * 获取教师的年级信息（从课程表获取）
     */
    private String getTeacherGrades(Long teacherId) {
        QueryWrapper<Course> courseQuery = new QueryWrapper<>();
        courseQuery.eq("teacher_id", teacherId);
        courseQuery.eq("is_deleted", false);
        courseQuery.eq("status", "active");

        List<Course> courses = courseMapper.selectList(courseQuery);
        if (courses.isEmpty()) {
            return "";
        }

        // 获取第一个课程的年级信息
        return courses.get(0).getGrade(); // grade字段存储年级信息
    }

    /**
     * 解析标签字符串为列表
     */
    private List<String> parseTagsToList(String tags) {
        if (!StringUtils.hasText(tags)) {
            return new ArrayList<>();
        }
        return Arrays.asList(tags.split(","))
                .stream()
                .map(String::trim)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有可用的年级选项（从课程表获取）
     */
    @Override
    public List<String> getAvailableGrades() {
        // 从courses表中获取所有不同的年级（grade字段）
        QueryWrapper<Course> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("DISTINCT grade");
        queryWrapper.eq("is_deleted", false);
        queryWrapper.eq("status", "active");
        queryWrapper.isNotNull("grade");
        queryWrapper.ne("grade", "");

        List<Course> courses = courseMapper.selectList(queryWrapper);

        return courses.stream()
                .map(Course::getGrade)
                .filter(grade -> StringUtils.hasText(grade))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public TeacherScheduleResponse getTeacherPublicSchedule(Long teacherId, LocalDate startDate, LocalDate endDate) {
        // 获取教师信息
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            throw new RuntimeException("教师不存在");
        }

        // 获取指定时间范围内的课程安排
        List<Schedule> schedules = scheduleMapper.findByTeacherIdAndDateRange(teacherId, startDate, endDate);

        // 按日期分组
        Map<LocalDate, List<Schedule>> schedulesByDate = schedules.stream()
                .collect(Collectors.groupingBy(Schedule::getScheduledDate));

        // 生成每日课表
        List<TeacherScheduleResponse.DaySchedule> daySchedules = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            List<Schedule> daySchedules_raw = schedulesByDate.getOrDefault(currentDate, new ArrayList<>());

            // 生成该日的时间段信息
            List<TeacherScheduleResponse.TimeSlotInfo> timeSlots = generateTimeSlots(daySchedules_raw, currentDate);

            int availableCount = (int) timeSlots.stream().filter(TeacherScheduleResponse.TimeSlotInfo::getAvailable).count();
            int bookedCount = (int) timeSlots.stream().filter(TeacherScheduleResponse.TimeSlotInfo::getBooked).count();

            TeacherScheduleResponse.DaySchedule daySchedule = TeacherScheduleResponse.DaySchedule.builder()
                    .date(currentDate)
                    .dayOfWeek(currentDate.getDayOfWeek().getValue() % 7) // 转换为0=周日格式
                    .timeSlots(timeSlots)
                    .availableCount(availableCount)
                    .bookedCount(bookedCount)
                    .build();

            daySchedules.add(daySchedule);
            currentDate = currentDate.plusDays(1);
        }

        return TeacherScheduleResponse.builder()
                .teacherId(teacherId)
                .teacherName(teacher.getRealName())
                .startDate(startDate)
                .endDate(endDate)
                .daySchedules(daySchedules)
                .build();
    }

    @Override
    public List<TimeSlotAvailability> checkTeacherAvailability(Long teacherId, LocalDate startDate, LocalDate endDate, List<String> timeSlots) {
        // 如果没有指定时间段，使用默认时间段
        if (timeSlots == null || timeSlots.isEmpty()) {
            timeSlots = getDefaultTimeSlots();
        }

        List<TimeSlotAvailability> availabilities = new ArrayList<>();

        // 检查每个星期几和时间段的组合
        for (int weekday = 0; weekday <= 6; weekday++) { // 0=周日, 1=周一, ..., 6=周六
            for (String timeSlot : timeSlots) {
                TimeSlotAvailability availability = checkWeekdayTimeSlotAvailability(
                    teacherId, weekday, timeSlot, startDate, endDate);
                availabilities.add(availability);
            }
        }

        return availabilities;
    }

    /**
     * 生成时间段信息
     */
    private List<TeacherScheduleResponse.TimeSlotInfo> generateTimeSlots(List<Schedule> daySchedules, LocalDate date) {
        List<String> allTimeSlots = getDefaultTimeSlots();
        List<TeacherScheduleResponse.TimeSlotInfo> timeSlots = new ArrayList<>();

        // 将已有课程按时间段分组
        Map<String, Schedule> scheduleByTimeSlot = new HashMap<>();
        for (Schedule schedule : daySchedules) {
            String timeSlot = schedule.getStartTime().toString() + "-" + schedule.getEndTime().toString();
            scheduleByTimeSlot.put(timeSlot, schedule);
        }

        // 生成所有时间段的信息
        for (String timeSlot : allTimeSlots) {
            String[] times = timeSlot.split("-");
            LocalTime startTime = LocalTime.parse(times[0]);
            LocalTime endTime = LocalTime.parse(times[1]);

            Schedule existingSchedule = scheduleByTimeSlot.get(timeSlot);
            boolean isBooked = existingSchedule != null && !"cancelled".equals(existingSchedule.getStatus());

            TeacherScheduleResponse.TimeSlotInfo timeSlotInfo = TeacherScheduleResponse.TimeSlotInfo.builder()
                    .timeSlot(timeSlot)
                    .startTime(startTime)
                    .endTime(endTime)
                    .available(!isBooked)
                    .booked(isBooked)
                    .build();

            if (isBooked) {
                // 获取学生信息
                Student student = studentMapper.selectById(existingSchedule.getStudentId());
                if (student != null) {
                    timeSlotInfo.setStudentName(student.getRealName());
                }

                // 获取课程信息
                Course course = courseMapper.selectById(existingSchedule.getCourseId());
                if (course != null) {
                    timeSlotInfo.setCourseTitle(course.getTitle());
                }

                timeSlotInfo.setStatus(existingSchedule.getStatus());
                timeSlotInfo.setIsTrial(existingSchedule.getIsTrial());
            }

            timeSlots.add(timeSlotInfo);
        }

        return timeSlots;
    }

    /**
     * 检查特定星期几和时间段的可用性
     */
    private TimeSlotAvailability checkWeekdayTimeSlotAvailability(Long teacherId, int weekday, String timeSlot, LocalDate startDate, LocalDate endDate) {
        String[] times = timeSlot.split("-");
        LocalTime startTime = LocalTime.parse(times[0]);
        LocalTime endTime = LocalTime.parse(times[1]);

        List<String> conflictDates = new ArrayList<>();
        LocalDate currentDate = startDate;
        int totalWeeks = 0;
        int conflictWeeks = 0;

        // 检查指定时间范围内每周该星期几的情况
        while (!currentDate.isAfter(endDate)) {
            int currentWeekday = currentDate.getDayOfWeek().getValue() % 7; // 转换为0=周日格式

            if (currentWeekday == weekday) {
                totalWeeks++;

                // 检查该日期该时间段是否有冲突
                int conflictCount = scheduleMapper.countConflictingSchedules(teacherId, currentDate, startTime, endTime);
                if (conflictCount > 0) {
                    conflictWeeks++;
                    conflictDates.add(currentDate.toString());
                }
            }

            currentDate = currentDate.plusDays(1);
        }

        double availabilityRate = totalWeeks > 0 ? (double) (totalWeeks - conflictWeeks) / totalWeeks : 1.0;
        boolean available = availabilityRate > 0.7; // 70%以上可用才认为可选

        return TimeSlotAvailability.builder()
                .weekday(weekday)
                .timeSlot(timeSlot)
                .available(available)
                .availabilityRate(availabilityRate)
                .conflictDates(conflictDates)
                .conflictReason(conflictDates.isEmpty() ? null : "已有其他学生预约")
                .description(String.format("该时间段在查询范围内有%d个日期冲突，可用率%.1f%%",
                    conflictDates.size(), availabilityRate * 100))
                .build();
    }

    /**
     * 获取默认时间段列表
     */
    private List<String> getDefaultTimeSlots() {
        return Arrays.asList(
            "09:00-10:00", "10:00-11:00", "11:00-12:00",
            "14:00-15:00", "15:00-16:00", "16:00-17:00", "17:00-18:00",
            "18:00-19:00", "19:00-20:00", "20:00-21:00"
        );
    }
}
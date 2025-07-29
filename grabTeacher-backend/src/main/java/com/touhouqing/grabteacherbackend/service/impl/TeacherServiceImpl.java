package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.dto.TeacherDetailResponse;
import com.touhouqing.grabteacherbackend.dto.TeacherInfoRequest;
import com.touhouqing.grabteacherbackend.dto.TeacherListResponse;
import com.touhouqing.grabteacherbackend.dto.TeacherMatchRequest;
import com.touhouqing.grabteacherbackend.dto.TeacherMatchResponse;
import com.touhouqing.grabteacherbackend.dto.TeacherProfileResponse;
import com.touhouqing.grabteacherbackend.dto.TeacherScheduleResponse;
import com.touhouqing.grabteacherbackend.dto.TimeSlotAvailability;
import com.touhouqing.grabteacherbackend.dto.TimeSlotDTO;
import com.touhouqing.grabteacherbackend.entity.Course;
import com.touhouqing.grabteacherbackend.entity.CourseGrade;
import com.touhouqing.grabteacherbackend.entity.Schedule;
import com.touhouqing.grabteacherbackend.entity.Student;
import com.touhouqing.grabteacherbackend.entity.Subject;
import com.touhouqing.grabteacherbackend.entity.Teacher;
import com.touhouqing.grabteacherbackend.entity.TeacherSubject;
import com.touhouqing.grabteacherbackend.entity.User;
import com.touhouqing.grabteacherbackend.entity.BookingRequest;
import com.touhouqing.grabteacherbackend.entity.RescheduleRequest;
import com.touhouqing.grabteacherbackend.mapper.CourseMapper;
import com.touhouqing.grabteacherbackend.mapper.CourseGradeMapper;
import com.touhouqing.grabteacherbackend.mapper.ScheduleMapper;
import com.touhouqing.grabteacherbackend.mapper.StudentMapper;
import com.touhouqing.grabteacherbackend.mapper.TeacherMapper;
import com.touhouqing.grabteacherbackend.mapper.TeacherSubjectMapper;
import com.touhouqing.grabteacherbackend.mapper.UserMapper;
import com.touhouqing.grabteacherbackend.mapper.BookingRequestMapper;
import com.touhouqing.grabteacherbackend.mapper.RescheduleRequestMapper;
import com.touhouqing.grabteacherbackend.service.SubjectService;
import com.touhouqing.grabteacherbackend.service.TeacherService;
import com.touhouqing.grabteacherbackend.dto.TimeSlotDTO;
import com.touhouqing.grabteacherbackend.util.TimeSlotUtil;
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
    private final TeacherSubjectMapper teacherSubjectMapper;
    private final SubjectService subjectService;
    private final CourseMapper courseMapper;
    private final CourseGradeMapper courseGradeMapper;
    private final ScheduleMapper scheduleMapper;
    private final StudentMapper studentMapper;
    private final UserMapper userMapper;
    private final BookingRequestMapper bookingRequestMapper;
    private final RescheduleRequestMapper rescheduleRequestMapper;

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
     * 根据ID获取教师详情（包含用户信息、科目信息、年级信息）
     */
    @Override
    public TeacherDetailResponse getTeacherDetailById(Long teacherId) {
        // 获取教师基本信息
        Teacher teacher = getTeacherById(teacherId);
        if (teacher == null) {
            return null;
        }

        // 获取用户信息
        User user = userMapper.selectById(teacher.getUserId());
        if (user == null) {
            return null;
        }

        // 获取教师的科目ID列表
        List<Long> subjectIds = teacherSubjectMapper.getSubjectIdsByTeacherId(teacher.getId());

        // 获取科目名称列表
        List<String> subjects = new ArrayList<>();
        for (Long subjectId : subjectIds) {
            try {
                Subject subject = subjectService.getSubjectById(subjectId);
                if (subject != null) {
                    subjects.add(subject.getName());
                }
            } catch (Exception e) {
                log.warn("获取科目信息失败，科目ID: {}", subjectId, e);
            }
        }

        // 获取年级信息
        List<String> grades = getTeacherGradesList(teacher.getId());

        // 解析可上课时间
        List<TimeSlotDTO> availableTimeSlots = null;
        if (teacher.getAvailableTimeSlots() != null) {
            availableTimeSlots = TimeSlotUtil.fromJsonString(teacher.getAvailableTimeSlots());
        }

        return TeacherDetailResponse.builder()
                .id(teacher.getId())
                .userId(teacher.getUserId())
                .realName(teacher.getRealName())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .educationBackground(teacher.getEducationBackground())
                .teachingExperience(teacher.getTeachingExperience())
                .specialties(teacher.getSpecialties())
                .subjects(subjects)
                .grades(grades)
                .hourlyRate(teacher.getHourlyRate())
                .introduction(teacher.getIntroduction())
                .videoIntroUrl(teacher.getVideoIntroUrl())
                .gender(teacher.getGender())
                .availableTimeSlots(availableTimeSlots)
                .isVerified(teacher.getIsVerified())
                .isDeleted(teacher.getIsDeleted())
                .deletedAt(teacher.getDeletedAt())
                .build();
    }

    /**
     * 根据用户ID获取教师详细信息（包含科目信息）
     */
    public TeacherProfileResponse getTeacherProfileByUserId(Long userId) {
        Teacher teacher = getTeacherByUserId(userId);
        if (teacher == null) {
            return null;
        }

        // 获取教师的科目ID列表
        List<Long> subjectIds = teacherSubjectMapper.getSubjectIdsByTeacherId(teacher.getId());

        // 解析可上课时间
        List<TimeSlotDTO> availableTimeSlots = null;
        if (teacher.getAvailableTimeSlots() != null) {
            availableTimeSlots = TimeSlotUtil.fromJsonString(teacher.getAvailableTimeSlots());
        }

        return TeacherProfileResponse.builder()
                .id(teacher.getId())
                .userId(teacher.getUserId())
                .realName(teacher.getRealName())
                .educationBackground(teacher.getEducationBackground())
                .teachingExperience(teacher.getTeachingExperience())
                .specialties(teacher.getSpecialties())
                .subjectIds(subjectIds)
                .hourlyRate(teacher.getHourlyRate())
                .introduction(teacher.getIntroduction())
                .videoIntroUrl(teacher.getVideoIntroUrl())
                .gender(teacher.getGender())
                .availableTimeSlots(availableTimeSlots)
                .isVerified(teacher.getIsVerified())
                .isDeleted(teacher.getIsDeleted())
                .deletedAt(teacher.getDeletedAt())
                .build();
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
        
        queryWrapper.orderByDesc("id");
        
        Page<Teacher> result = teacherMapper.selectPage(pageParam, queryWrapper);
        return result.getRecords();
    }

    /**
     * 获取教师列表（包含科目信息）
     */
    @Override
    public List<TeacherListResponse> getTeacherListWithSubjects(int page, int size, String subject, String grade, String keyword) {
        // 获取教师列表，支持科目和年级筛选
        List<Teacher> teachers = getFilteredTeacherList(page, size, subject, grade, keyword);

        // 转换为包含科目信息的响应DTO
        List<TeacherListResponse> responseList = new ArrayList<>();
        for (Teacher teacher : teachers) {
            // 获取用户信息
            User user = userMapper.selectById(teacher.getUserId());

            // 获取教师的科目ID列表
            List<Long> subjectIds = teacherSubjectMapper.getSubjectIdsByTeacherId(teacher.getId());

            // 获取科目名称列表
            List<String> subjects = new ArrayList<>();
            for (Long subjectId : subjectIds) {
                try {
                    Subject subjectEntity = subjectService.getSubjectById(subjectId);
                    if (subjectEntity != null) {
                        subjects.add(subjectEntity.getName());
                    }
                } catch (Exception e) {
                    log.warn("获取科目信息失败，科目ID: {}", subjectId, e);
                }
            }

            // 获取年级信息
            List<String> grades = getTeacherGradesList(teacher.getId());

            TeacherListResponse response = TeacherListResponse.builder()
                    .id(teacher.getId())
                    .realName(teacher.getRealName())
                    .avatarUrl(user != null ? user.getAvatarUrl() : null)
                    .educationBackground(teacher.getEducationBackground())
                    .teachingExperience(teacher.getTeachingExperience())
                    .specialties(teacher.getSpecialties())
                    .subjects(subjects)
                    .grades(grades)
                    .hourlyRate(teacher.getHourlyRate())
                    .introduction(teacher.getIntroduction())
                    .gender(teacher.getGender())
                    .isVerified(teacher.getIsVerified())
                    .build();

            responseList.add(response);
        }

        return responseList;
    }

    /**
     * 更新教师信息（教师自己更新，不能修改科目和收费）
     */
    @Override
    @Transactional
    public Teacher updateTeacherInfo(Long userId, TeacherInfoRequest request) {
        Teacher teacher = getTeacherByUserId(userId);
        if (teacher == null) {
            throw new RuntimeException("教师信息不存在");
        }

        // 教师只能更新基本信息，不能修改科目和收费
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
        // 注意：教师不能修改 hourlyRate 和 subjectIds
        if (request.getIntroduction() != null) {
            teacher.setIntroduction(request.getIntroduction());
        }
        if (request.getVideoIntroUrl() != null) {
            teacher.setVideoIntroUrl(request.getVideoIntroUrl());
        }
        if (request.getGender() != null) {
            teacher.setGender(request.getGender());
        }

        // 处理可上课时间更新
        if (request.getAvailableTimeSlots() != null) {
            if (request.getAvailableTimeSlots().isEmpty()) {
                // 如果传入空列表，表示清空可上课时间，设置为null（表示所有时间都可以）
                teacher.setAvailableTimeSlots(null);
                log.info("教师清空可上课时间设置，默认所有时间可用: userId={}", userId);
            } else {
                // 验证时间安排格式
                if (TimeSlotUtil.isValidTimeSlots(request.getAvailableTimeSlots())) {
                    String timeSlotsJson = TimeSlotUtil.toJsonString(request.getAvailableTimeSlots());
                    teacher.setAvailableTimeSlots(timeSlotsJson);
                    log.info("教师更新可上课时间成功: userId={}, totalSlots={}", userId,
                            request.getAvailableTimeSlots().stream()
                                    .mapToInt(slot -> slot.getTimeSlots() != null ? slot.getTimeSlots().size() : 0)
                                    .sum());
                } else {
                    throw new RuntimeException("可上课时间格式不正确");
                }
            }
        }

        teacherMapper.updateById(teacher);
        log.info("教师更新个人信息成功: userId={}", userId);

        return teacher;
    }

    /**
     * 匹配教师 - 优化版本，减少数据库查询次数
     */
    @Override
    public List<TeacherMatchResponse> matchTeachers(TeacherMatchRequest request) {
        log.info("开始匹配教师，请求参数: {}", request);

        // 转换前端星期几格式为后端格式
        if (request.getPreferredWeekdays() != null && !request.getPreferredWeekdays().isEmpty()) {
            List<Integer> convertedWeekdays = request.getPreferredWeekdays().stream()
                .map(TimeSlotUtil::convertFrontendWeekdayToBackend)
                .collect(Collectors.toList());
            request.setPreferredWeekdays(convertedWeekdays);
            log.info("转换后的星期几偏好: {}", convertedWeekdays);
        }

        // 使用优化的查询方法
        List<Teacher> teachers = matchTeachersOptimized(request);

        // 转换为响应DTO并计算匹配分数
        List<TeacherMatchResponse> responses = teachers.stream()
                .filter(teacher -> matchesGenderPreference(teacher, request)) // 添加性别过滤
                .map(teacher -> convertToMatchResponse(teacher, request))
                .sorted(Comparator.comparing(TeacherMatchResponse::getMatchScore).reversed())
                .limit(request.getLimit() != null ? request.getLimit() : 3)
                .collect(Collectors.toList());

        log.info("匹配到 {} 位教师", responses.size());
        return responses;
    }

    /**
     * 优化的教师匹配查询 - 使用JOIN减少数据库查询次数
     */
    private List<Teacher> matchTeachersOptimized(TeacherMatchRequest request) {
        // 如果同时指定了科目和年级，使用联合查询
        if (StringUtils.hasText(request.getSubject()) && StringUtils.hasText(request.getGrade())) {
            return teacherMapper.findTeachersBySubjectAndGrade(request.getSubject(), request.getGrade());
        }

        // 如果只指定了科目
        if (StringUtils.hasText(request.getSubject())) {
            return teacherMapper.findTeachersBySubject(request.getSubject());
        }

        // 如果只指定了年级
        if (StringUtils.hasText(request.getGrade())) {
            return teacherMapper.findTeachersByGrade(request.getGrade());
        }

        // 如果都没指定，返回所有已认证的教师
        QueryWrapper<Teacher> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_deleted", false);
        queryWrapper.eq("is_verified", true);
        queryWrapper.orderByDesc("teaching_experience");
        return teacherMapper.selectList(queryWrapper);
    }

    /**
     * 检查教师是否有匹配的年级课程
     */
    private boolean hasMatchingGrade(Long teacherId, String grade) {
        // 首先获取教师的所有课程
        QueryWrapper<Course> courseQuery = new QueryWrapper<>();
        courseQuery.eq("teacher_id", teacherId);
        courseQuery.eq("is_deleted", false);
        courseQuery.eq("status", "active");

        List<Course> courses = courseMapper.selectList(courseQuery);
        if (courses.isEmpty()) {
            return false;
        }

        // 检查这些课程是否有匹配的年级
        for (Course course : courses) {
            QueryWrapper<CourseGrade> gradeQuery = new QueryWrapper<>();
            gradeQuery.eq("course_id", course.getId());
            gradeQuery.eq("grade", grade);

            List<CourseGrade> courseGrades = courseGradeMapper.selectList(gradeQuery);
            if (!courseGrades.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    /**
     * 将Teacher实体转换为TeacherMatchResponse并计算匹配分数
     */
    private TeacherMatchResponse convertToMatchResponse(Teacher teacher, TeacherMatchRequest request) {
        // 解析教师的可上课时间
        List<TimeSlotDTO> availableTimeSlots = TimeSlotUtil.fromJsonString(teacher.getAvailableTimeSlots());

        // 生成可读的时间安排描述
        List<String> scheduleDescriptions = generateScheduleDescriptions(availableTimeSlots);

        TeacherMatchResponse response = TeacherMatchResponse.builder()
                .id(teacher.getId())
                .name(teacher.getRealName())
                .subject(getFirstSubject(teacher.getId()))
                .grade(getTeacherGrades(teacher.getId()))
                .experience(teacher.getTeachingExperience() != null ? teacher.getTeachingExperience() : 0)
                .description(teacher.getIntroduction())
                .avatar(null) // 当前Teacher实体没有avatar字段
                .tags(parseTagsToList(teacher.getSpecialties()))
                .schedule(scheduleDescriptions)
                .hourlyRate(teacher.getHourlyRate())
                .educationBackground(teacher.getEducationBackground())
                .specialties(teacher.getSpecialties())
                .isVerified(teacher.getIsVerified())
                .gender(teacher.getGender())
                .availableTimeSlots(availableTimeSlots)
                .build();

        // 计算匹配分数和时间匹配度
        int matchScore = calculateMatchScore(teacher, request);
        int timeMatchScore = calculateTimeMatchScore(availableTimeSlots, request);

        response.setMatchScore(matchScore);
        response.setTimeMatchScore(timeMatchScore);

        return response;
    }

    /**
     * 计算匹配分数
     */
    private int calculateMatchScore(Teacher teacher, TeacherMatchRequest request) {
        int score = 85; // 基础分数

        // 科目匹配加分
        if (StringUtils.hasText(request.getSubject())) {
            List<Long> subjectIds = teacherSubjectMapper.getSubjectIdsByTeacherId(teacher.getId());
            for (Long subjectId : subjectIds) {
                try {
                    String subjectName = subjectService.getSubjectById(subjectId).getName();
                    if (request.getSubject().equals(subjectName)) {
                        score += 10;
                        break;
                    }
                } catch (Exception e) {
                    // 忽略异常，继续检查下一个科目
                }
            }
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

        // 时间匹配加分
        List<TimeSlotDTO> availableTimeSlots = TimeSlotUtil.fromJsonString(teacher.getAvailableTimeSlots());
        int timeMatchScore = calculateTimeMatchScore(availableTimeSlots, request);
        score += (timeMatchScore / 10); // 时间匹配度转换为分数加成

        // 限制最高分为99
        return Math.min(score, 99);
    }

    /**
     * 获取教师的第一个科目名称
     */
    private String getFirstSubject(Long teacherId) {
        List<Long> subjectIds = teacherSubjectMapper.getSubjectIdsByTeacherId(teacherId);
        if (subjectIds.isEmpty()) {
            return "";
        }
        // 获取第一个科目的名称
        try {
            return subjectService.getSubjectById(subjectIds.get(0)).getName();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 获取教师的年级信息（从课程年级关联表获取）
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
        List<CourseGrade> courseGrades = courseGradeMapper.findByCourseId(courses.get(0).getId());
        if (courseGrades.isEmpty()) {
            return "";
        }

        return courseGrades.stream()
                .map(CourseGrade::getGrade)
                .collect(Collectors.joining(","));
    }

    /**
     * 获取教师的年级信息列表（从课程年级关联表获取）
     */
    private List<String> getTeacherGradesList(Long teacherId) {
        QueryWrapper<Course> courseQuery = new QueryWrapper<>();
        courseQuery.eq("teacher_id", teacherId);
        courseQuery.eq("is_deleted", false);
        courseQuery.eq("status", "active");

        List<Course> courses = courseMapper.selectList(courseQuery);
        if (courses.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取所有课程的年级信息并去重
        Set<String> gradeSet = new HashSet<>();
        for (Course course : courses) {
            List<CourseGrade> courseGrades = courseGradeMapper.findByCourseId(course.getId());
            for (CourseGrade courseGrade : courseGrades) {
                gradeSet.add(courseGrade.getGrade());
            }
        }

        return new ArrayList<>(gradeSet);
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
     * 获取所有可用的年级选项（从课程年级关联表获取）
     */
    @Override
    public List<String> getAvailableGrades() {
        // 从course_grades表中获取所有不同的年级
        List<String> grades = courseGradeMapper.findAllDistinctGrades();

        // 收集年级数据
        Set<String> gradeSet = new HashSet<>();
        for (String grade : grades) {
            if (StringUtils.hasText(grade)) {
                gradeSet.add(grade.trim());
            }
        }

        // 转换为列表并排序
        List<String> result = new ArrayList<>(gradeSet);

        // 自定义排序：按学段和年级排序
        result.sort((g1, g2) -> {
            // 定义年级顺序
            String[] gradeOrder = {
                "小学一年级", "小学二年级", "小学三年级", "小学四年级", "小学五年级", "小学六年级",
                "初中一年级", "初中二年级", "初中三年级",
                "高中一年级", "高中二年级", "高中三年级"
            };

            int index1 = Arrays.asList(gradeOrder).indexOf(g1);
            int index2 = Arrays.asList(gradeOrder).indexOf(g2);

            // 如果都在预定义列表中，按顺序排序
            if (index1 != -1 && index2 != -1) {
                return Integer.compare(index1, index2);
            }
            // 如果只有一个在预定义列表中，预定义的排在前面
            if (index1 != -1) return -1;
            if (index2 != -1) return 1;
            // 如果都不在预定义列表中，按字典序排序
            return g1.compareTo(g2);
        });

        return result;
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
            List<TeacherScheduleResponse.TimeSlotInfo> timeSlots = generateTimeSlots(teacherId, daySchedules_raw, currentDate);

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
        List<TimeSlotAvailability> availabilities = new ArrayList<>();

        // 获取教师设置的可上课时间
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            return availabilities;
        }

        List<TimeSlotDTO> teacherAvailableSlots = TimeSlotUtil.fromJsonString(teacher.getAvailableTimeSlots());

        // 如果教师没有设置可上课时间，返回所有时间段为不可用
        if (teacherAvailableSlots.isEmpty()) {
            // 如果没有指定时间段，使用默认时间段
            if (timeSlots == null || timeSlots.isEmpty()) {
                timeSlots = getDefaultTimeSlots();
            }

            // 所有时间段都标记为不可用，因为教师没有设置可上课时间
            for (int weekday = 1; weekday <= 7; weekday++) {
                for (String timeSlot : timeSlots) {
                    TimeSlotAvailability availability = TimeSlotAvailability.builder()
                            .weekday(TimeSlotUtil.convertBackendWeekdayToFrontend(weekday))
                            .timeSlot(timeSlot)
                            .available(false)
                            .availabilityRate(0.0)
                            .conflictDates(new ArrayList<>())
                            .conflictReason("教师未设置可上课时间")
                            .description("教师未设置该时间段为可上课时间")
                            .build();
                    availabilities.add(availability);
                }
            }
        } else {
            // 只检查教师设置的可上课时间段
            for (TimeSlotDTO teacherSlot : teacherAvailableSlots) {
                int weekday = teacherSlot.getWeekday();
                List<String> slotTimes = teacherSlot.getTimeSlots();

                if (slotTimes != null) {
                    for (String timeSlot : slotTimes) {
                        TimeSlotAvailability availability = checkWeekdayTimeSlotAvailability(
                            teacherId, weekday, timeSlot, startDate, endDate);
                        // 转换为前端格式返回
                        availability.setWeekday(TimeSlotUtil.convertBackendWeekdayToFrontend(weekday));
                        availabilities.add(availability);
                    }
                }
            }
        }

        return availabilities;
    }

    /**
     * 生成时间段信息 - 根据教师设置的可上课时间和实际课表安排
     */
    private List<TeacherScheduleResponse.TimeSlotInfo> generateTimeSlots(Long teacherId, List<Schedule> daySchedules, LocalDate date) {
        // 获取该日期对应的星期几（ISO标准：1=周一, 7=周日）
        int weekday = date.getDayOfWeek().getValue();

        // 获取教师设置的可上课时间段
        List<String> availableTimeSlots = getTeacherAvailableTimeSlotsForWeekday(teacherId, weekday);

        // 如果教师没有设置可上课时间，返回空列表（不显示任何时间段）
        if (availableTimeSlots.isEmpty()) {
            return new ArrayList<>();
        }

        List<TeacherScheduleResponse.TimeSlotInfo> timeSlots = new ArrayList<>();

        // 将已有课程按时间段分组
        Map<String, Schedule> scheduleByTimeSlot = new HashMap<>();
        for (Schedule schedule : daySchedules) {
            String timeSlot = schedule.getStartTime().toString() + "-" + schedule.getEndTime().toString();
            scheduleByTimeSlot.put(timeSlot, schedule);
        }

        // 只生成教师可上课时间段的信息
        for (String timeSlot : availableTimeSlots) {
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

        // 首先检查教师是否设置了该时间段为可上课时间
        List<String> teacherAvailableSlots = getTeacherAvailableTimeSlotsForWeekday(teacherId, weekday);
        boolean isTeacherAvailable = teacherAvailableSlots.isEmpty() || teacherAvailableSlots.contains(timeSlot);

        // 如果教师没有设置该时间段为可上课时间，直接返回不可用
        if (!isTeacherAvailable) {
            return TimeSlotAvailability.builder()
                    .weekday(weekday)
                    .timeSlot(timeSlot)
                    .available(false)
                    .availabilityRate(0.0)
                    .conflictDates(new ArrayList<>())
                    .conflictReason("教师未设置该时间段为可上课时间")
                    .description("教师未在该时间段设置为可上课时间")
                    .build();
        }

        List<String> conflictDates = new ArrayList<>();
        LocalDate currentDate = startDate;
        int totalWeeks = 0;
        int conflictWeeks = 0;

        // 检查指定时间范围内每周该星期几的情况
        while (!currentDate.isAfter(endDate)) {
            // 使用ISO标准：1=周一, 2=周二, ..., 7=周日
            int currentWeekday = currentDate.getDayOfWeek().getValue();
            if (currentWeekday == 7) {
                currentWeekday = 7; // 周日保持为7
            }

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
        boolean available = availabilityRate >= 0.5; // 50%以上可用才认为可选（降低阈值）
        String availabilityLevel = calculateAvailabilityLevel(availabilityRate);

        return TimeSlotAvailability.builder()
                .weekday(weekday)
                .timeSlot(timeSlot)
                .available(available)
                .availabilityRate(availabilityRate)
                .conflictDates(conflictDates)
                .conflictReason(conflictDates.isEmpty() ? null : "已有其他学生预约")
                .description(String.format("该时间段在查询范围内有%d个日期冲突，可用率%.1f%%（%s）",
                    conflictDates.size(), availabilityRate * 100, availabilityLevel))
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

    /**
     * 获取教师在指定星期几的可上课时间段
     * @param teacherId 教师ID
     * @param weekday 星期几（ISO标准：1=周一, 7=周日）
     * @return 该星期几的可上课时间段列表
     */
    private List<String> getTeacherAvailableTimeSlotsForWeekday(Long teacherId, int weekday) {
        if (teacherId == null) {
            return new ArrayList<>();
        }

        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null || teacher.getAvailableTimeSlots() == null) {
            return new ArrayList<>();
        }

        // 解析教师的可上课时间设置
        List<TimeSlotDTO> availableTimeSlots = TimeSlotUtil.fromJsonString(teacher.getAvailableTimeSlots());

        // 查找指定星期几的时间段
        for (TimeSlotDTO timeSlotDTO : availableTimeSlots) {
            if (timeSlotDTO.getWeekday() != null && timeSlotDTO.getWeekday() == weekday) {
                return timeSlotDTO.getTimeSlots() != null ? timeSlotDTO.getTimeSlots() : new ArrayList<>();
            }
        }

        return new ArrayList<>();
    }

    /**
     * 生成可读的时间安排描述
     */
    private List<String> generateScheduleDescriptions(List<TimeSlotDTO> availableTimeSlots) {
        List<String> descriptions = new ArrayList<>();

        if (availableTimeSlots == null || availableTimeSlots.isEmpty()) {
            return Arrays.asList("暂无可用时间");
        }

        String[] weekdayNames = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};

        for (TimeSlotDTO timeSlot : availableTimeSlots) {
            if (timeSlot.getTimeSlots() != null && !timeSlot.getTimeSlots().isEmpty()) {
                String weekdayName = timeSlot.getWeekday() != null &&
                                   timeSlot.getWeekday() >= 1 && timeSlot.getWeekday() <= 7
                                   ? weekdayNames[timeSlot.getWeekday()] : "未知";

                for (String slot : timeSlot.getTimeSlots()) {
                    descriptions.add(weekdayName + " " + slot);
                }
            }
        }

        return descriptions.isEmpty() ? Arrays.asList("暂无可用时间") : descriptions;
    }

    /**
     * 计算时间匹配度 - 改进版本
     */
    private int calculateTimeMatchScore(List<TimeSlotDTO> availableTimeSlots, TeacherMatchRequest request) {
        if (availableTimeSlots == null || availableTimeSlots.isEmpty()) {
            return 0;
        }

        // 如果学生没有时间偏好，给予基础分数
        if ((request.getPreferredWeekdays() == null || request.getPreferredWeekdays().isEmpty()) &&
            (request.getPreferredTimeSlots() == null || request.getPreferredTimeSlots().isEmpty())) {
            return 50;
        }

        int totalScore = 0;
        int maxPossibleScore = 0;

        // 处理时间段匹配
        if (request.getPreferredTimeSlots() != null && !request.getPreferredTimeSlots().isEmpty()) {
            for (String preferredTimeSlot : request.getPreferredTimeSlots()) {
                maxPossibleScore += 100; // 每个偏好时间段最高100分

                int bestMatchScore = 0;

                for (TimeSlotDTO teacherSlot : availableTimeSlots) {
                    if (teacherSlot.getTimeSlots() != null) {
                        for (String teacherTimeSlot : teacherSlot.getTimeSlots()) {
                            // 计算时间重叠度
                            int overlapScore = TimeSlotUtil.calculateTimeOverlapScore(preferredTimeSlot, teacherTimeSlot);

                            // 检查星期几是否匹配
                            if (request.getPreferredWeekdays() != null &&
                                request.getPreferredWeekdays().contains(teacherSlot.getWeekday())) {
                                overlapScore = Math.min(100, overlapScore + 20); // 星期几匹配加分，但不超过100
                            }

                            bestMatchScore = Math.max(bestMatchScore, overlapScore);
                        }
                    }
                }
                totalScore += bestMatchScore;
            }
        } else if (request.getPreferredWeekdays() != null && !request.getPreferredWeekdays().isEmpty()) {
            // 只有星期几偏好，没有具体时间段偏好
            for (Integer preferredWeekday : request.getPreferredWeekdays()) {
                maxPossibleScore += 100;

                boolean hasMatchingWeekday = availableTimeSlots.stream()
                    .anyMatch(slot -> preferredWeekday.equals(slot.getWeekday()) &&
                             slot.getTimeSlots() != null && !slot.getTimeSlots().isEmpty());

                if (hasMatchingWeekday) {
                    totalScore += 80; // 星期几匹配得80分
                }
            }
        }

        return maxPossibleScore > 0 ? Math.min((totalScore * 100) / maxPossibleScore, 100) : 50;
    }

    /**
     * 计算教师时间可用性等级
     */
    private String calculateAvailabilityLevel(double availabilityRate) {
        if (availabilityRate >= 0.9) {
            return "高可用";
        } else if (availabilityRate >= 0.7) {
            return "中等可用";
        } else if (availabilityRate >= 0.5) {
            return "低可用";
        } else {
            return "基本不可用";
        }
    }

    /**
     * 检查教师是否匹配性别偏好
     */
    private boolean matchesGenderPreference(Teacher teacher, TeacherMatchRequest request) {
        // 如果没有性别偏好，则不过滤
        if (request.getPreferredGender() == null || request.getPreferredGender().trim().isEmpty()) {
            return true;
        }

        // 检查教师性别是否匹配偏好
        String teacherGender = teacher.getGender();
        if (teacherGender == null) {
            return true; // 如果教师性别未设置，则不过滤
        }

        // 性别匹配逻辑（支持中英文）
        String preferredGender = request.getPreferredGender().toLowerCase();
        String actualGender = teacherGender.toLowerCase();

        return preferredGender.equals(actualGender) ||
               (preferredGender.equals("male") && actualGender.equals("男")) ||
               (preferredGender.equals("female") && actualGender.equals("女")) ||
               (preferredGender.equals("男") && actualGender.equals("male")) ||
               (preferredGender.equals("女") && actualGender.equals("female"));
    }

    /**
     * 获取过滤后的教师列表（支持科目和年级筛选）
     */
    private List<Teacher> getFilteredTeacherList(int page, int size, String subject, String grade, String keyword) {
        // 如果没有年级筛选，使用原有方法
        if (!StringUtils.hasText(grade)) {
            return getTeacherList(page, size, subject, keyword);
        }

        // 有年级筛选时，需要通过课程年级关联表查询
        Page<Teacher> pageParam = new Page<>(page, size);

        // 构建查询条件
        QueryWrapper<Teacher> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("t.is_deleted", false);
        queryWrapper.eq("t.is_verified", true);

        // 添加年级筛选条件
        queryWrapper.exists("SELECT 1 FROM courses c " +
                           "INNER JOIN course_grades cg ON c.id = cg.course_id " +
                           "WHERE c.teacher_id = t.id AND c.is_deleted = 0 AND c.status = 'active' " +
                           "AND cg.grade = {0}", grade);

        // 添加科目筛选条件
        if (StringUtils.hasText(subject)) {
            queryWrapper.exists("SELECT 1 FROM teacher_subjects ts " +
                               "INNER JOIN subjects s ON ts.subject_id = s.id " +
                               "WHERE ts.teacher_id = t.id AND s.name = {0} AND s.is_deleted = 0", subject);
        }

        // 添加关键词筛选条件
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like("t.real_name", keyword)
                    .or()
                    .like("t.specialties", keyword)
                    .or()
                    .like("t.introduction", keyword)
            );
        }

        queryWrapper.orderByDesc("t.id");

        // 使用原生SQL查询
        List<Teacher> teachers = teacherMapper.selectList(new QueryWrapper<Teacher>()
                .eq("is_deleted", false)
                .eq("is_verified", true)
                .orderByDesc("id"));

        // 手动过滤年级和科目
        List<Teacher> filteredTeachers = new ArrayList<>();
        for (Teacher teacher : teachers) {
            boolean matchGrade = true;
            boolean matchSubject = true;
            boolean matchKeyword = true;

            // 检查年级匹配
            if (StringUtils.hasText(grade)) {
                List<String> teacherGrades = getTeacherGradesList(teacher.getId());
                matchGrade = teacherGrades.contains(grade);
            }

            // 检查科目匹配
            if (StringUtils.hasText(subject)) {
                List<Long> subjectIds = teacherSubjectMapper.getSubjectIdsByTeacherId(teacher.getId());
                matchSubject = false;
                for (Long subjectId : subjectIds) {
                    try {
                        Subject subjectEntity = subjectService.getSubjectById(subjectId);
                        if (subjectEntity != null && subject.equals(subjectEntity.getName())) {
                            matchSubject = true;
                            break;
                        }
                    } catch (Exception e) {
                        // 忽略异常
                    }
                }
            }

            // 检查关键词匹配
            if (StringUtils.hasText(keyword)) {
                matchKeyword = (teacher.getRealName() != null && teacher.getRealName().contains(keyword)) ||
                              (teacher.getSpecialties() != null && teacher.getSpecialties().contains(keyword)) ||
                              (teacher.getIntroduction() != null && teacher.getIntroduction().contains(keyword));
            }

            if (matchGrade && matchSubject && matchKeyword) {
                filteredTeachers.add(teacher);
            }
        }

        // 手动分页
        int start = (page - 1) * size;
        int end = Math.min(start + size, filteredTeachers.size());
        if (start >= filteredTeachers.size()) {
            return new ArrayList<>();
        }

        return filteredTeachers.subList(start, end);
    }

    /**
     * 获取教师控制台统计数据
     */
    @Override
    public Map<String, Object> getTeacherStatistics(Long userId) {
        Map<String, Object> statistics = new HashMap<>();

        // 获取教师信息
        Teacher teacher = getTeacherByUserId(userId);
        if (teacher == null) {
            throw new RuntimeException("教师信息不存在");
        }

        // 1. 调课申请数 - 使用mapper中的专用方法统计
        int rescheduleRequestsCount = rescheduleRequestMapper.countPendingByTeacherId(teacher.getId());

        // 2. 总课程数 - 查询教师的所有课程
        QueryWrapper<Course> courseWrapper = new QueryWrapper<>();
        courseWrapper.eq("teacher_id", teacher.getId());
        courseWrapper.eq("is_deleted", false);
        Long totalCourses = courseMapper.selectCount(courseWrapper);

        // 3. 即将上课数 - 查询状态为progressing的课程安排
        QueryWrapper<Schedule> upcomingWrapper = new QueryWrapper<>();
        upcomingWrapper.eq("teacher_id", teacher.getId());
        upcomingWrapper.eq("status", "progressing");
        upcomingWrapper.eq("is_deleted", false);
        Long upcomingClasses = scheduleMapper.selectCount(upcomingWrapper);

        // 4. 预约申请数 - 查询状态为pending的预约申请
        QueryWrapper<BookingRequest> bookingWrapper = new QueryWrapper<>();
        bookingWrapper.eq("teacher_id", teacher.getId());
        bookingWrapper.eq("status", "pending");
        bookingWrapper.eq("is_deleted", false);
        Long bookingRequests = bookingRequestMapper.selectCount(bookingWrapper);

        statistics.put("rescheduleRequests", rescheduleRequestsCount);
        statistics.put("totalCourses", totalCourses != null ? totalCourses.intValue() : 0);
        statistics.put("upcomingClasses", upcomingClasses != null ? upcomingClasses.intValue() : 0);
        statistics.put("bookingRequests", bookingRequests != null ? bookingRequests.intValue() : 0);

        log.info("获取教师统计数据成功: userId={}, statistics={}", userId, statistics);

        return statistics;
    }
}
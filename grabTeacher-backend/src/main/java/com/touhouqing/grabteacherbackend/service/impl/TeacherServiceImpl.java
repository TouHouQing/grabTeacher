package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.model.vo.TeacherDetailVO;
import com.touhouqing.grabteacherbackend.model.dto.TeacherInfoDTO;
import com.touhouqing.grabteacherbackend.model.vo.TeacherListVO;
import com.touhouqing.grabteacherbackend.model.dto.TeacherMatchDTO;
import com.touhouqing.grabteacherbackend.model.vo.TeacherMatchVO;
import com.touhouqing.grabteacherbackend.model.vo.TeacherProfileVO;
import com.touhouqing.grabteacherbackend.model.vo.TeacherScheduleVO;
import com.touhouqing.grabteacherbackend.model.dto.TimeSlotAvailabilityDTO;
import com.touhouqing.grabteacherbackend.model.dto.TimeSlotDTO;
import com.touhouqing.grabteacherbackend.model.entity.Course;
import com.touhouqing.grabteacherbackend.model.entity.CourseGrade;
import com.touhouqing.grabteacherbackend.model.entity.Schedule;
import com.touhouqing.grabteacherbackend.model.entity.Student;
import com.touhouqing.grabteacherbackend.model.entity.Subject;
import com.touhouqing.grabteacherbackend.model.entity.Teacher;
import com.touhouqing.grabteacherbackend.model.entity.User;
import com.touhouqing.grabteacherbackend.model.entity.TeacherSubject;
import com.touhouqing.grabteacherbackend.model.entity.BookingRequest;
import com.touhouqing.grabteacherbackend.mapper.CourseMapper;
import com.touhouqing.grabteacherbackend.mapper.CourseGradeMapper;
import com.touhouqing.grabteacherbackend.mapper.ScheduleMapper;
import com.touhouqing.grabteacherbackend.mapper.StudentMapper;
import com.touhouqing.grabteacherbackend.mapper.TeacherMapper;
import com.touhouqing.grabteacherbackend.mapper.TeacherSubjectMapper;
import com.touhouqing.grabteacherbackend.mapper.SubjectMapper;

import com.touhouqing.grabteacherbackend.mapper.UserMapper;
import com.touhouqing.grabteacherbackend.mapper.BookingRequestMapper;
import com.touhouqing.grabteacherbackend.mapper.RescheduleRequestMapper;
import com.touhouqing.grabteacherbackend.service.SubjectService;
import com.touhouqing.grabteacherbackend.service.TeacherService;
import com.touhouqing.grabteacherbackend.service.TeacherScheduleCacheService;
import com.touhouqing.grabteacherbackend.util.AliyunOssUtil;

import com.touhouqing.grabteacherbackend.util.TimeSlotUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "teachers")
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

    private final TeacherScheduleCacheService teacherScheduleCacheService;
    private final SubjectMapper subjectMapper;
    private final AliyunOssUtil ossUtil;

    private final com.touhouqing.grabteacherbackend.service.CacheKeyEvictor cacheKeyEvictor;

    /**
     * 根据用户ID获取教师信息
     */
    @Override
    @Cacheable(cacheNames = "teachers",
               keyGenerator = "teacherCacheKeyGenerator",
               unless = "#result == null")
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
    @Cacheable(cacheNames = "teachers", key = "#teacherId", unless = "#result == null")
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
    @Cacheable(cacheNames = "teacherDetails",
               keyGenerator = "teacherCacheKeyGenerator",
               unless = "#result == null")
    public TeacherDetailVO getTeacherDetailById(Long teacherId) {
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

        return TeacherDetailVO.builder()
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
                .verified(teacher.getVerified())
                .deleted(teacher.getDeleted())
                .deletedAt(teacher.getDeletedAt())
                .build();
    }

    /**
     * 根据用户ID获取教师详细信息（包含科目信息）
     */
    public TeacherProfileVO getTeacherProfileByUserId(Long userId) {
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

        // 获取用户的出生年月和头像
        User user = userMapper.selectById(teacher.getUserId());
        String birthDate = user != null ? user.getBirthDate() : null;
        String avatarUrl = user != null ? user.getAvatarUrl() : null;

        return TeacherProfileVO.builder()
                .id(teacher.getId())
                .userId(teacher.getUserId())
                .realName(teacher.getRealName())
                .birthDate(birthDate)
                .educationBackground(teacher.getEducationBackground())
                .teachingExperience(teacher.getTeachingExperience())
                .specialties(teacher.getSpecialties())
                .subjectIds(subjectIds)
                .hourlyRate(teacher.getHourlyRate())
                .introduction(teacher.getIntroduction())
                .videoIntroUrl(teacher.getVideoIntroUrl())
                .gender(teacher.getGender())
                .avatarUrl(avatarUrl)
                .availableTimeSlots(availableTimeSlots)
                .verified(teacher.getVerified())
                .deleted(teacher.getDeleted())
                .deletedAt(teacher.getDeletedAt())
                .build();
    }

    /**
     * 获取教师列表
     */
    @Override
    public List<Teacher> getTeacherList(int page, int size, String subject, String keyword) {
        // 如果有科目筛选，需要通过关联表查询
        if (StringUtils.hasText(subject)) {
            return getTeacherListBySubject(page, size, subject, keyword);
        }

        // 没有科目筛选时的简单查询
        Page<Teacher> pageParam = new Page<>(page, size);
        QueryWrapper<Teacher> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("is_deleted", false);
        queryWrapper.eq("is_verified", true); // 数据列不变，仅实体字段改名

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
     * 统计教师总数（用于分页）
     */
    @Override
    public long countTeachers(String subject, String grade, String keyword) {
        // 如果有科目筛选，需要通过关联表查询
        if (StringUtils.hasText(subject)) {
            return countTeachersBySubject(subject, grade, keyword);
        }

        // 没有科目筛选时的简单统计
        QueryWrapper<Teacher> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_deleted", false);
        queryWrapper.eq("is_verified", true);

        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like("real_name", keyword)
                    .or()
                    .like("specialties", keyword)
                    .or()
                    .like("introduction", keyword)
            );
        }

        return teacherMapper.selectCount(queryWrapper);
    }

    /**
     * 根据科目统计教师数量
     */
    private long countTeachersBySubject(String subject, String grade, String keyword) {
        QueryWrapper<TeacherSubject> tsWrapper = new QueryWrapper<>();
        tsWrapper.eq("subject_name", subject);

        if (StringUtils.hasText(grade)) {
            tsWrapper.like("grade_levels", grade);
        }

        List<TeacherSubject> teacherSubjects = teacherSubjectMapper.selectList(tsWrapper);
        if (teacherSubjects.isEmpty()) {
            return 0L;
        }

        List<Long> teacherIds = teacherSubjects.stream()
                .map(TeacherSubject::getTeacherId)
                .distinct()
                .collect(Collectors.toList());

        QueryWrapper<Teacher> teacherWrapper = new QueryWrapper<>();
        teacherWrapper.in("id", teacherIds);
        teacherWrapper.eq("is_deleted", false);
        teacherWrapper.eq("is_verified", true);

        if (StringUtils.hasText(keyword)) {
            teacherWrapper.and(wrapper -> wrapper
                    .like("real_name", keyword)
                    .or()
                    .like("specialties", keyword)
                    .or()
                    .like("introduction", keyword)
            );
        }

        return teacherMapper.selectCount(teacherWrapper);
    }

    /**
     * 根据科目获取教师列表
     */
    private List<Teacher> getTeacherListBySubject(int page, int size, String subject, String keyword) {
        int offset = Math.max(0, (page - 1) * size);
        return teacherMapper.findTeachersBySubjectPaged(subject, keyword, offset, size);
    }

    /**
     * 获取教师列表（包含科目信息）
     */
    @Override
    @Cacheable(cacheNames = "teacherList",
               keyGenerator = "teacherCacheKeyGenerator",
               sync = true)
    public List<TeacherListVO> getTeacherListWithSubjects(int page, int size, String subject, String grade, String keyword) {
        // 获取教师列表（分页+筛选）
        List<Teacher> teachers = getFilteredTeacherList(page, size, subject, grade, keyword);
        // 批量装配，消除 N+1
        return assembleTeacherListResponses(teachers);
    }

    /**
     * 获取精选教师列表（天下名师页面使用）
     */
    @Override
    @Cacheable(cacheNames = "featuredTeachers",
               keyGenerator = "teacherCacheKeyGenerator",
               sync = true)
    public List<TeacherListVO> getFeaturedTeachers(int page, int size, String subject, String grade, String keyword) {
        // 获取精选教师列表（分页+筛选）
        List<Teacher> teachers = getFeaturedTeacherList(page, size, subject, grade, keyword);
        // 批量装配，消除 N+1
        return assembleTeacherListResponses(teachers);
    }

    @Override
    @Cacheable(cacheNames = "featuredTeachersCount",
               keyGenerator = "teacherCacheKeyGenerator",
               sync = true)
    public long countFeaturedTeachers(String subject, String grade, String keyword) {
        // 统计精选教师总数（考虑筛选条件），用于服务端分页 total
        QueryWrapper<Teacher> qw = new QueryWrapper<>();
        qw.eq("is_deleted", false)
          .eq("is_verified", true)
          .eq("is_featured", true);
        if (org.springframework.util.StringUtils.hasText(subject)) {
            qw.exists("SELECT 1 FROM teacher_subjects ts INNER JOIN subjects s ON ts.subject_id = s.id " +
                     "WHERE ts.teacher_id = teachers.id AND s.name = '" + subject + "' AND s.is_deleted = 0");
        }
        if (org.springframework.util.StringUtils.hasText(grade)) {
            qw.exists("SELECT 1 FROM courses c INNER JOIN course_grades cg ON c.id = cg.course_id " +
                     "WHERE c.teacher_id = teachers.id AND c.is_deleted = 0 AND c.status = 'active' AND cg.grade = '" + grade + "'");
        }
        if (org.springframework.util.StringUtils.hasText(keyword)) {
            qw.and(w -> w.like("real_name", keyword).or().like("specialties", keyword).or().like("introduction", keyword));
        }
        Long cnt = teacherMapper.selectCount(qw);
        return cnt != null ? cnt : 0L;
    }


    // 批量装配教师列表响应，消除 N+1 查询
    private List<TeacherListVO> assembleTeacherListResponses(List<Teacher> teachers) {
        if (teachers == null || teachers.isEmpty()) {
            return new ArrayList<>();
        }
        // 1) 准备ID集合
        List<Long> teacherIds = teachers.stream().map(Teacher::getId).collect(Collectors.toList());
        List<Long> userIds = teachers.stream().map(Teacher::getUserId).collect(Collectors.toList());

        // 2) 批量查询用户信息并映射
        QueryWrapper<User> userQ = new QueryWrapper<>();
        userQ.in("id", userIds);
        List<User> users = userMapper.selectList(userQ);
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));

        // 3) 批量查询教师-科目关联和科目名称
        List<TeacherSubject> tsList = teacherSubjectMapper.findByTeacherIds(teacherIds);
        Map<Long, List<Long>> teacherToSubjectIds = new HashMap<>();
        for (TeacherSubject ts : tsList) {
            teacherToSubjectIds.computeIfAbsent(ts.getTeacherId(), k -> new ArrayList<>()).add(ts.getSubjectId());
        }
        // 收集所有 subjectId 并批量查科目名称
        Set<Long> subjectIds = tsList.stream().map(TeacherSubject::getSubjectId).collect(Collectors.toSet());
        Map<Long, String> subjectNameMap = new HashMap<>();
        if (!subjectIds.isEmpty()) {
            QueryWrapper<Subject> sQ = new QueryWrapper<>();
            sQ.in("id", subjectIds);
            sQ.eq("is_deleted", false);
            List<Subject> subjList = subjectMapper.selectList(sQ);
            for (Subject s : subjList) {
                subjectNameMap.put(s.getId(), s.getName());
            }
        }

        // 4) 批量查询活跃课程并一次性查年级表
        List<Course> activeCourses = courseMapper.findActiveByTeacherIds(teacherIds);
        Map<Long, List<Long>> teacherToCourseIds = new HashMap<>();
        for (Course c : activeCourses) {
            teacherToCourseIds.computeIfAbsent(c.getTeacherId(), k -> new ArrayList<>()).add(c.getId());
        }
        Set<Long> courseIds = activeCourses.stream().map(Course::getId).collect(Collectors.toSet());
        Map<Long, Set<String>> teacherToGrades = new HashMap<>();
        if (!courseIds.isEmpty()) {
            List<CourseGrade> cgs = courseGradeMapper.findByCourseIds(new ArrayList<>(courseIds));
            // 反向映射 courseId -> grades
            Map<Long, List<String>> courseToGrades = new HashMap<>();
            for (CourseGrade cg : cgs) {
                courseToGrades.computeIfAbsent(cg.getCourseId(), k -> new ArrayList<>()).add(cg.getGrade());
            }
            for (Map.Entry<Long, List<Long>> e : teacherToCourseIds.entrySet()) {
                Long tId = e.getKey();
                Set<String> gset = new HashSet<>();
                for (Long cid : e.getValue()) {
                    List<String> gs = courseToGrades.get(cid);
                    if (gs != null) gset.addAll(gs);
                }
                teacherToGrades.put(tId, gset);
            }
        }

        // 5) 组装DTO
        List<TeacherListVO> list = new ArrayList<>();
        for (Teacher t : teachers) {
            User user = userMap.get(t.getUserId());
            List<Long> sids = teacherToSubjectIds.getOrDefault(t.getId(), Collections.emptyList());
            List<String> subjectNames = sids.stream()
                    .map(subjectNameMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            List<String> grades = new ArrayList<>(teacherToGrades.getOrDefault(t.getId(), Collections.emptySet()));

            // 列表端点仅返回短介绍，降低回包体积；详情端点仍返回完整介绍
            String intro = t.getIntroduction();
            String shortIntro = (intro != null && intro.length() > 160) ? intro.substring(0, 160) + "…" : intro;

            TeacherListVO dto = TeacherListVO.builder()
                    .id(t.getId())
                    .realName(t.getRealName())
                    .avatarUrl(user != null ? user.getAvatarUrl() : null)
                    .educationBackground(t.getEducationBackground())
                    .teachingExperience(t.getTeachingExperience())
                    .specialties(t.getSpecialties())
                    .subjects(subjectNames)
                    .grades(grades)
                    .hourlyRate(t.getHourlyRate())
                    .introduction(shortIntro)
                    .gender(t.getGender())
                    .verified(t.getVerified())
                    .build();
            list.add(dto);
        }
        return list;
    }

    /**
     * 更新教师信息（教师自己更新，不能修改科目和收费）
     */
    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = "teachers", allEntries = true),
        @CacheEvict(cacheNames = "teacherDetails", allEntries = true),
        @CacheEvict(cacheNames = "teacherList", allEntries = true),
        @CacheEvict(cacheNames = "teacherMatch", allEntries = true)
    })
    public Teacher updateTeacherInfo(Long userId, TeacherInfoDTO request) {
        Teacher teacher = getTeacherByUserId(userId);
        if (teacher == null) {
            throw new RuntimeException("教师信息不存在");
        }
        String oldAvailableTimeSlots = teacher.getAvailableTimeSlots();

        // 教师只能更新基本信息，不能修改科目和收费
        if (request.getRealName() != null) {
            teacher.setRealName(request.getRealName());
        }

        // 更新用户表中的出生年月/头像
        if (request.getBirthDate() != null || request.getAvatarUrl() != null) {
            User user = userMapper.selectById(userId);
            if (user != null) {
                String oldAvatar = user.getAvatarUrl();
                if (request.getBirthDate() != null) {
                    user.setBirthDate(request.getBirthDate());
                }
                if (request.getAvatarUrl() != null && !request.getAvatarUrl().isEmpty()) {
                    user.setAvatarUrl(request.getAvatarUrl());
                }
                userMapper.updateById(user);
                // 删除旧头像
                if (request.getAvatarUrl() != null && oldAvatar != null && !oldAvatar.isEmpty() && !oldAvatar.equals(request.getAvatarUrl())) {
                    ossUtil.deleteByUrl(oldAvatar);
                }
            }
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

        try {
            if (!Objects.equals(oldAvailableTimeSlots, teacher.getAvailableTimeSlots())) {
                cacheKeyEvictor.evictTeacherScheduleAndAvailability(teacher.getId());
                log.info("已清理教师课表/可用性相关缓存，teacherId={}", teacher.getId());
            }
        } catch (Exception e) {
            log.warn("清理教师课表/可用性缓存失败 teacherId={}", teacher.getId(), e);
        }

        return teacher;
    }

    /**
     * 匹配教师 - 优化版本，减少数据库查询次数
     */
    @Override
    @Cacheable(cacheNames = "teacherMatch",
               keyGenerator = "teacherCacheKeyGenerator",
               sync = true)
    public List<TeacherMatchVO> matchTeachers(TeacherMatchDTO request) {
        log.info("开始匹配教师，请求参数: {}", request);

        // 前端现在直接发送1-7格式，不需要转换
        if (request.getPreferredWeekdays() != null && !request.getPreferredWeekdays().isEmpty()) {
            log.info("学生偏好星期几（无需转换）: {}", request.getPreferredWeekdays());
        }

        // 使用优化的查询方法
        List<Teacher> teachers = matchTeachersOptimized(request);

        // 转换为响应DTO并计算匹配分数
        List<TeacherMatchVO> responses = teachers.stream()
                .filter(teacher -> matchesGenderPreference(teacher, request)) // 添加性别过滤
                .map(teacher -> convertToMatchResponse(teacher, request))
                .sorted(Comparator.comparing(TeacherMatchVO::getMatchScore).reversed())
                .limit(request.getLimit() != null ? request.getLimit() : 3)
                .collect(Collectors.toList());

        log.info("匹配到 {} 位教师", responses.size());

        return responses;
    }

    /**
     * 优化的教师匹配查询 - 使用JOIN减少数据库查询次数
     */
    private List<Teacher> matchTeachersOptimized(TeacherMatchDTO request) {
        // 智能匹配功能只匹配提供1对1课程的教师
        
        // 如果同时指定了科目和年级，使用联合查询
        if (StringUtils.hasText(request.getSubject()) && StringUtils.hasText(request.getGrade())) {
            return teacherMapper.findOneOnOneTeachersBySubjectAndGrade(request.getSubject(), request.getGrade());
        }

        // 如果只指定了科目
        if (StringUtils.hasText(request.getSubject())) {
            return teacherMapper.findOneOnOneTeachersBySubject(request.getSubject());
        }

        // 如果只指定了年级
        if (StringUtils.hasText(request.getGrade())) {
            return teacherMapper.findOneOnOneTeachersByGrade(request.getGrade());
        }

        // 如果都没指定，返回所有提供1对1课程的已认证教师
        return teacherMapper.findAllOneOnOneTeachers();
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
    private TeacherMatchVO convertToMatchResponse(Teacher teacher, TeacherMatchDTO request) {
        // 解析教师的可上课时间
        List<TimeSlotDTO> availableTimeSlots = TimeSlotUtil.fromJsonString(teacher.getAvailableTimeSlots());

        // 生成可读的时间安排描述
        List<String> scheduleDescriptions = generateScheduleDescriptions(availableTimeSlots);

        TeacherMatchVO response = TeacherMatchVO.builder()
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
                .isVerified(teacher.getVerified())
                .gender(teacher.getGender())
                .availableTimeSlots(availableTimeSlots)
                .build();

        // 计算匹配分数和时间匹配度
        log.info("正在计算教师 {} (ID: {}) 的匹配分数", teacher.getRealName(), teacher.getId());
        int matchScore = calculateMatchScore(teacher, request);
        int timeMatchScore = calculateTimeMatchScore(availableTimeSlots, request);
        log.info("教师 {} 的匹配分数: 总分={}, 时间匹配度={}", teacher.getRealName(), matchScore, timeMatchScore);

        response.setMatchScore(matchScore);
        response.setTimeMatchScore(timeMatchScore);

        return response;
    }

    /**
     * 计算匹配分数 - 基于学生填写信息的智能匹配
     */
    private int calculateMatchScore(Teacher teacher, TeacherMatchDTO request) {
        int totalScore = 0;

        // 1. 科目匹配 (30分) - 最重要的匹配因素
        totalScore += calculateSubjectMatchScore(teacher, request);

        // 2. 年级匹配 (25分) - 第二重要的匹配因素
        totalScore += calculateGradeMatchScore(teacher, request);

        // 3. 时间匹配 (25分) - 基于学生的时间偏好
        List<TimeSlotDTO> availableTimeSlots = TimeSlotUtil.fromJsonString(teacher.getAvailableTimeSlots());
        totalScore += calculateTimeMatchScore(availableTimeSlots, request) * 25 / 100;

        // 4. 性别匹配 (10分) - 基于学生的性别偏好
        totalScore += calculateGenderMatchScore(teacher, request);

        // 5. 教学经验加成 (10分) - 经验丰富的教师更受欢迎
        totalScore += calculateExperienceBonus(teacher);

        // 确保分数在合理范围内 (40-95)，提供更好的区分度
        return Math.max(40, Math.min(totalScore, 95));
    }

    /**
     * 计算科目匹配分数 (0-30分)
     */
    private int calculateSubjectMatchScore(Teacher teacher, TeacherMatchDTO request) {
        if (!StringUtils.hasText(request.getSubject())) {
            return 20; // 没有科目要求时给予中等分数
        }

        List<Long> subjectIds = teacherSubjectMapper.getSubjectIdsByTeacherId(teacher.getId());
        for (Long subjectId : subjectIds) {
            try {
                String subjectName = subjectService.getSubjectById(subjectId).getName();
                if (request.getSubject().equals(subjectName)) {
                    return 30; // 完全匹配
                }
                // 相关科目逻辑
                if (isRelatedSubject(request.getSubject(), subjectName)) {
                    return 18; // 相关科目
                }
            } catch (Exception e) {
                log.warn("获取科目信息失败: subjectId={}", subjectId, e);
            }
        }
        return 5; // 不匹配但给予基础分数
    }

    /**
     * 计算年级匹配分数 (0-25分)
     */
    private int calculateGradeMatchScore(Teacher teacher, TeacherMatchDTO request) {
        if (!StringUtils.hasText(request.getGrade())) {
            return 15; // 没有年级要求时给予中等分数
        }

        if (hasMatchingGrade(teacher.getId(), request.getGrade())) {
            return 25; // 完全匹配
        }

        // 检查相邻年级
        if (hasAdjacentGrade(teacher.getId(), request.getGrade())) {
            return 12; // 相邻年级
        }

        return 3; // 不匹配但给予基础分数
    }

    /**
     * 计算性别匹配分数 (0-10分)
     */
    private int calculateGenderMatchScore(Teacher teacher, TeacherMatchDTO request) {
        if (!StringUtils.hasText(request.getPreferredGender())) {
            return 8; // 没有性别偏好时给予较高分数
        }

        if (request.getPreferredGender().equals(teacher.getGender())) {
            return 10; // 性别匹配
        }

        return 3; // 性别不匹配但给予基础分数
    }

    /**
     * 计算教学经验加成分数 (0-10分)
     */
    private int calculateExperienceBonus(Teacher teacher) {
        Integer experience = teacher.getTeachingExperience();
        if (experience == null || experience <= 0) {
            return 2; // 新教师给予基础分数
        }

        // 根据经验年数给分，经验越丰富分数越高
        if (experience >= 10) {
            return 10; // 资深教师
        } else if (experience >= 5) {
            return 8; // 有经验教师
        } else if (experience >= 3) {
            return 6; // 中级教师
        } else if (experience >= 1) {
            return 4; // 初级教师
        } else {
            return 2; // 新手教师
        }
    }

    /**
     * 判断两个科目是否相关
     */
    private boolean isRelatedSubject(String subject1, String subject2) {
        // 定义相关科目组
        String[][] relatedGroups = {
            {"数学", "物理", "化学"}, // 理科组
            {"语文", "历史", "政治"}, // 文科组
            {"英语", "日语", "法语"}, // 语言组
            {"美术", "音乐", "舞蹈"}  // 艺术组
        };

        for (String[] group : relatedGroups) {
            boolean hasSubject1 = Arrays.asList(group).contains(subject1);
            boolean hasSubject2 = Arrays.asList(group).contains(subject2);
            if (hasSubject1 && hasSubject2) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查教师是否教授相邻年级
     */
    private boolean hasAdjacentGrade(Long teacherId, String targetGrade) {
        // 定义年级相邻关系
        String[][] adjacentGrades = {
            {"小学一年级", "小学二年级"},
            {"小学二年级", "小学三年级"},
            {"小学三年级", "小学四年级"},
            {"小学四年级", "小学五年级"},
            {"小学五年级", "小学六年级"},
            {"小学六年级", "初中一年级"},
            {"初中一年级", "初中二年级"},
            {"初中二年级", "初中三年级"},
            {"初中三年级", "高中一年级"},
            {"高中一年级", "高中二年级"},
            {"高中二年级", "高中三年级"}
        };

        // 获取教师教授的年级
        List<String> teacherGrades = getTeacherGradesList(teacherId);

        for (String[] pair : adjacentGrades) {
            if (targetGrade.equals(pair[0]) && teacherGrades.contains(pair[1])) {
                return true;
            }
            if (targetGrade.equals(pair[1]) && teacherGrades.contains(pair[0])) {
                return true;
            }
        }
        return false;
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
    @Cacheable(cacheNames = "grades",
               keyGenerator = "teacherCacheKeyGenerator",
               unless = "#result == null || #result.isEmpty()")
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
    @Cacheable(cacheNames = "teacherSchedule",
               keyGenerator = "teacherCacheKeyGenerator",
               sync = true)
    public TeacherScheduleVO getTeacherPublicSchedule(Long teacherId, LocalDate startDate, LocalDate endDate) {
        // 获取教师信息
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            throw new RuntimeException("教师不存在");
        }

        // 获取指定时间范围内的课程安排（一次 DB）
        List<Schedule> schedules = scheduleMapper.findByTeacherIdAndDateRange(teacherId, startDate, endDate);

        // 按日期分组
        Map<LocalDate, List<Schedule>> schedulesByDate = schedules.stream()
                .collect(Collectors.groupingBy(Schedule::getScheduledDate));

        // 基于本次 DB 结果构建 busy 映射，并批量回写 Redis（避免逐日多次往返）
        Map<LocalDate, List<String>> busyMap = new HashMap<>();
        for (Schedule s : schedules) {
            LocalDate d = s.getScheduledDate();
            String slot = s.getStartTime().toString() + "-" + s.getEndTime().toString();
            busyMap.computeIfAbsent(d, k -> new ArrayList<>()).add(slot);
        }
        LocalDate fill = startDate;
        while (!fill.isAfter(endDate)) {
            busyMap.putIfAbsent(fill, new ArrayList<>());
            fill = fill.plusDays(1);
        }
        try { teacherScheduleCacheService.putBusySlotsBatch(teacherId, busyMap); } catch (Exception ignore) {}

        // 生成每日课表
        List<TeacherScheduleVO.DaySchedule> daySchedules = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            List<String> busySlots = busyMap.getOrDefault(currentDate, java.util.Collections.emptyList());
            List<Schedule> daySchedules_raw = schedulesByDate.getOrDefault(currentDate, new ArrayList<>());

            // 生成该日的时间段信息（使用内存 busy 判定占用）
            List<TeacherScheduleVO.TimeSlotInfo> timeSlots = generateTimeSlotsWithBusy(teacherId, daySchedules_raw, currentDate, busySlots);

            int availableCount = (int) timeSlots.stream().filter(TeacherScheduleVO.TimeSlotInfo::getAvailable).count();
            int bookedCount = (int) timeSlots.stream().filter(TeacherScheduleVO.TimeSlotInfo::getBooked).count();


            TeacherScheduleVO.DaySchedule daySchedule = TeacherScheduleVO.DaySchedule.builder()
                    .date(currentDate)
                    .dayOfWeek(currentDate.getDayOfWeek().getValue()) // 使用ISO标准：1=周一, 7=周日
                    .timeSlots(timeSlots)
                    .availableCount(availableCount)
                    .bookedCount(bookedCount)
                    .build();

            daySchedules.add(daySchedule);
            currentDate = currentDate.plusDays(1);
        }

        return TeacherScheduleVO.builder()
                .teacherId(teacherId)
                .teacherName(teacher.getRealName())
                .startDate(startDate)
                .endDate(endDate)
                .daySchedules(daySchedules)
                .build();
    }

    @Override
    @Cacheable(cacheNames = "teacherAvailability",
               keyGenerator = "teacherCacheKeyGenerator",
               sync = true)
    public List<TimeSlotAvailabilityDTO> checkTeacherAvailability(Long teacherId, LocalDate startDate, LocalDate endDate, List<String> timeSlots) {
        List<TimeSlotAvailabilityDTO> availabilities = new ArrayList<>();

        // 获取教师设置的可上课时间
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            return availabilities;
        }

        List<TimeSlotDTO> teacherAvailableSlots = TimeSlotUtil.fromJsonString(teacher.getAvailableTimeSlots());

        // 如果教师没有设置可上课时间，返回所有时间段为不可用
        if (teacherAvailableSlots.isEmpty()) {
            if (timeSlots == null || timeSlots.isEmpty()) {
                timeSlots = getDefaultTimeSlots();
            }
            for (int weekday = 1; weekday <= 7; weekday++) {
                for (String timeSlot : timeSlots) {
                    TimeSlotAvailabilityDTO availability = TimeSlotAvailabilityDTO.builder()
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
            // 一次性加载范围内所有课程，构建 busyMap 并批量回填 Redis，避免每个 timeSlot 再次访问 DB
            Map<LocalDate, List<String>> busyMap = new java.util.HashMap<>();
            List<Schedule> range = scheduleMapper.findByTeacherIdAndDateRange(teacherId, startDate, endDate);
            for (Schedule s : range) {
                LocalDate d = s.getScheduledDate();
                String slot = s.getStartTime().toString() + "-" + s.getEndTime().toString();
                busyMap.computeIfAbsent(d, k -> new ArrayList<>()).add(slot);
            }
            LocalDate fill = startDate;
            while (!fill.isAfter(endDate)) { busyMap.putIfAbsent(fill, new ArrayList<>()); fill = fill.plusDays(1); }
            try { teacherScheduleCacheService.putBusySlotsBatch(teacherId, busyMap); } catch (Exception ignore) {}

            // 只检查教师设置的可上课时间段（使用内存 busyMap 判断冲突）
            for (TimeSlotDTO teacherSlot : teacherAvailableSlots) {
                int weekday = teacherSlot.getWeekday();
                List<String> slotTimes = teacherSlot.getTimeSlots();
                if (slotTimes != null) {
                    for (String timeSlot : slotTimes) {
                        TimeSlotAvailabilityDTO availability = checkWeekdayTimeSlotAvailabilityFromBusyMap(
                            teacherId, weekday, timeSlot, startDate, endDate, busyMap);
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
    private List<TeacherScheduleVO.TimeSlotInfo> generateTimeSlotsWithBusy(Long teacherId, List<Schedule> daySchedules, LocalDate date, List<String> busySlots) {
        // 获取该日期对应的星期几（ISO标准：1=周一, 7=周日）
        int weekday = date.getDayOfWeek().getValue();

        // 获取教师设置的可上课时间段
        List<String> availableTimeSlots = getTeacherAvailableTimeSlotsForWeekday(teacherId, weekday);

        // 如果教师没有设置可上课时间，返回空列表（不显示任何时间段）
        if (availableTimeSlots.isEmpty()) {
            return new ArrayList<>();
        }

        List<TeacherScheduleVO.TimeSlotInfo> timeSlots = new ArrayList<>();

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

            boolean isBooked;
            Schedule existingSchedule = scheduleByTimeSlot.get(timeSlot);
            if (busySlots != null && !busySlots.isEmpty()) {
                // 用 busy 缓存判定是否占用
                isBooked = busySlots.stream().anyMatch(slot -> TimeSlotUtil.hasTimeConflict(timeSlot, slot));
            } else {
                isBooked = existingSchedule != null && !"cancelled".equals(existingSchedule.getStatus());
            }

            TeacherScheduleVO.TimeSlotInfo timeSlotInfo = TeacherScheduleVO.TimeSlotInfo.builder()
                    .timeSlot(timeSlot)
                    .startTime(startTime)
                    .endTime(endTime)
                    .available(!isBooked)
                    .booked(isBooked)
                    .build();

            if (isBooked && existingSchedule != null) {
                // 补充学生/课程信息
                Student student = studentMapper.selectById(existingSchedule.getStudentId());
                if (student != null) {
                    timeSlotInfo.setStudentName(student.getRealName());
                }
                Course course = courseMapper.selectById(existingSchedule.getCourseId());
                if (course != null) {
                    timeSlotInfo.setCourseTitle(course.getTitle());
                }
                timeSlotInfo.setStatus(existingSchedule.getStatus());
                timeSlotInfo.setIsTrial(existingSchedule.getTrial());
            }

            timeSlots.add(timeSlotInfo);
        }

        return timeSlots;
    }

    /**
     * 检查特定星期几和时间段的可用性
     */
    private TimeSlotAvailabilityDTO checkWeekdayTimeSlotAvailabilityFromBusyMap(Long teacherId, int weekday, String timeSlot,
                                                                                LocalDate startDate, LocalDate endDate,
                                                                                Map<LocalDate, List<String>> busyMap) {
        // 检查教师是否设置了该时间段为可上课时间
        List<String> teacherAvailableSlots = getTeacherAvailableTimeSlotsForWeekday(teacherId, weekday);
        boolean isTeacherAvailable = teacherAvailableSlots.isEmpty() || teacherAvailableSlots.contains(timeSlot);
        if (!isTeacherAvailable) {
            return TimeSlotAvailabilityDTO.builder()
                    .weekday(weekday)
                    .timeSlot(timeSlot)
                    .available(false)
                    .availabilityRate(0.0)
                    .conflictDates(new ArrayList<>())
                    .conflictReason("教师未设置该时间段为可上课时间")
                    .description("教师未在该时间段设置为可上课时间")
                    .build();
        }

        // 预解析目标时间段到分钟，减少解析开销
        int[] target = parseSlotToMinutes(timeSlot);
        List<String> conflictDates = new ArrayList<>();
        LocalDate cur = startDate;
        int totalWeeks = 0;
        int conflictWeeks = 0;
        while (!cur.isAfter(endDate)) {
            int wd = cur.getDayOfWeek().getValue();
            if (wd == weekday) {
                totalWeeks++;
                List<String> dayBusy = busyMap.getOrDefault(cur, java.util.Collections.emptyList());
                boolean conflict = false;
                if (target != null && !dayBusy.isEmpty()) {
                    for (String b : dayBusy) {
                        int[] bb = parseSlotToMinutes(b);
                        if (bb != null && bb[1] > target[0] && bb[0] < target[1]) { conflict = true; break; }
                    }
                }
                if (conflict) { conflictWeeks++; conflictDates.add(cur.toString()); }
            }
            cur = cur.plusDays(1);
        }
        double availabilityRate = totalWeeks > 0 ? (double) (totalWeeks - conflictWeeks) / totalWeeks : 1.0;
        boolean available = availabilityRate >= 0.5;
        String availabilityLevel = calculateAvailabilityLevel(availabilityRate);
        return TimeSlotAvailabilityDTO.builder()
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

    // 解析 "HH:mm-HH:mm" 到 [startMin, endMin]
    private int[] parseSlotToMinutes(String slot) {
        if (slot == null || slot.length() < 11) return null;
        try {
            int h1 = Integer.parseInt(slot.substring(0, 2));
            int m1 = Integer.parseInt(slot.substring(3, 5));
            int h2 = Integer.parseInt(slot.substring(6, 8));
            int m2 = Integer.parseInt(slot.substring(9, 11));
            int s = h1 * 60 + m1;
            int e = h2 * 60 + m2;
            if (e > s) return new int[]{s, e};
        } catch (Exception ignore) {}
        return null;
    }

    /**
     * 获取默认时间段列表 - 固定为6个系统上课时间
     */
    private List<String> getDefaultTimeSlots() {
        return Arrays.asList(
            "08:00-10:00", "10:00-12:00", "13:00-15:00",
            "15:00-17:00", "17:00-19:00", "19:00-21:00"
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
     * 计算时间匹配度 - 修复版本，正确处理星期几和时间段的匹配
     */
    private int calculateTimeMatchScore(List<TimeSlotDTO> availableTimeSlots, TeacherMatchDTO request) {
        log.info("=== 开始计算时间匹配度 ===");
        log.info("教师可用时间段: {}", availableTimeSlots);
        log.info("学生偏好星期几: {}", request.getPreferredWeekdays());
        log.info("学生偏好时间段: {}", request.getPreferredTimeSlots());

        if (availableTimeSlots == null || availableTimeSlots.isEmpty()) {
            log.info("教师没有可用时间段，返回0分");
            return 0;
        }

        // 如果学生没有时间偏好，认为所有时间都匹配，给予满分
        if ((request.getPreferredWeekdays() == null || request.getPreferredWeekdays().isEmpty()) &&
            (request.getPreferredTimeSlots() == null || request.getPreferredTimeSlots().isEmpty())) {
            log.info("学生没有时间偏好，返回满分100");
            return 100;
        }



        int totalScore = 0;
        int maxPossibleScore = 0;

        // 处理时间段匹配 - 必须同时匹配星期几和时间段
        if (request.getPreferredTimeSlots() != null && !request.getPreferredTimeSlots().isEmpty()) {
            // 获取学生偏好的星期几
            Set<Integer> studentPreferredWeekdays = new HashSet<>();
            if (request.getPreferredWeekdays() != null && !request.getPreferredWeekdays().isEmpty()) {
                studentPreferredWeekdays.addAll(request.getPreferredWeekdays());
            }

            for (String preferredTimeSlot : request.getPreferredTimeSlots()) {
                maxPossibleScore += 100; // 每个偏好时间段最高100分
                int bestMatchScore = 0;

                log.info("检查学生偏好时间段: {}", preferredTimeSlot);

                // 遍历教师的所有可用时间段
                for (TimeSlotDTO teacherSlot : availableTimeSlots) {
                    log.info("检查教师时间段 - 星期{}: {}", teacherSlot.getWeekday(), teacherSlot.getTimeSlots());

                    if (teacherSlot.getTimeSlots() != null && !teacherSlot.getTimeSlots().isEmpty()) {
                        // 如果学生指定了星期几偏好，必须严格匹配星期几
                        if (!studentPreferredWeekdays.isEmpty() &&
                            !studentPreferredWeekdays.contains(teacherSlot.getWeekday())) {
                            log.info("星期几不匹配，跳过 - 学生偏好: {}, 教师星期: {}",
                                   studentPreferredWeekdays, teacherSlot.getWeekday());
                            continue; // 星期几不匹配，跳过
                        }

                        // 如果学生没有指定星期几偏好，但选择了时间段，则降低匹配度
                        // 这种情况下，我们认为学生对星期几没有明确要求，但仍然需要时间段匹配
                        int weekdayMatchPenalty = studentPreferredWeekdays.isEmpty() ? 20 : 0; // 没有指定星期几时扣20分

                        // 检查时间段匹配
                        for (String teacherTimeSlot : teacherSlot.getTimeSlots()) {
                            int overlapScore = TimeSlotUtil.calculateTimeOverlapScore(preferredTimeSlot, teacherTimeSlot);
                            log.info("时间重叠计算: {} vs {} = {}分", preferredTimeSlot, teacherTimeSlot, overlapScore);

                            // 如果有重叠，这就是一个有效匹配
                            if (overlapScore > 0) {
                                // 应用星期几匹配惩罚
                                int adjustedScore = Math.max(0, overlapScore - weekdayMatchPenalty);
                                bestMatchScore = Math.max(bestMatchScore, adjustedScore);
                                log.info("调整后匹配分数: {} (原始: {}, 星期几惩罚: {})",
                                       adjustedScore, overlapScore, weekdayMatchPenalty);
                            }
                        }
                    }
                }
                log.info("时间段 {} 的最终匹配分数: {}", preferredTimeSlot, bestMatchScore);
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

        int finalScore = maxPossibleScore > 0 ? Math.min((totalScore * 100) / maxPossibleScore, 100) : 100;

        // 详细调试日志
        log.info("=== 时间匹配计算结果 ===");
        log.info("总分: {}, 最大可能分数: {}, 最终得分: {}", totalScore, maxPossibleScore, finalScore);
        log.info("========================");

        return finalScore;
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
    private boolean matchesGenderPreference(Teacher teacher, TeacherMatchDTO request) {
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

    /**
     * 获取精选教师列表（支持科目和年级筛选）
     */
    private List<Teacher> getFeaturedTeacherList(int page, int size, String subject, String grade, String keyword) {
        // 如果没有年级筛选，使用简化方法
        if (!StringUtils.hasText(grade)) {
            return getFeaturedTeacherListSimple(page, size, subject, keyword);
        }

        // 有年级筛选时，需要通过课程年级关联表查询
        // 构建查询条件
        QueryWrapper<Teacher> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("t.is_deleted", false);
        queryWrapper.eq("t.is_verified", true);
        queryWrapper.eq("t.is_featured", true); // 只查询精选教师

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
                .eq("is_featured", true)
                .orderByDesc("id")
                .last("LIMIT " + ((page - 1) * size) + ", " + size));

        return teachers != null ? teachers : new ArrayList<>();
    }

    /**
     * 获取精选教师列表（简化版，不包含年级筛选）
     */
    private List<Teacher> getFeaturedTeacherListSimple(int page, int size, String subject, String keyword) {
        QueryWrapper<Teacher> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_deleted", false);
        queryWrapper.eq("is_verified", true);
        queryWrapper.eq("is_featured", true); // 只查询精选教师

        // 添加科目筛选条件
        if (StringUtils.hasText(subject)) {
            queryWrapper.exists("SELECT 1 FROM teacher_subjects ts " +
                               "INNER JOIN subjects s ON ts.subject_id = s.id " +
                               "WHERE ts.teacher_id = teachers.id AND s.name = '" + subject + "' AND s.is_deleted = 0");
        }

        // 添加关键词筛选条件
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
        queryWrapper.last("LIMIT " + ((page - 1) * size) + ", " + size);

        List<Teacher> teachers = teacherMapper.selectList(queryWrapper);
        return teachers != null ? teachers : new ArrayList<>();
    }
}
package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.model.entity.*;
import com.touhouqing.grabteacherbackend.model.vo.TeacherDetailVO;
import com.touhouqing.grabteacherbackend.model.dto.TeacherInfoDTO;
import com.touhouqing.grabteacherbackend.model.vo.TeacherListVO;
import com.touhouqing.grabteacherbackend.model.dto.TeacherMatchDTO;
import com.touhouqing.grabteacherbackend.model.vo.TeacherMatchVO;
import com.touhouqing.grabteacherbackend.model.vo.TeacherProfileVO;
import com.touhouqing.grabteacherbackend.model.vo.TeacherScheduleVO;
import com.touhouqing.grabteacherbackend.model.vo.ClassRecordVO;

import com.touhouqing.grabteacherbackend.model.dto.TimeSlotDTO;
// import removed: Schedule replaced by CourseSchedule in services
import com.touhouqing.grabteacherbackend.mapper.CourseMapper;
import com.touhouqing.grabteacherbackend.mapper.StudentMapper;
import com.touhouqing.grabteacherbackend.mapper.TeacherMapper;
import com.touhouqing.grabteacherbackend.mapper.TeacherSubjectMapper;
import com.touhouqing.grabteacherbackend.mapper.SubjectMapper;

import com.touhouqing.grabteacherbackend.mapper.TeachingLocationMapper;

import com.touhouqing.grabteacherbackend.mapper.UserMapper;
import com.touhouqing.grabteacherbackend.mapper.BookingRequestMapper;
import com.touhouqing.grabteacherbackend.mapper.RescheduleRequestMapper;
import com.touhouqing.grabteacherbackend.mapper.HourDetailMapper;
import com.touhouqing.grabteacherbackend.service.SubjectService;
import com.touhouqing.grabteacherbackend.service.TeacherService;
import com.touhouqing.grabteacherbackend.service.TeacherScheduleCacheService;
import com.touhouqing.grabteacherbackend.util.AliyunOssUtil;

import com.touhouqing.grabteacherbackend.service.TeacherDailyAvailabilityService;
import com.touhouqing.grabteacherbackend.util.TimeSlotUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final com.touhouqing.grabteacherbackend.mapper.CourseScheduleMapper courseScheduleMapper;
    private final StudentMapper studentMapper;
    private final UserMapper userMapper;
    private final BookingRequestMapper bookingRequestMapper;
    private final TeachingLocationMapper teachingLocationMapper;

    private final RescheduleRequestMapper rescheduleRequestMapper;
    private final HourDetailMapper hourDetailMapper;

    private final TeacherScheduleCacheService teacherScheduleCacheService;
    private final SubjectMapper subjectMapper;
    private final AliyunOssUtil ossUtil;
    private final TeacherDailyAvailabilityService teacherDailyAvailabilityService;

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
     * 根据ID获取教师详情（包含用户信息、科目信息）
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

        // 获取用户信息（容错：若用户行缺失，仍返回教师基础信息）
        User user = userMapper.selectById(teacher.getUserId());

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


        // 支持线上与线下地点ID集合
        Boolean supportsOnline = Boolean.TRUE.equals(teacher.getSupportsOnline());
        List<Long> locationIds = new java.util.ArrayList<>();
        if (org.springframework.util.StringUtils.hasText(teacher.getTeachingLocations())) {
            for (String s : teacher.getTeachingLocations().split(",")) {
                if (org.springframework.util.StringUtils.hasText(s)) {
                    try { locationIds.add(Long.parseLong(s.trim())); } catch (NumberFormatException ignore) {}
                }
            }
        }


        return TeacherDetailVO.builder()
                .id(teacher.getId())
                .userId(teacher.getUserId())
                .realName(teacher.getRealName())
                .username(user != null ? user.getUsername() : null)
                .email(user != null ? user.getEmail() : null)
                .phone(user != null ? user.getPhone() : null)
                .avatarUrl(user != null ? user.getAvatarUrl() : null)
                .educationBackground(teacher.getEducationBackground())
                .teachingExperience(teacher.getTeachingExperience())
                .specialties(teacher.getSpecialties())
                .subjects(subjects)
                .introduction(teacher.getIntroduction())
                .gender(teacher.getGender())
                .level(teacher.getLevel())
                .supportsOnline(supportsOnline)
                .teachingLocationIds(locationIds)
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


        // 获取用户的出生年月和头像
        User user = userMapper.selectById(teacher.getUserId());
        String birthDate = user != null ? user.getBirthDate() : null;
        String avatarUrl = user != null ? user.getAvatarUrl() : null;
        // 兼容：计算 supportsOnline 与线下地点ID列表
        Boolean supportsOnline = teacher.getSupportsOnline();
        if (supportsOnline == null) {
            supportsOnline = (teacher.getTeachingLocations() == null || teacher.getTeachingLocations().isEmpty());
        }
        java.util.List<Long> offlineLocationIds = new java.util.ArrayList<>();
        if (teacher.getTeachingLocations() != null && !teacher.getTeachingLocations().isEmpty()) {
            offlineLocationIds = java.util.Arrays.stream(teacher.getTeachingLocations().split(","))
                    .filter(s -> s != null && !s.isEmpty())
                    .map(Long::valueOf)
                    .collect(java.util.stream.Collectors.toList());
        }


        return TeacherProfileVO.builder()
                .id(teacher.getId())
                .userId(teacher.getUserId())
                .realName(teacher.getRealName())
                .birthDate(birthDate)
                .educationBackground(teacher.getEducationBackground())
                .teachingExperience(teacher.getTeachingExperience())
                .specialties(teacher.getSpecialties())
                .subjectIds(subjectIds)
                .introduction(teacher.getIntroduction())
                .hourlyRateText(teacher.getHourlyRateText())
                .level(teacher.getLevel())
                .gender(teacher.getGender())
                .avatarUrl(avatarUrl)
                .supportsOnline(supportsOnline)
                .teachingLocationIds(offlineLocationIds)
                .teachingLocations(teacher.getTeachingLocations())
                .verified(teacher.getVerified())
                .adjustmentTimes(user != null ? user.getAdjustmentTimes() : null)
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
    public long countTeachers(String subject, String keyword) {
        // 如果有科目筛选，需要通过关联表查询
        if (StringUtils.hasText(subject)) {
            return countTeachersBySubject(subject, keyword);
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

    @Override
    public long countTeachers(String subject, String keyword, String realNameExact) {
        if (org.springframework.util.StringUtils.hasText(realNameExact)) {
            if (org.springframework.util.StringUtils.hasText(subject)) {
                return teacherMapper.countTeachersBySubjectExactName(subject, realNameExact);
            }
            QueryWrapper<Teacher> qw = new QueryWrapper<>();
            qw.eq("is_deleted", false).eq("is_verified", true).eq("real_name", realNameExact);
            Long cnt = teacherMapper.selectCount(qw);
            return cnt != null ? cnt : 0L;
        }
        return countTeachers(subject, keyword);
    }

    /**
     * 根据科目统计教师数量
     */
    private long countTeachersBySubject(String subject, String keyword) {
        return teacherMapper.countTeachersBySubject(subject, keyword);
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
               unless = "#result == null || #result.isEmpty()")
    public List<TeacherListVO> getTeacherListWithSubjects(int page, int size, String subject, String keyword) {
        // 获取教师列表（分页+筛选）
        List<Teacher> teachers = getFilteredTeacherList(page, size, subject, keyword);
        // 批量装配，消除 N+1
        return assembleTeacherListResponses(teachers);
    }

    @Override
    public List<TeacherListVO> getTeacherListWithSubjects(int page, int size, String subject, String keyword, String realNameExact) {
        if (org.springframework.util.StringUtils.hasText(realNameExact)) {
            // 精确姓名优先，且只匹配 real_name 字段
            List<Teacher> teachers;
            if (org.springframework.util.StringUtils.hasText(subject)) {
                int offset = Math.max(0, (page - 1) * size);
                teachers = teacherMapper.findTeachersBySubjectPagedExactName(subject, realNameExact, offset, size);
            } else {
                Page<Teacher> pageParam = new Page<>(page, size);
                QueryWrapper<Teacher> qw = new QueryWrapper<>();
                qw.eq("is_deleted", false).eq("is_verified", true).eq("real_name", realNameExact).orderByDesc("id");
                Page<Teacher> result = teacherMapper.selectPage(pageParam, qw);
                teachers = result.getRecords();
            }
            return assembleTeacherListResponses(teachers);
        }
        // 默认走原逻辑（支持 keyword 模糊）
        List<Teacher> teachers = getFilteredTeacherList(page, size, subject, keyword);
        return assembleTeacherListResponses(teachers);
    }


    /**
     * 获取精选教师列表（天下名师页面使用）
     */
    @Override
    @Cacheable(cacheNames = "featuredTeachers",
               keyGenerator = "teacherCacheKeyGenerator",
               unless = "#result == null || #result.isEmpty()")
    public List<TeacherListVO> getFeaturedTeachers(int page, int size, String subject, String keyword) {
        // 获取精选教师列表（分页+筛选）
        List<Teacher> teachers = getFeaturedTeacherList(page, size, subject, keyword);
        // 批量装配，消除 N+1
        return assembleTeacherListResponses(teachers);
    }

    @Override
    @Cacheable(cacheNames = "featuredTeachersCount",
               keyGenerator = "teacherCacheKeyGenerator",
               sync = true)
    public long countFeaturedTeachers(String subject, String keyword) {
        // 统计精选教师总数（考虑筛选条件），用于服务端分页 total
        QueryWrapper<Teacher> qw = new QueryWrapper<>();
        qw.eq("is_deleted", false)
          .eq("is_verified", true)
          .eq("is_featured", true);
        if (StringUtils.hasText(subject)) {
            qw.exists("SELECT 1 FROM teacher_subjects ts INNER JOIN subjects s ON ts.subject_id = s.id " +
                     "WHERE ts.teacher_id = teachers.id AND s.name = '" + subject + "' AND s.is_deleted = 0");
        }
        if (StringUtils.hasText(keyword)) {
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

        // 4) 批量查询活跃课程
        List<Course> activeCourses = courseMapper.findActiveByTeacherIds(teacherIds);
        Map<Long, List<Long>> teacherToCourseIds = new HashMap<>();
        for (Course c : activeCourses) {
            teacherToCourseIds.computeIfAbsent(c.getTeacherId(), k -> new ArrayList<>()).add(c.getId());
        }
        Set<Long> courseIds = activeCourses.stream().map(Course::getId).collect(Collectors.toSet());

        // 5) 组装DTO
        List<TeacherListVO> list = new ArrayList<>();
        for (Teacher t : teachers) {
            User user = userMap.get(t.getUserId());
            List<Long> sids = teacherToSubjectIds.getOrDefault(t.getId(), Collections.emptyList());
            List<String> subjectNames = sids.stream()
                    .map(subjectNameMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

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
                    .introduction(shortIntro)
                    .gender(t.getGender())
                    .level(t.getLevel())
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


        // 更新教师真实姓名
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

        // 教师不可修改学历（仅管理员可改）
        if (request.getEducationBackground() != null) {
            log.warn("教师尝试修改学历被忽略, userId={}", userId);
        }
        // 教师不可修改教学经验（仅管理员可改）
        if (request.getTeachingExperience() != null) {
            log.warn("教师尝试修改教学经验被忽略, userId={}", userId);
        }
        if (request.getSpecialties() != null) {
            teacher.setSpecialties(request.getSpecialties());
        }
        // 注意：教师端不可修改时薪（已改为按课程维度设置），科目也仅管理员可改
        if (request.getIntroduction() != null) {
            teacher.setIntroduction(request.getIntroduction());
        }

        // 教师不可修改科目（仅管理员可改）
        if (request.getSubjectIds() != null) {
            log.warn("教师尝试修改科目被忽略, userId={}, subjectIds={}", userId, request.getSubjectIds());
        }
        if (request.getVideoIntroUrl() != null) {
            log.warn("教师尝试修改视频链接被忽略, userId={}", userId);
        }
        // 教师不可修改性别（仅管理员可改）

        // 教师不可修改级别（仅管理员可改）
        if (request.getLevel() != null) {
            log.warn("教师尝试修改级别被忽略, userId={}", userId);
        }
        // 教师可以修改授课地点：supportsOnline 为线上开关；线下地点必须来源于授课地点表且启用
        if (request.getSupportsOnline() != null) {
            teacher.setSupportsOnline(Boolean.TRUE.equals(request.getSupportsOnline()));
        }
        if (request.getTeachingLocationIds() != null) {
            if (request.getTeachingLocationIds().isEmpty()) {
                // 空表示无任何线下地点
                teacher.setTeachingLocations(null);
            } else {
                List<Long> ids = request.getTeachingLocationIds();
                QueryWrapper<TeachingLocation> q = new QueryWrapper<>();
                q.in("id", ids).eq("is_active", true);
                Long cnt = teachingLocationMapper.selectCount(q);
                if (cnt == null || cnt.intValue() != ids.size()) {
                    throw new RuntimeException("存在无效或未启用的授课地点");
                }
                teacher.setTeachingLocations(ids.stream().map(String::valueOf).collect(Collectors.joining(",")));
            }
        }

        if (request.getGender() != null) {
            log.warn("教师尝试修改性别被忽略, userId={}", userId);
        }


        teacherMapper.updateById(teacher);
        log.info("教师更新个人信息成功: userId={}", userId);

        //  weekly availableTimeSlots 6       


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

        // 优先：按学生偏好日期范围进行可用性过滤（仅在有日期范围时生效）
        if (org.springframework.util.StringUtils.hasText(request.getPreferredDateStart())
                && org.springframework.util.StringUtils.hasText(request.getPreferredDateEnd())) {
            try {
                java.time.LocalDate start = java.time.LocalDate.parse(request.getPreferredDateStart());
                java.time.LocalDate end = java.time.LocalDate.parse(request.getPreferredDateEnd());
                if (!end.isBefore(start)) {
                    final java.util.Set<String> preferredSlots = request.getPreferredTimeSlots() != null
                            ? new java.util.HashSet<>(request.getPreferredTimeSlots())
                            : java.util.Collections.emptySet();
                    java.util.List<Teacher> filtered = teachers.stream().filter(t -> {
                        try {
                            java.util.Map<java.time.LocalDate, java.util.List<String>> avail = teacherDailyAvailabilityService.getDailyAvailability(t.getId(), start, end);
                            if (avail == null || avail.isEmpty()) return false;
                            if (preferredSlots.isEmpty()) {
                                // 只要有任意一天有任意基础段可用即可
                                return avail.values().stream().anyMatch(list -> list != null && !list.isEmpty());
                            } else {
                                // 需要与学生偏好基础段有交集
                                return avail.values().stream().filter(list -> list != null && !list.isEmpty())
                                        .anyMatch(list -> list.stream().anyMatch(preferredSlots::contains));
                            }
                        } catch (Exception ex) {
                            return false;
                        }
                    }).collect(java.util.stream.Collectors.toList());
                    if (!filtered.isEmpty()) {
                        teachers = filtered;
                        log.info("按日期范围({}~{})与时段{}过滤后剩余教师数: {}", start, end,
                                preferredSlots.isEmpty() ? "(不限)" : preferredSlots, teachers.size());
                    }
                }
            } catch (Exception e) {
                log.warn("偏好日期范围解析/过滤失败，忽略日期过滤: {}", e.getMessage());
            }
        }

        // 统一在此强制执行“教师级别”硬过滤（若学生指定了级别，则不允许返回其他级别）
        if (org.springframework.util.StringUtils.hasText(request.getTeacherLevel())) {
            final String reqLevel = request.getTeacherLevel().trim();
            teachers = teachers.stream()
                    .filter(t -> t.getLevel() != null && reqLevel.equals(t.getLevel().trim()))
                    .collect(java.util.stream.Collectors.toList());
        }

        // 若严格筛选结果为空，进行有约束的降级：
        // - 若指定了教师级别：仍然保留级别约束，不返回其他级别
        // - 若指定了科目：优先按科目放宽（无级别时）
        if (teachers == null || teachers.isEmpty()) {
            boolean hasSubject = org.springframework.util.StringUtils.hasText(request.getSubject());
            boolean hasLevel = org.springframework.util.StringUtils.hasText(request.getTeacherLevel());
            if (hasLevel) {
                if (hasSubject) {
                    // 放宽为“科目+级别过滤”，不要求已发布课程
                    teachers = teacherMapper.findTeachersBySubject(request.getSubject())
                            .stream().filter(t -> t.getLevel() != null && request.getTeacherLevel().trim().equals(t.getLevel().trim()))
                            .collect(java.util.stream.Collectors.toList());
                } else {
                    // 无科目，仅按级别过滤（不要求已发布课程）
                    teachers = teacherMapper.findAllVerifiedTeachers()
                            .stream().filter(t -> t.getLevel() != null && request.getTeacherLevel().trim().equals(t.getLevel().trim()))
                            .collect(java.util.stream.Collectors.toList());
                }
            } else {
                // 未指定级别时的回退逻辑（不要求已发布课程）
                if (hasSubject) {
                    teachers = teacherMapper.findTeachersBySubject(request.getSubject());
                } else {
                    teachers = teacherMapper.findAllVerifiedTeachers();
                }
            }
        }

        // 转换为响应DTO并计算匹配分数；同分随机展示
        java.util.Comparator<TeacherMatchVO> cmp =
                java.util.Comparator.comparing(TeacherMatchVO::getRecommendationScore)
                        .reversed()
                        .thenComparingInt(x -> java.util.concurrent.ThreadLocalRandom.current().nextInt());

        List<TeacherMatchVO> responses = teachers.stream()
                .filter(teacher -> matchesGenderPreference(teacher, request)) // 添加性别过滤
                .map(teacher -> convertToMatchResponse(teacher, request))
                // 再次按级别做最终兜底过滤，防止任何回退路径带入非指定级别
                .filter(vo -> !org.springframework.util.StringUtils.hasText(request.getTeacherLevel())
                        || (vo.getLevel() != null && request.getTeacherLevel().trim().equals(vo.getLevel().trim())))
                .sorted(cmp)
                .limit(request.getLimit() != null ? Math.max(request.getLimit(), 5) : 5)
                .collect(java.util.stream.Collectors.toList());

        log.info("匹配到 {} 位教师", responses.size());

        return responses;
    }

    /**
     * 优化的教师匹配查询 - 使用JOIN减少数据库查询次数
     */
    private List<Teacher> matchTeachersOptimized(TeacherMatchDTO request) {
        // 智能匹配功能只匹配提供1对1课程的教师

        // 如果指定了科目和教师级别，放宽：不要求已发布课程
        if (StringUtils.hasText(request.getSubject()) && StringUtils.hasText(request.getTeacherLevel())) {
            return teacherMapper.findTeachersBySubjectAndLevel(request.getSubject(), request.getTeacherLevel());
        }

        // 如果只指定了科目，放宽：不要求已发布课程
        if (StringUtils.hasText(request.getSubject())) {
            return teacherMapper.findTeachersBySubject(request.getSubject());
        }

        // 如果都没指定，返回所有已认证教师（包含未发布课程）
        return teacherMapper.findAllVerifiedTeachers();
    }


    /**
     * 将Teacher实体转换为TeacherMatchResponse并计算匹配分数
     */
    private TeacherMatchVO convertToMatchResponse(Teacher teacher, TeacherMatchDTO request) {
        // 日历化后不再返回按星期的可上课时间；卡片仅展示基础信息
        List<String> scheduleDescriptions = java.util.Collections.emptyList();

        // 构建科目名称列表（避免N+1：已在上层查询优化过，这里仅按ID映射名称）
        java.util.List<Long> subjectIdsForMatch = teacherSubjectMapper.getSubjectIdsByTeacherId(teacher.getId());
        java.util.List<String> subjectNamesForMatch = new java.util.ArrayList<>();
        for (Long subjectId : subjectIdsForMatch) {
            try {
                Subject subject = subjectService.getSubjectById(subjectId);
                if (subject != null) {
                    subjectNamesForMatch.add(subject.getName());
                }
            } catch (Exception e) {
                log.warn("获取科目信息失败，科目ID: {}", subjectId, e);
            }
        }

        // 压缩响应体：仅返回前若干个科目用于卡片展示
        int subjectsTotal = subjectNamesForMatch.size();
        final int MAX_SUBJECTS = 4;
        java.util.List<String> subjectsForCard = subjectsTotal > MAX_SUBJECTS
                ? new java.util.ArrayList<>(subjectNamesForMatch.subList(0, MAX_SUBJECTS))
                : subjectNamesForMatch;

        TeacherMatchVO response = TeacherMatchVO.builder()
                .id(teacher.getId())
                .name(teacher.getRealName())
                .subject(getFirstSubject(teacher.getId()))
                .subjects(subjectsForCard)
                .subjectsCount(subjectsTotal)
                .level(teacher.getLevel())
                .experience(teacher.getTeachingExperience() != null ? teacher.getTeachingExperience() : 0)
                .description(teacher.getIntroduction())
                .avatar(null) // 当前Teacher实体没有avatar字段
                .tags(parseTagsToList(teacher.getSpecialties()))
                .schedule(scheduleDescriptions)
                .educationBackground(teacher.getEducationBackground())
                .specialties(teacher.getSpecialties())
                .isVerified(teacher.getVerified())
                .gender(teacher.getGender())
                .build();

        // 计算推荐度和时间匹配度
        log.info("正在计算教师 {} (ID: {}) 的推荐度", teacher.getRealName(), teacher.getId());
        int recommendationScore = calculateRecommendationScore(teacher);
        int timeMatchScore = calculateDailyTimeMatchScore(teacher.getId(), request);
        log.info("教师 {} 的推荐度: 总分={}, 时间匹配度={}", teacher.getRealName(), recommendationScore, timeMatchScore);

        response.setRecommendationScore(recommendationScore);
        // 兼容老前端：同步填充 legacy matchScore 字段
        response.setLegacyMatchScore(recommendationScore);
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

        // 2. 时间匹配 (25分) - 已改为按日历选择，卡片不再计算时间匹配度
        totalScore += 0;

        // 4. 性别匹配 (10分) - 基于学生的性别偏好
        totalScore += calculateGenderMatchScore(teacher, request);

        // 5. 教学经验加成 (10分) - 经验丰富的教师更受欢迎
        totalScore += calculateExperienceBonus(teacher);

        // 确保分数在合理范围内 (40-95)，提供更好的区分度
        return Math.max(40, Math.min(totalScore, 95));
    }


    /**
     * 计算推荐度 (0-100) = 教师评价(50%) + 学历(30%) + 教龄(20%)
     * 教师评价按0-5分换算到百分制；
     * 学历按映射赋分：博士>硕士>本科>大专>其他；
     * 教龄按年限线性归一到上限（默认15年达满分）。
     */
    private int calculateRecommendationScore(Teacher teacher) {
        // 教师评价（0-5）按百分制归一
        double rating = teacher.getRating() != null ? teacher.getRating().doubleValue() : 0.0;
        if (rating < 0) rating = 0.0;
        if (rating > 5.0) rating = 5.0;
        int ratingScore = (int) Math.round((rating / 5.0) * 100);

        // 学历点数：专科及以下=1，本科=2，硕士=3，博士=4 -> 百分制
        int eduPoint = getEducationPoint(teacher.getEducationBackground());
        int educationScore = (int) Math.round((eduPoint / 4.0) * 100);

        // 教龄点数：N≤3=1，3<N≤5=2，N>5=3 -> 百分制
        int expPoint = getExperiencePoint(teacher.getTeachingExperience());
        int experienceScore = (int) Math.round((expPoint / 3.0) * 100);

        // 加权：教师评价50% + 学历30% + 教龄20%
        double weighted = ratingScore * 0.5 + educationScore * 0.3 + experienceScore * 0.2;
        int total = (int) Math.round(weighted);
        return Math.max(0, Math.min(100, total));
    }

    // 学历点数：博士4，硕士3，本科2，专科及以下1
    private int getEducationPoint(String educationBackground) {
        if (educationBackground == null || educationBackground.trim().isEmpty()) {
            return 1;
        }
        String edu = educationBackground.trim().toLowerCase();
        if (edu.contains("博士") || edu.contains("phd") || edu.contains("doctor")) return 4;
        if (edu.contains("硕士") || edu.contains("master") || edu.contains("msc")) return 3;
        if (edu.contains("本科") || edu.contains("学士") || edu.contains("bachelor") || edu.contains("bsc")) return 2;
        // 专科/大专/高中/中专/associate 归为1
        return 1;
    }

    // 教龄点数：≤3年=1；3<≤5年=2；>5年=3
    private int getExperiencePoint(Integer years) {
        int y = years == null ? 0 : years;
        if (y <= 3) return 1;
        if (y <= 5) return 2;
        return 3;
    }

    /**
     * 学历转百分制评分
     */
    private int getEducationScore(String educationBackground) {
        if (educationBackground == null || educationBackground.trim().isEmpty()) {
            return 60; // 未填写给及格分
        }
        String edu = educationBackground.trim().toLowerCase();
        // 常见中英文映射
        if (edu.contains("博士") || edu.contains("phd") || edu.contains("doctor")) {
            return 100;
        }
        if (edu.contains("硕士") || edu.contains("master") || edu.contains("msc")) {
            return 85;
        }
        if (edu.contains("本科") || edu.contains("学士") || edu.contains("bachelor") || edu.contains("bsc")) {
            return 75;
        }
        if (edu.contains("大专") || edu.contains("专科") || edu.contains("associate")) {
            return 65;
        }
        if (edu.contains("高中") || edu.contains("中专")) {
            return 55;
        }
        return 60; // 其他/无法识别，给中间分
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

        // 获取指定时间范围内的课程安排（一次 DB）→ 使用新表
        List<CourseSchedule> schedules = courseScheduleMapper.findByTeacherIdAndDateRange(teacherId, startDate, endDate);

        // 按日期分组
        Map<LocalDate, List<CourseSchedule>> schedulesByDate = schedules.stream()
                .collect(Collectors.groupingBy(CourseSchedule::getScheduledDate));

        // 基于本次 DB 结果构建 busy 映射，并批量回写 Redis（避免逐日多次往返）
        Map<LocalDate, List<String>> busyMap = new HashMap<>();
        for (CourseSchedule s : schedules) {
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
            List<String> busySlots = busyMap.getOrDefault(currentDate, Collections.emptyList());
            List<CourseSchedule> daySchedules_raw = schedulesByDate.getOrDefault(currentDate, new ArrayList<>());

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


    /**
     * 生成时间段信息 - 根据教师设置的可上课时间和实际课表安排
     */
    private List<TeacherScheduleVO.TimeSlotInfo> generateTimeSlotsWithBusy(Long teacherId, List<CourseSchedule> daySchedules, LocalDate date, List<String> busySlots) {
        // 获取该日期对应的星期几（ISO标准：1=周一, 7=周日）
        int weekday = date.getDayOfWeek().getValue();

        // 获取教师在该具体日期的可上课时间段（按日历）
        Map<LocalDate, List<String>> daily = teacherDailyAvailabilityService.getDailyAvailability(teacherId, date, date);
        List<String> availableTimeSlots = daily.getOrDefault(date, Collections.emptyList());
        if (availableTimeSlots.isEmpty()) {
            return new ArrayList<>();
        }

        List<TeacherScheduleVO.TimeSlotInfo> timeSlots = new ArrayList<>();

        // 将已有课程按时间段分组（新表）
        Map<String, CourseSchedule> scheduleByTimeSlot = new HashMap<>();
        for (CourseSchedule cs : daySchedules) {
            String timeSlot = cs.getStartTime().toString() + "-" + cs.getEndTime().toString();
            scheduleByTimeSlot.put(timeSlot, cs);
        }

        // 只生成教师可上课时间段的信息
        for (String timeSlot : availableTimeSlots) {
            String[] times = timeSlot.split("-");
            LocalTime startTime = LocalTime.parse(times[0]);
            LocalTime endTime = LocalTime.parse(times[1]);

            boolean isBooked = (busySlots != null && !busySlots.isEmpty())
                    ? busySlots.contains(timeSlot)
                    : scheduleByTimeSlot.containsKey(timeSlot);

            TeacherScheduleVO.TimeSlotInfo timeSlotInfo = TeacherScheduleVO.TimeSlotInfo.builder()
                    .timeSlot(timeSlot)
                    .startTime(startTime)
                    .endTime(endTime)
                    .available(!isBooked)
                    .booked(isBooked)
                    .build();

            // 新表路径：如需补充学生/课程/状态，可通过 enrollment 反查；为降低开销，这里仅标注占用，不回填详情

            timeSlots.add(timeSlotInfo);
        }

        return timeSlots;
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

    // 已移除：按星期 weekly 可上课时间查询（统一改为按日历 daily）

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

        // 标准化学生偏好时间段：若未选择，则默认采用上午/下午/晚上六个基础段
        java.util.List<String> preferredTimeSlots = request.getPreferredTimeSlots();
        if (preferredTimeSlots == null || preferredTimeSlots.isEmpty()) {
            preferredTimeSlots = java.util.Arrays.asList(
                "08:00-10:00", "10:00-12:00",
                "13:00-15:00", "15:00-17:00",
                "17:00-19:00", "19:00-21:00"
            );
            log.info("学生未选择偏好上课时间，默认使用上午/下午/晚上六个基础段: {}", preferredTimeSlots);
        }

        int totalScore = 0;
        int maxPossibleScore = 0;

        // 处理时间段匹配 - 必须同时匹配星期几和时间段
        if (preferredTimeSlots != null && !preferredTimeSlots.isEmpty()) {
            // 获取学生偏好的星期几
            Set<Integer> studentPreferredWeekdays = new HashSet<>();
            if (request.getPreferredWeekdays() != null && !request.getPreferredWeekdays().isEmpty()) {
                studentPreferredWeekdays.addAll(request.getPreferredWeekdays());
            }

            for (String preferredTimeSlot : preferredTimeSlots) {
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
     * 基于“日历每天”计算时间匹配度：在指定日期范围内，按学生偏好基础时段逐日校验教师是否可上课且无已排程冲突
     * 评分=可用(无冲突)的(日期×时段)组合 / 总组合 × 100
     */
    private int calculateDailyTimeMatchScore(Long teacherId, TeacherMatchDTO request) {
        try {
            String ds = request.getPreferredDateStart();
            String de = request.getPreferredDateEnd();

            // 若学生未选择开始/结束日期，则默认使用“今天起未来三个月”
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate start = org.springframework.util.StringUtils.hasText(ds) ? java.time.LocalDate.parse(ds) : today;
            java.time.LocalDate end = org.springframework.util.StringUtils.hasText(de) ? java.time.LocalDate.parse(de) : start.plusMonths(3);

            if (end.isBefore(start)) {
                // 若传入非法（结束早于开始），兜底为从开始起三个月
                end = start.plusMonths(3);
            }
            // 限制最大评估跨度，避免性能问题（最多约三个月）
            java.time.LocalDate maxEnd = start.plusMonths(3);
            if (end.isAfter(maxEnd)) end = maxEnd;

            java.util.List<String> wanted = request.getPreferredTimeSlots();
            if (wanted == null || wanted.isEmpty()) {
                // 未指定则默认使用6个基础段
                wanted = java.util.Arrays.asList("08:00-10:00", "10:00-12:00", "13:00-15:00", "15:00-17:00", "17:00-19:00", "19:00-21:00");
            }

            // 读取教师在日期范围内的“日历可上课基础段”设置
            java.util.Map<java.time.LocalDate, java.util.List<String>> daily = teacherDailyAvailabilityService.getDailyAvailability(teacherId, start, end);
            // 读取该教师在范围内的已排程课程（含试听/正式）
            java.util.List<CourseSchedule> schedules = courseScheduleMapper.findByTeacherIdAndDateRange(teacherId, start, end);
            java.util.Map<java.time.LocalDate, java.util.List<CourseSchedule>> schByDate = schedules.stream()
                    .collect(java.util.stream.Collectors.groupingBy(CourseSchedule::getScheduledDate));

            // 按周聚合：一周内只要有任意一天存在“开放且无冲突”的任一偏好时间段，即记为该周命中
            java.util.Set<Integer> weekKeys = new java.util.LinkedHashSet<>();
            java.util.Set<Integer> matchedWeeks = new java.util.HashSet<>();

            java.time.LocalDate cur = start;
            while (!cur.isAfter(end)) {
                // 计算 ISO 周：year-week 作为 key
                java.time.temporal.WeekFields wf = java.time.temporal.WeekFields.ISO;
                int week = cur.get(wf.weekOfWeekBasedYear());
                int weekYear = cur.get(wf.weekBasedYear());
                int weekKey = weekYear * 100 + week;
                weekKeys.add(weekKey);

                java.util.List<String> allow = daily.getOrDefault(cur, java.util.Collections.emptyList());
                if (allow != null && !allow.isEmpty()) {
                    java.util.List<CourseSchedule> daySch = schByDate.getOrDefault(cur, java.util.Collections.emptyList());
                    // 只要当天有任一偏好基础段开放且无排程冲突，就标记该周命中
                    outer:
                    for (String slot : wanted) {
                        if (!allow.contains(slot)) continue;
                        java.time.LocalTime[] tt = parseBaseSlot(slot);
                        if (tt == null) continue;
                        java.time.LocalTime baseStart = tt[0];
                        java.time.LocalTime baseEnd = tt[1];
                        boolean conflict = false;
                        for (CourseSchedule cs : daySch) {
                            if (cs.getStartTime() != null && cs.getEndTime() != null &&
                                    cs.getStartTime().isBefore(baseEnd) && baseStart.isBefore(cs.getEndTime())) {
                                conflict = true; break;
                            }
                        }
                        if (!conflict) { matchedWeeks.add(weekKey); break outer; }
                    }
                }
                cur = cur.plusDays(1);
            }

            int totalWeeks = weekKeys.size();
            if (totalWeeks <= 0) return 0;
            int score = (int) Math.round((matchedWeeks.size() * 100.0) / totalWeeks);
            return Math.max(0, Math.min(100, score));
        } catch (Exception e) {
            log.warn("calculateDailyTimeMatchScore 失败: {}", e.getMessage());
            return 0;
        }
    }

    // 解析基础时间段 "HH:mm-HH:mm"
    private static java.time.LocalTime[] parseBaseSlot(String slot) {
        if (slot == null) return null;
        String[] arr = slot.split("-");
        if (arr.length != 2) return null;
        try {
            java.time.LocalTime s = java.time.LocalTime.parse(arr[0].trim());
            java.time.LocalTime e = java.time.LocalTime.parse(arr[1].trim());
            if (!s.isBefore(e)) return null;
            return new java.time.LocalTime[]{s, e};
        } catch (Exception ignore) {
            return null;
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
     * 获取过滤后的教师列表（支持科目筛选）
     */
    private List<Teacher> getFilteredTeacherList(int page, int size, String subject, String keyword) {
        return getTeacherList(page, size, subject, keyword);
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

        // 3. 即将上课数 - 查询course_schedules中scheduled状态且日期>=今天的安排
        Long upcomingClasses = (long) courseScheduleMapper.countUpcomingByTeacherId(teacher.getId(), LocalDate.now());

        // 4. 预约申请数 - 查询状态为pending的预约申请
        QueryWrapper<BookingRequest> bookingWrapper = new QueryWrapper<>();
        bookingWrapper.eq("teacher_id", teacher.getId());
        bookingWrapper.eq("status", "pending");
        bookingWrapper.eq("is_deleted", false);
        Long bookingRequests = bookingRequestMapper.selectCount(bookingWrapper);

        // 5. 本月调课/请假次数 - 查询教师本月发起的调课申请
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());

        QueryWrapper<RescheduleRequest> monthlyRescheduleWrapper = new QueryWrapper<>();
        monthlyRescheduleWrapper.eq("applicant_id", teacher.getId());
        monthlyRescheduleWrapper.eq("applicant_type", "teacher");
        monthlyRescheduleWrapper.ge("created_at", startOfMonth.atStartOfDay());
        monthlyRescheduleWrapper.le("created_at", endOfMonth.atTime(23, 59, 59));
        monthlyRescheduleWrapper.eq("is_deleted", false);
        Long monthlyRescheduleCount = rescheduleRequestMapper.selectCount(monthlyRescheduleWrapper);

        // 6. 本月课时和上月课时（改为按一对一课程汇总）
        BigDecimal currentHours = courseMapper.sumCurrentHoursByTeacher(teacher.getId());
        BigDecimal lastHours = courseMapper.sumLastHoursByTeacher(teacher.getId());

        // 7. 基于“课程专属时薪(一对一)”计算当月/上月收入（完成课节）
        try {
            // 当月范围
            LocalDateTime curStart = startOfMonth.atStartOfDay();
            LocalDateTime curEnd = endOfMonth.atTime(23, 59, 59);

            // 上月范围
            LocalDate lastMonth = now.minusMonths(1);
            LocalDate lastStartOfMonth = lastMonth.withDayOfMonth(1);
            LocalDate lastEndOfMonth = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth());

            // 拉取两个月内课表（含课程ID与单次时长）
            java.util.List<com.touhouqing.grabteacherbackend.model.entity.CourseSchedule> curList = courseScheduleMapper.findByTeacherIdAndDateRange(teacher.getId(), startOfMonth, endOfMonth);
            java.util.List<com.touhouqing.grabteacherbackend.model.entity.CourseSchedule> lastList = courseScheduleMapper.findByTeacherIdAndDateRange(teacher.getId(), lastStartOfMonth, lastEndOfMonth);

            // 收集课程ID并批量查询课程的教师时薪
            java.util.Set<Long> courseIds = new java.util.HashSet<>();
            for (var cs : curList) { if (cs.getCourseId() != null) courseIds.add(cs.getCourseId()); }
            for (var cs : lastList) { if (cs.getCourseId() != null) courseIds.add(cs.getCourseId()); }
            java.util.Map<Long, java.math.BigDecimal> courseRateMap = new java.util.HashMap<>();
            if (!courseIds.isEmpty()) {
                java.util.List<com.touhouqing.grabteacherbackend.model.entity.Course> courses = courseMapper.selectBatchIds(new java.util.ArrayList<>(courseIds));
                for (var c : courses) {
                    courseRateMap.put(c.getId(), c.getTeacherHourlyRate());
                }
            }
            java.math.BigDecimal fallbackRate = java.math.BigDecimal.ZERO;

            java.util.function.Function<java.util.List<com.touhouqing.grabteacherbackend.model.entity.CourseSchedule>, java.math.BigDecimal> calcSum = (list) -> {
                java.math.BigDecimal sum = java.math.BigDecimal.ZERO;
                for (var cs : list) {
                    if (!"completed".equalsIgnoreCase(cs.getScheduleStatus())) continue; // 仅完成的课节计入收入
                    Integer mins = cs.getDurationMinutes();
                    if (mins == null || mins <= 0) {
                        // 兜底：用时间差计算
                        if (cs.getStartTime() != null && cs.getEndTime() != null) {
                            int m = java.time.Duration.between(cs.getStartTime(), cs.getEndTime()).toMinutesPart();
                            mins = m > 0 ? m : 0;
                        } else {
                            continue;
                        }
                    }
                    java.math.BigDecimal hours = new java.math.BigDecimal(mins).divide(new java.math.BigDecimal(60), 4, java.math.RoundingMode.HALF_UP);
                    java.math.BigDecimal rate = fallbackRate;
                    if (cs.getCourseId() != null) {
                        java.math.BigDecimal r = courseRateMap.get(cs.getCourseId());
                        if (r != null) rate = r;
                    }
                    if (rate.compareTo(java.math.BigDecimal.ZERO) > 0) {
                        sum = sum.add(rate.multiply(hours));
                    }
                }
                return sum.setScale(2, java.math.RoundingMode.HALF_UP);
            };

            java.math.BigDecimal currentEarnings = calcSum.apply(curList);
            java.math.BigDecimal lastEarnings = calcSum.apply(lastList);

            statistics.put("currentEarnings", currentEarnings);
            statistics.put("lastEarnings", lastEarnings);
        } catch (Exception e) {
            log.warn("统计教师收入失败，降级为0: userId={}, err={}", userId, e.toString());
            statistics.put("currentEarnings", java.math.BigDecimal.ZERO);
            statistics.put("lastEarnings", java.math.BigDecimal.ZERO);
        }

        statistics.put("rescheduleRequests", rescheduleRequestsCount);
        statistics.put("totalCourses", totalCourses != null ? totalCourses.intValue() : 0);
        statistics.put("upcomingClasses", upcomingClasses != null ? upcomingClasses.intValue() : 0);
        statistics.put("bookingRequests", bookingRequests != null ? bookingRequests.intValue() : 0);
        statistics.put("monthlyRescheduleCount", monthlyRescheduleCount != null ? monthlyRescheduleCount.intValue() : 0);
        statistics.put("currentHours", currentHours);
        statistics.put("lastHours", lastHours);

        log.info("获取教师统计数据成功: userId={}, statistics={}", userId, statistics);

        return statistics;
    }

    /**
     * 获取教师课时详情统计
     */
    @Override
    public Map<String, Object> getHourDetailsSummary(Long userId) {
        Map<String, Object> summary = new HashMap<>();

        // 获取教师信息
        Teacher teacher = getTeacherByUserId(userId);
        if (teacher == null) {
            throw new RuntimeException("教师信息不存在");
        }

        // 获取本月时间范围
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
        LocalDateTime startDateTime = startOfMonth.atStartOfDay();
        LocalDateTime endDateTime = endOfMonth.atTime(23, 59, 59);

        // 1. 正常上课的课时数 - 查询课程完成自动结算的记录
        QueryWrapper<HourDetail> normalHoursWrapper = new QueryWrapper<>();
        normalHoursWrapper.eq("user_id", userId);
        normalHoursWrapper.ge("created_at", startDateTime);
        normalHoursWrapper.le("created_at", endDateTime);
        normalHoursWrapper.and(q -> q
                .eq("reason_code", com.touhouqing.grabteacherbackend.model.entity.HourDetail.REASON_CODE_LESSON_COMPLETED_AUTO)
                .or()
                .eq("reason", "课程完成自动结算")
        );
        List<HourDetail> normalHoursList = hourDetailMapper.selectList(normalHoursWrapper);
        BigDecimal normalHours = normalHoursList.stream()
                .map(HourDetail::getHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. 教师超出调课/请假次数后所扣课时 - 兼容当前文案："教师超额…扣减"
        QueryWrapper<HourDetail> teacherDeductionWrapper = new QueryWrapper<>();
        teacherDeductionWrapper.eq("user_id", userId);
        teacherDeductionWrapper.ge("created_at", startDateTime);
        teacherDeductionWrapper.le("created_at", endDateTime);
        teacherDeductionWrapper.and(q -> q
                .eq("reason_code", com.touhouqing.grabteacherbackend.model.entity.HourDetail.REASON_CODE_TEACHER_OVER_QUOTA_DEDUCTION)
                .or(w -> w.like("reason", "教师超额").like("reason", "扣减"))
        );
        List<HourDetail> teacherDeductionList = hourDetailMapper.selectList(teacherDeductionWrapper);
        BigDecimal teacherDeduction = teacherDeductionList.stream()
                .map(HourDetail::getHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. 学生超出调课/请假次数的补偿课时 - 兼容当前文案："学生超额…补偿"（包含调课/请假两类）
        QueryWrapper<HourDetail> studentCompensationWrapper = new QueryWrapper<>();
        studentCompensationWrapper.eq("user_id", userId);
        studentCompensationWrapper.ge("created_at", startDateTime);
        studentCompensationWrapper.le("created_at", endDateTime);
        studentCompensationWrapper.and(q -> q
                .eq("reason_code", com.touhouqing.grabteacherbackend.model.entity.HourDetail.REASON_CODE_STUDENT_OVER_QUOTA_COMPENSATION)
                .or(w -> w.eq("transaction_type", 1).like("reason", "学生超额").like("reason", "补偿"))
        );
        List<HourDetail> studentCompensationList = hourDetailMapper.selectList(studentCompensationWrapper);
        BigDecimal studentCompensation = studentCompensationList.stream()
                .map(HourDetail::getHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. Admin手动调整记录 - 查询有操作员ID的记录
        QueryWrapper<HourDetail> adminAdjustmentWrapper = new QueryWrapper<>();
        adminAdjustmentWrapper.eq("user_id", userId);
        adminAdjustmentWrapper.isNotNull("operator_id");
        adminAdjustmentWrapper.ge("created_at", startDateTime);
        adminAdjustmentWrapper.le("created_at", endDateTime);
        List<HourDetail> adminAdjustmentList = hourDetailMapper.selectList(adminAdjustmentWrapper);
        BigDecimal adminAdjustment = adminAdjustmentList.stream()
                .map(HourDetail::getHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 最近10条管理员手动调整记录（本月）
        QueryWrapper<HourDetail> adminAdjustmentTop10Wrapper = new QueryWrapper<>();
        adminAdjustmentTop10Wrapper.eq("user_id", userId)
                .isNotNull("operator_id")
                .ge("created_at", startDateTime)
                .le("created_at", endDateTime)
                .orderByDesc("created_at")
                .last("LIMIT 10");
        List<HourDetail> adminAdjustTop10 = hourDetailMapper.selectList(adminAdjustmentTop10Wrapper);
        List<Map<String, Object>> adminAdjustList = new java.util.ArrayList<>();
        for (HourDetail hd : adminAdjustTop10) {
            Map<String, Object> row = new HashMap<>();
            row.put("createdAt", hd.getCreatedAt());
            row.put("hours", hd.getHours());
            row.put("reason", hd.getReason());
            adminAdjustList.add(row);
        }
        summary.put("adminAdjustList", adminAdjustList);


        // 5. 总课时 = 正常课时 + 学生补偿 - 教师扣除 + 管理员调整
        BigDecimal totalHours = normalHours.add(studentCompensation).subtract(teacherDeduction).add(adminAdjustment);

        summary.put("normalHours", normalHours);
        summary.put("teacherDeduction", teacherDeduction.abs()); // 取绝对值显示
        summary.put("studentCompensation", studentCompensation);
        summary.put("adminAdjustment", adminAdjustment);
        summary.put("totalHours", totalHours);

        // 新增：按课程聚合（仅一对一），输出本月/上月课时与薪资
        try {
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.touhouqing.grabteacherbackend.model.entity.Course> cqw = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            cqw.eq("teacher_id", teacher.getId()).eq("course_type", "one_on_one").eq("is_deleted", false);
            java.util.List<com.touhouqing.grabteacherbackend.model.entity.Course> courses = courseMapper.selectList(cqw);
            java.util.List<java.util.Map<String, Object>> courseSummaries = new java.util.ArrayList<>();
            java.math.BigDecimal tCurH = java.math.BigDecimal.ZERO;
            java.math.BigDecimal tLastH = java.math.BigDecimal.ZERO;
            java.math.BigDecimal tCurAmt = java.math.BigDecimal.ZERO;
            java.math.BigDecimal tLastAmt = java.math.BigDecimal.ZERO;
            for (var c : courses) {
                java.math.BigDecimal ch = c.getCurrentHours() != null ? c.getCurrentHours() : java.math.BigDecimal.ZERO;
                java.math.BigDecimal lh = c.getLastHours() != null ? c.getLastHours() : java.math.BigDecimal.ZERO;
                java.math.BigDecimal rate = c.getTeacherHourlyRate() != null ? c.getTeacherHourlyRate() : java.math.BigDecimal.ZERO;
                java.math.BigDecimal curAmt = rate.multiply(ch).setScale(2, java.math.RoundingMode.HALF_UP);
                java.math.BigDecimal lastAmt = rate.multiply(lh).setScale(2, java.math.RoundingMode.HALF_UP);
                java.util.Map<String, Object> row = new java.util.HashMap<>();
                row.put("courseId", c.getId());
                row.put("title", c.getTitle());
                row.put("teacherHourlyRate", rate);
                row.put("currentHours", ch);
                row.put("lastHours", lh);
                row.put("currentAmount", curAmt);
                row.put("lastAmount", lastAmt);
                courseSummaries.add(row);
                tCurH = tCurH.add(ch);
                tLastH = tLastH.add(lh);
                tCurAmt = tCurAmt.add(curAmt);
                tLastAmt = tLastAmt.add(lastAmt);
            }
            summary.put("courseSummaries", courseSummaries);
            summary.put("totalCurrentHoursByCourses", tCurH);
            summary.put("totalLastHoursByCourses", tLastH);
            summary.put("totalCurrentAmount", tCurAmt);
            summary.put("totalLastAmount", tLastAmt);
        } catch (Exception e) {
            log.warn("按课程聚合月课时/薪资失败，降级为空: userId={}, err={}", userId, e.toString());
        }

        log.info("获取教师课时详情统计成功: userId={}, summary={}", userId, summary);

        return summary;
    }

    /**
     * 获取精选教师列表（支持科目筛选）
     */
    private List<Teacher> getFeaturedTeacherList(int page, int size, String subject, String keyword) {
        return getFeaturedTeacherListSimple(page, size, subject, keyword);

    }

    /**
     * 获取精选教师列表（简化版）
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

    @Override
    public Page<ClassRecordVO> getClassRecords(Long userId, int page, int size, Integer year, Integer month, String studentName, String courseName) {
        // 获取教师信息
        Teacher teacher = getTeacherByUserId(userId);
        if (teacher == null) {
            throw new RuntimeException("教师信息不存在");
        }

        // 执行查询
        List<ClassRecordVO> records = courseScheduleMapper.selectClassRecords(
                teacher.getId(),
                year,
                month,
                StringUtils.hasText(studentName) ? studentName : null,
                StringUtils.hasText(courseName) ? courseName : null
        );

        // 手动分页
        int total = records.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);

        List<ClassRecordVO> pageRecords = records.subList(start, end);

        // 构建分页结果
        Page<ClassRecordVO> result = new Page<>(page, size);
        result.setRecords(pageRecords);
        result.setTotal(total);
        result.setPages((long) Math.ceil((double) total / size));

        return result;
    }
}
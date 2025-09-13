package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.model.dto.CourseDTO;
import com.touhouqing.grabteacherbackend.model.vo.CourseVO;
import com.touhouqing.grabteacherbackend.model.entity.Course;
import com.touhouqing.grabteacherbackend.model.entity.Teacher;
import com.touhouqing.grabteacherbackend.model.entity.Subject;
import com.touhouqing.grabteacherbackend.mapper.CourseMapper;
import com.touhouqing.grabteacherbackend.mapper.TeacherMapper;
import com.touhouqing.grabteacherbackend.mapper.SubjectMapper;
import com.touhouqing.grabteacherbackend.service.CourseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.context.ApplicationEventPublisher;

import com.touhouqing.grabteacherbackend.event.CourseChangedEvent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@CacheConfig(cacheNames = "course")
public class CourseServiceImpl implements CourseService {

    @Autowired
    private CourseMapper courseMapper;


    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private SubjectMapper subjectMapper;

    @Autowired
    private com.touhouqing.grabteacherbackend.mapper.TeachingLocationMapper teachingLocationMapper;

    @Autowired
    private com.touhouqing.grabteacherbackend.util.AliyunOssUtil ossUtil;

    @Autowired
    private ApplicationEventPublisher eventPublisher;


    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = "course", allEntries = true),
        @CacheEvict(cacheNames = "courseList", allEntries = true),
        @CacheEvict(cacheNames = "teacherCourses", allEntries = true),
        @CacheEvict(cacheNames = "activeCourses", allEntries = true),
        @CacheEvict(cacheNames = "activeCoursesAll", allEntries = true),
        @CacheEvict(cacheNames = "activeCoursesLimited", allEntries = true),
        @CacheEvict(cacheNames = "teacherList", allEntries = true),
        @CacheEvict(cacheNames = "featuredCourses", allEntries = true),
        @CacheEvict(cacheNames = "featuredCourseIds", allEntries = true)
    })
    public Course createCourse(CourseDTO request, Long currentUserId, String userType) {
        log.info("创建课程，用户ID: {}, 用户类型: {}", currentUserId, userType);

        // 验证科目是否存在
        Subject subject = subjectMapper.selectById(request.getSubjectId());
        if (subject == null || subject.getDeleted()) {
            throw new RuntimeException("科目不存在");
        }

        Long teacherId;
        if ("admin".equals(userType)) {
            // 管理员可以为任何教师创建课程
            if (request.getTeacherId() == null) {
                throw new RuntimeException("管理员创建课程时必须指定教师ID");
            }
            teacherId = request.getTeacherId();

            // 验证教师是否存在
            Teacher teacher = teacherMapper.selectById(teacherId);
            if (teacher == null || teacher.getDeleted()) {
                throw new RuntimeException("指定的教师不存在");
            }
        } else if ("teacher".equals(userType)) {
            // 教师只能为自己创建课程
            QueryWrapper<Teacher> teacherQuery = new QueryWrapper<>();
            teacherQuery.eq("user_id", currentUserId);
            teacherQuery.eq("is_deleted", false);
            Teacher teacher = teacherMapper.selectOne(teacherQuery);

            if (teacher == null) {
                throw new RuntimeException("教师信息不存在，请先完善教师资料");
            }
            teacherId = teacher.getId();
        } else {
            throw new RuntimeException("只有教师和管理员可以创建课程");
        }

        // 业务约束：每位教师仅允许一个课程（未删除）
        {
            QueryWrapper<Course> q = new QueryWrapper<>();
            q.eq("teacher_id", teacherId).eq("is_deleted", false);
            Long exist = courseMapper.selectCount(q);
            if (exist != null && exist > 0) {
                throw new RuntimeException("该教师已存在课程，每位教师仅允许一个课程。如需变更请先删除或下架原课程");
            }
        }

        // 验证课程类型
        if (!isValidCourseType(request.getCourseType())) {
            throw new RuntimeException("无效的课程类型");
        }

        // 验证课程状态
        if (request.getStatus() != null && !isValidCourseStatus(request.getStatus())) {
            throw new RuntimeException("无效的课程状态");
        }

        // 验证大班课专用字段
        validateLargeClassFields(request);

        // 课程时长：仅大班课需要；一对一不需要，预约时由学生选择
        Integer finalDuration = null;
        if ("large_class".equals(request.getCourseType())) {
            Integer durationMinutes = request.getDurationMinutes();
            if (durationMinutes == null) {
                throw new RuntimeException("大班课必须设置课程时长（90或120分钟）");
            }
            if (durationMinutes != 90 && durationMinutes != 120) {
                throw new RuntimeException("课程时长只能选择90分钟（一个半小时）或120分钟（俩小时）");
            }
            finalDuration = durationMinutes;
        }

        // 确定课程状态：教师创建的课程默认为pending，管理员创建的课程可以直接设置状态
        String courseStatus;
        if ("teacher".equals(userType)) {
            // 教师创建的课程需要审批
            courseStatus = "pending";
        } else if ("admin".equals(userType)) {
            // 管理员创建的课程可以直接设置状态，默认为active
            courseStatus = request.getStatus() != null ? request.getStatus() : "active";
        } else {
            courseStatus = "pending";
        }

        // 课程地点约束：一对一不需要设置；大班课需二选一（线上 或 线下且选定地点）
        String finalCourseLocation = null;
        if ("one_on_one".equals(request.getCourseType())) {
            // 一对一课程不需要设置课程地点
            finalCourseLocation = null;
        } else {
            Long offlineId = request.getOfflineLocationId();
            boolean reqOnline = Boolean.TRUE.equals(request.getSupportsOnline());
            if (offlineId != null) {
                var loc = teachingLocationMapper.selectById(offlineId);
                if (loc == null || !Boolean.TRUE.equals(loc.getIsActive())) {
                    throw new RuntimeException("线下地点无效或未激活");
                }
                // 方案A：仅存地点名称，保持与地点表一致
                finalCourseLocation = loc.getName();
            } else if (reqOnline) {
                finalCourseLocation = "线上";
            } else {
                // 兼容老前端仅传 courseLocation 的情况
                String locStr = request.getCourseLocation();
                if ("线上".equals(locStr)) {
                    finalCourseLocation = "线上";
                } else if ("线下".equals(locStr)) {
                    throw new RuntimeException("请选择线下授课地点");
                } else {
                    throw new RuntimeException("大班课必须选择线上或线下地点");
                }
            }
        }

        Course course = Course.builder()
                .teacherId(teacherId)
                .subjectId(request.getSubjectId())
                .title(request.getTitle())
                .description(request.getDescription())
                .courseType(request.getCourseType())
                .durationMinutes(finalDuration)
                .status(courseStatus)
                .deleted(false)
                .imageUrl(request.getImageUrl())
                .price(request.getPrice()) // 所有课程类型都可以设置价格
                .build();
        course.setCourseLocation(finalCourseLocation);

        // 设置大班课专用字段
        if ("large_class".equals(request.getCourseType())) {
            course.setStartDate(request.getStartDate());
            course.setEndDate(request.getEndDate());
            course.setPersonLimit(request.getPersonLimit());
            // 处理课时周期 JSON
            if (request.getCourseTimeSlots() != null && !request.getCourseTimeSlots().isEmpty()) {
                if (com.touhouqing.grabteacherbackend.util.TimeSlotUtil.isValidTimeSlots(request.getCourseTimeSlots())) {
                    course.setCourseTimeSlots(com.touhouqing.grabteacherbackend.util.TimeSlotUtil.toJsonString(request.getCourseTimeSlots()));
                } else {
                    throw new RuntimeException("上课时间安排格式不正确");
                }
            } else {
                // 允许为空：表示未设置固定每周时间
                course.setCourseTimeSlots(null);
            }
        }

        courseMapper.insert(course);


        log.info("课程创建成功: {}", course.getTitle());
        try { eventPublisher.publishEvent(new CourseChangedEvent(this, CourseChangedEvent.ChangeType.CREATE)); } catch (Exception ignore) {}
        return course;
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = "course", key = "#id"),
        @CacheEvict(cacheNames = "courseList", allEntries = true),
        @CacheEvict(cacheNames = "teacherCourses", allEntries = true),
        @CacheEvict(cacheNames = "activeCourses", allEntries = true),
        @CacheEvict(cacheNames = "activeCoursesAll", allEntries = true),
        @CacheEvict(cacheNames = "activeCoursesLimited", allEntries = true),
        @CacheEvict(cacheNames = "teacherList", allEntries = true),
        @CacheEvict(cacheNames = "featuredCourses", allEntries = true),
        @CacheEvict(cacheNames = "featuredCourseIds", allEntries = true)
    })
    public Course updateCourse(Long id, CourseDTO request, Long currentUserId, String userType) {
        log.info("更新课程，课程ID: {}, 用户ID: {}, 用户类型: {}", id, currentUserId, userType);

        Course course = courseMapper.selectById(id);
        if (course == null || course.getDeleted()) {
            throw new RuntimeException("课程不存在");
        }

        // 权限检查
        if (!hasPermissionToManageCourse(id, currentUserId, userType)) {
            throw new RuntimeException("没有权限操作此课程");
        }

            // 业务约束：每位教师仅允许一个课程（排除当前课程）
            {
                QueryWrapper<Course> q = new QueryWrapper<>();
                q.eq("teacher_id", course.getTeacherId()).eq("is_deleted", false).ne("id", course.getId());
                Long exist = courseMapper.selectCount(q);
                if (exist != null && exist > 0) {
                    throw new RuntimeException("该教师已存在其他课程，每位教师仅允许一个课程");
                }
            }

        // 验证科目是否存在
        Subject subject = subjectMapper.selectById(request.getSubjectId());
        if (subject == null || subject.getDeleted()) {
            throw new RuntimeException("科目不存在");
        }

        // 管理员可以修改教师，教师不能修改
        if ("admin".equals(userType) && request.getTeacherId() != null) {
            Teacher teacher = teacherMapper.selectById(request.getTeacherId());
            if (teacher == null || teacher.getDeleted()) {
                throw new RuntimeException("指定的教师不存在");
            }
            course.setTeacherId(request.getTeacherId());
        }

        // 验证课程类型和状态
        if (!isValidCourseType(request.getCourseType())) {
            throw new RuntimeException("无效的课程类型");
        }
        if (request.getStatus() != null && !isValidCourseStatus(request.getStatus())) {
            throw new RuntimeException("无效的课程状态");
        }

        // 验证大班课专用字段
        validateLargeClassFields(request);

        // 课程时长：仅大班课需要；一对一不需要
        Integer finalDuration = null;
        if ("large_class".equals(request.getCourseType())) {
            Integer durationMinutes = request.getDurationMinutes();
            if (durationMinutes == null) {
                throw new RuntimeException("大班课必须设置课程时长（90或120分钟）");
            }
            if (durationMinutes != 90 && durationMinutes != 120) {
                throw new RuntimeException("课程时长只能选择90分钟（一个半小时）或120分钟（俩小时）");
            }
            finalDuration = durationMinutes;
        }

        course.setSubjectId(request.getSubjectId());
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setCourseType(request.getCourseType());
        course.setDurationMinutes(finalDuration);

        // 课程地点/授课方式：一对一不设置；大班课二选一（线上 或 线下并指定地点）
        if ("one_on_one".equals(request.getCourseType())) {
            course.setCourseLocation(null);
        } else if ("large_class".equals(request.getCourseType())) {
            Long offlineId = request.getOfflineLocationId();
            Boolean reqOnline = request.getSupportsOnline();
            if (offlineId != null) {
                var loc = teachingLocationMapper.selectById(offlineId);
                if (loc == null || !Boolean.TRUE.equals(loc.getIsActive())) {
                    throw new RuntimeException("线下地点无效或未激活");
                }
                // 方案A：仅存地点名称
                course.setCourseLocation(loc.getName());
            } else if (Boolean.TRUE.equals(reqOnline)) {
                course.setCourseLocation("线上");
            } else if (request.getCourseLocation() != null) {
                // 兼容老前端：若仅提供 courseLocation
                if ("线上".equals(request.getCourseLocation())) {
                    course.setCourseLocation("线上");
                } else if ("线下".equals(request.getCourseLocation())) {
                    throw new RuntimeException("请选择线下授课地点");
                } else {
                    throw new RuntimeException("无效的课程地点");
                }
            } else {
                throw new RuntimeException("大班课必须选择线上或线下地点");
            }
        }

        // 记录旧封面，若请求带新图则覆盖
        String oldImageUrl = course.getImageUrl();
        if (request.getImageUrl() != null && !request.getImageUrl().isEmpty()) {
            course.setImageUrl(request.getImageUrl());
        }

        // 设置价格（所有课程类型都可以有价格）
        course.setPrice(request.getPrice());

        // 更新大班课专用字段
        if ("large_class".equals(request.getCourseType())) {
            course.setStartDate(request.getStartDate());
            course.setEndDate(request.getEndDate());
            course.setPersonLimit(request.getPersonLimit());
            if (request.getCourseTimeSlots() != null) {
                if (com.touhouqing.grabteacherbackend.util.TimeSlotUtil.isValidTimeSlots(request.getCourseTimeSlots())) {
                    course.setCourseTimeSlots(com.touhouqing.grabteacherbackend.util.TimeSlotUtil.toJsonString(request.getCourseTimeSlots()));
                } else {
                    throw new RuntimeException("上课时间安排格式不正确");
                }
            } else {
                course.setCourseTimeSlots(null);
            }
        } else {
            // 非大班课不保留周期设置
            course.setStartDate(null);
            course.setEndDate(null);
            course.setPersonLimit(null);
            course.setCourseTimeSlots(null);
        }

        if ("large_class".equals(request.getCourseType())) {
            course.setStartDate(request.getStartDate());
            course.setEndDate(request.getEndDate());
            course.setPersonLimit(request.getPersonLimit());
        } else {
            // 一对一课程清空大班课专用字段
            course.setStartDate(null);
            course.setEndDate(null);
            course.setPersonLimit(null);
        }

        // 状态更新逻辑：教师编辑课程时状态强制重置为pending，需要重新审批
        if ("teacher".equals(userType)) {
            // 教师编辑课程时，无论原状态如何，都重置为pending状态，需要管理员重新审批
            course.setStatus("pending");
            log.info("教师编辑课程，状态重置为pending，需要管理员重新审批");
        } else if ("admin".equals(userType)) {
            // 管理员可以设置任何状态
            if (request.getStatus() != null) {
                course.setStatus(request.getStatus());
            }
        }

        // 执行更新
        courseMapper.updateById(course);

        // 如果封面发生变更，删除旧图
        if (oldImageUrl != null && !oldImageUrl.equals(course.getImageUrl())) {
            try {
                ossUtil.deleteByUrl(oldImageUrl);
                log.info("已删除旧课程封面: {}", oldImageUrl);
            } catch (Exception ex) {
                log.warn("删除旧课程封面失败: {}", oldImageUrl, ex);
            }
        }


        log.info("课程更新成功: {}", course.getTitle());
        try { eventPublisher.publishEvent(new CourseChangedEvent(this, CourseChangedEvent.ChangeType.UPDATE)); } catch (Exception ignore) {}
        return course;
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = "course", key = "#id"),
        @CacheEvict(cacheNames = "courseList", allEntries = true),
        @CacheEvict(cacheNames = "teacherCourses", allEntries = true),
        @CacheEvict(cacheNames = "activeCourses", allEntries = true),
        @CacheEvict(cacheNames = "activeCoursesAll", allEntries = true),
        @CacheEvict(cacheNames = "activeCoursesLimited", allEntries = true),
        @CacheEvict(cacheNames = "teacherList", allEntries = true)
    })
    public void deleteCourse(Long id, Long currentUserId, String userType) {
        log.info("删除课程，课程ID: {}, 用户ID: {}, 用户类型: {}", id, currentUserId, userType);

        Course course = courseMapper.selectById(id);
        if (course == null || course.getDeleted()) {
            throw new RuntimeException("课程不存在");
        }

        // 权限检查
        if (!hasPermissionToManageCourse(id, currentUserId, userType)) {
            throw new RuntimeException("没有权限操作此课程");
        }

        course.setDeleted(true);
        course.setDeletedAt(LocalDateTime.now());
        courseMapper.updateById(course);


        log.info("课程删除成功: {}", course.getTitle());
        try { eventPublisher.publishEvent(new CourseChangedEvent(this, CourseChangedEvent.ChangeType.DELETE)); } catch (Exception ignore) {}
    }

    /**
     * 根据ID查询课程详情
     */
    // 辅助方法：验证课程地点
    private boolean isValidCourseLocation(String location) {
        return "线上".equals(location) || "线下".equals(location);
    }

    @Override
    @Cacheable(cacheNames = "course", key = "#id", unless = "#result == null")
    public CourseVO getCourseById(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null || course.getDeleted()) {
            return null;
        }
        return convertToCourseResponse(course);
    }

    // 辅助方法：验证课程类型
    private boolean isValidCourseType(String courseType) {
        return "one_on_one".equals(courseType) || "large_class".equals(courseType);
    }

    // 辅助方法：验证课程状态
    private boolean isValidCourseStatus(String status) {
        return "active".equals(status) || "inactive".equals(status) || "full".equals(status) || "pending".equals(status);
    }

    // 辅助方法：验证大班课专用字段
    private void validateLargeClassFields(CourseDTO request) {
        if ("large_class".equals(request.getCourseType())) {
            // 大班课必须设置开始日期和结束日期
            if (request.getStartDate() == null) {
                throw new RuntimeException("大班课必须设置开始日期");
            }
            if (request.getEndDate() == null) {
                throw new RuntimeException("大班课必须设置结束日期");
            }
            // 结束日期必须晚于开始日期
            if (!request.getEndDate().isAfter(request.getStartDate())) {
                throw new RuntimeException("结束日期必须晚于开始日期");
            }
            // 价格和人数限制可以为空（表示可定制价格和不限制人数）
            if (request.getPrice() != null && request.getPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("课程价格必须大于0");
            }
            if (request.getPersonLimit() != null && request.getPersonLimit() <= 0) {
                throw new RuntimeException("人数限制必须大于0");
            }
            // 验证每周时间周期（必填）
            if (request.getCourseTimeSlots() == null || request.getCourseTimeSlots().isEmpty()) {
                throw new RuntimeException("大班课必须设置每周上课时间周期");
            }
            if (!com.touhouqing.grabteacherbackend.util.TimeSlotUtil.isValidTimeSlots(request.getCourseTimeSlots())) {
                throw new RuntimeException("上课时间安排格式不正确");
            }
        }
    }

    /**
     * 分页查询课程列表
     */
    @Override
    @Cacheable(cacheNames = "courseList",
               keyGenerator = "courseCacheKeyGenerator",
               unless = "#result == null || #result.records.isEmpty()")
    public Page<CourseVO> getCourseList(int page, int size, String keyword, Long subjectId,
                                        Long teacherId, String status, String courseType, String courseLocation, String teacherLevel) {
        return doGetCourseList(page, size, keyword, subjectId, teacherId, status, courseType, courseLocation, teacherLevel);
    }

    // 管理端直查 DB
    @Override
    public Page<CourseVO> getCourseListNoCache(int page, int size, String keyword, Long subjectId,
                                               Long teacherId, String status, String courseType) {
        return doGetCourseList(page, size, keyword, subjectId, teacherId, status, courseType, null, null);
    }

    // 共享查询实现
    private Page<CourseVO> doGetCourseList(int page, int size, String keyword, Long subjectId,
                                           Long teacherId, String status, String courseType, String courseLocation, String teacherLevel) {
        Page<Course> pageParam = new Page<>(page, size);
        QueryWrapper<Course> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("is_deleted", false);

        if (StringUtils.hasText(keyword)) {
            queryWrapper.like("title", keyword);
        }

        if (subjectId != null) {
            queryWrapper.eq("subject_id", subjectId);
        }

        if (teacherId != null) {
            queryWrapper.eq("teacher_id", teacherId);
        }

        if (StringUtils.hasText(status)) {
            queryWrapper.eq("status", status);
        }

        if (StringUtils.hasText(courseType)) {
            queryWrapper.eq("course_type", courseType);
        }

        if (StringUtils.hasText(courseLocation)) {
            String loc = courseLocation.trim();
            if (StringUtils.hasText(loc)) {
                queryWrapper.eq("course_location", loc);
            }
        }

        // 教师级别筛选：需关联教师表过滤
        if (StringUtils.hasText(teacherLevel)) {
            String lvl = teacherLevel.trim();
            if (StringUtils.hasText(lvl)) {
                QueryWrapper<Teacher> tq = new QueryWrapper<>();
                tq.eq("is_deleted", false);
                tq.eq("level", lvl);
                List<Teacher> ts = teacherMapper.selectList(tq);
                if (ts == null || ts.isEmpty()) {
                    // 无匹配教师，直接返回空的分页结果（类型为 CourseVO）
                    Page<CourseVO> empty = new Page<>(page, size, 0);
                    empty.setRecords(new ArrayList<>());
                    return empty;
                }
                List<Long> teacherIdsForLevel = ts.stream().map(Teacher::getId).collect(Collectors.toList());
                if (teacherIdsForLevel.isEmpty()) {
                    Page<CourseVO> empty = new Page<>(page, size, 0);
                    empty.setRecords(new ArrayList<>());
                    return empty;
                }
                queryWrapper.in("teacher_id", teacherIdsForLevel);
            }
        }

        queryWrapper.orderByDesc("created_at");

        Page<Course> coursePage = courseMapper.selectPage(pageParam, queryWrapper);

        Page<CourseVO> responsePage = new Page<>(coursePage.getCurrent(), coursePage.getSize(), coursePage.getTotal());
        List<CourseVO> responseList = assembleCourseResponses(coursePage.getRecords());
        responsePage.setRecords(responseList);

        return responsePage;
    }
    /**
     * 获取教师的课程列表
     */
    @Override
    @Cacheable(cacheNames = "teacherCourses",
               keyGenerator = "courseCacheKeyGenerator",
               unless = "#result == null || #result.isEmpty()")
    public List<CourseVO> getTeacherCourses(Long teacherId, Long currentUserId, String userType) {
        // 权限检查：教师只能查看自己的课程，管理员可以查看任何教师的课程，public模式允许查看活跃课程
        if ("teacher".equals(userType)) {
            QueryWrapper<Teacher> teacherQuery = new QueryWrapper<>();
            teacherQuery.eq("user_id", currentUserId);
            teacherQuery.eq("is_deleted", false);
            Teacher teacher = teacherMapper.selectOne(teacherQuery);

            if (teacher == null) {
                throw new RuntimeException("教师信息不存在");
            }

            // 如果没有指定teacherId，使用当前用户对应的教师ID
            if (teacherId == null) {
                teacherId = teacher.getId();
            } else if (!teacher.getId().equals(teacherId)) {
                throw new RuntimeException("没有权限查看此教师的课程");
            }
        } else if ("public".equals(userType)) {
            // 公开访问模式，只返回活跃状态的课程
            if (teacherId == null) {
                throw new RuntimeException("教师ID不能为空");
            }
            // 验证教师是否存在且已认证
            Teacher teacher = teacherMapper.selectById(teacherId);
            if (teacher == null || teacher.getDeleted() || !teacher.getVerified()) {
                throw new RuntimeException("教师不存在或未认证");
            }
        } else if (!"admin".equals(userType)) {
            throw new RuntimeException("没有权限查看教师课程");
        }

        if (teacherId == null) {
            throw new RuntimeException("教师ID不能为空");
        }

        List<Course> courses;
        if ("public".equals(userType)) {
            // 公开访问只返回活跃状态的课程
            courses = courseMapper.findActiveByTeacherId(teacherId);
        } else {
            // 其他情况返回所有课程
            courses = courseMapper.findByTeacherId(teacherId);
        }

        return assembleCourseResponses(courses);
    }

    /**
     * 获取所有活跃课程
     */
    @Override
    @Cacheable(cacheNames = "activeCoursesAll",
               key = "'all'",
               unless = "#result == null || #result.isEmpty()")
    public List<CourseVO> getActiveCourses() {
        List<Course> courses = courseMapper.findActiveCourses();
        return assembleCourseResponses(courses);
    }

    /**
     * 获取活跃课程列表（限制条数）
     */
    @Override
    @Cacheable(cacheNames = "activeCoursesLimited",
               key = "'limit:' + (#limit == null ? 'all' : #limit)",
               sync = true)
    public List<CourseVO> getActiveCoursesLimited(Integer limit) {
        if (limit == null || limit <= 0) {
            return getActiveCourses();
        }
        QueryWrapper<Course> qw = new QueryWrapper<>();
        qw.eq("status", "active").eq("is_deleted", false).orderByDesc("created_at");
        Page<Course> page = new Page<>(1, limit);
        Page<Course> res = courseMapper.selectPage(page, qw);
        return assembleCourseResponses(res.getRecords());
    }

    /**
     * 更新课程状态
     */
    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = "course", key = "#id"),
        @CacheEvict(cacheNames = "courseList", allEntries = true),
        @CacheEvict(cacheNames = "teacherCourses", allEntries = true),
        @CacheEvict(cacheNames = "activeCourses", allEntries = true),
        @CacheEvict(cacheNames = "activeCoursesAll", allEntries = true),
        @CacheEvict(cacheNames = "activeCoursesLimited", allEntries = true),
        @CacheEvict(cacheNames = "teacherList", allEntries = true),
        @CacheEvict(cacheNames = "featuredCourses", allEntries = true),
        @CacheEvict(cacheNames = "featuredCourseIds", allEntries = true)
    })
    public void updateCourseStatus(Long id, String status, Long currentUserId, String userType) {
        log.info("更新课程状态，课程ID: {}, 新状态: {}, 用户ID: {}", id, status, currentUserId);

        Course course = courseMapper.selectById(id);
        if (course == null || course.getDeleted()) {
            throw new RuntimeException("课程不存在");
        }

        // 权限检查
        if (!hasPermissionToManageCourse(id, currentUserId, userType)) {
            throw new RuntimeException("没有权限操作此课程");
        }

        if (!isValidCourseStatus(status)) {
            throw new RuntimeException("无效的课程状态");
        }

        course.setStatus(status);
        courseMapper.updateById(course);
        log.info("课程状态更新成功: {} -> {}", course.getTitle(), status);
        try { eventPublisher.publishEvent(new CourseChangedEvent(this, CourseChangedEvent.ChangeType.STATUS)); } catch (Exception ignore) {}
    }

    @Override
    public boolean hasPermissionToManageCourse(Long courseId, Long currentUserId, String userType) {
        if ("admin".equals(userType)) {
            return true; // 管理员有所有权限
        }

        if ("teacher".equals(userType)) {
            Course course = courseMapper.selectById(courseId);
            if (course == null || course.getDeleted()) {
                return false;
            }

            // 查找当前用户对应的教师记录
            QueryWrapper<Teacher> teacherQuery = new QueryWrapper<>();
            teacherQuery.eq("user_id", currentUserId);
            teacherQuery.eq("is_deleted", false);
            Teacher teacher = teacherMapper.selectOne(teacherQuery);

            return teacher != null && teacher.getId().equals(course.getTeacherId());
        }

        return false; // 学生没有管理课程的权限
    }

    // 辅助方法：转换为CourseResponse
    private CourseVO convertToCourseResponse(Course course) {
        // 获取教师信息
        Teacher teacher = teacherMapper.selectById(course.getTeacherId());
        String teacherName = teacher != null ? teacher.getRealName() : "未知教师";

        // 获取科目信息
        Subject subject = subjectMapper.selectById(course.getSubjectId());
        String subjectName = subject != null ? subject.getName() : "未知科目";


        CourseVO.CourseVOBuilder builder = CourseVO.builder()
                .id(course.getId())
                .teacherId(course.getTeacherId())
                .teacherName(teacherName)
                .subjectId(course.getSubjectId())
                .subjectName(subjectName)
                .title(course.getTitle())
                .description(course.getDescription())
                .courseType(course.getCourseType())
                .durationMinutes(course.getDurationMinutes())
                .status(course.getStatus())
                .featured(course.getFeatured())
                .createdAt(course.getCreatedAt())
                .courseLocation(course.getCourseLocation())
                .price(course.getPrice())
                .startDate(course.getStartDate())
                .endDate(course.getEndDate())
                .personLimit(course.getPersonLimit())
                .enrollmentCount(course.getEnrollmentCount())
                .imageUrl(course.getImageUrl());

        CourseVO response = builder.build();
        // 附加授课方式（方案A：从course_location推导），线下地点ID无法从名称反推，置空
        response.setSupportsOnline("线上".equals(course.getCourseLocation()));
        response.setOfflineLocationId(null);

        // 回显大班课每周时间周期
        if (course.getCourseTimeSlots() != null && !course.getCourseTimeSlots().isEmpty()) {
            try {
                java.util.List<com.touhouqing.grabteacherbackend.model.dto.TimeSlotDTO> slots =
                        com.touhouqing.grabteacherbackend.util.TimeSlotUtil.fromJsonString(course.getCourseTimeSlots());
                response.setCourseTimeSlots(slots);
            } catch (Exception ignored) {
                // 忽略解析异常，保持为空
            }
        }

        // 设置显示名称
        response.setCourseTypeDisplay(response.getCourseTypeDisplay());
        response.setStatusDisplay(response.getStatusDisplay());

        return response;
    }

    /**
     * 获取精选课程列表（分页）
     */
    @Override
    @Cacheable(cacheNames = "featuredCourses",
               key = "'page:' + #page + ':size:' + #size + ':subject:' + (#subjectId ?: 'all')",
               unless = "#result == null || #result.records.isEmpty()")
    public Page<CourseVO> getFeaturedCourses(int page, int size, Long subjectId) {
        return doGetFeaturedCourses(page, size, subjectId);
    }

    @Override
    public Page<CourseVO> getFeaturedCoursesNoCache(int page, int size, Long subjectId) {
        return doGetFeaturedCourses(page, size, subjectId);
    }

    /**
     * 获取所有精选课程列表（不分页，用于首页滚动展示）
     */
    @Override
    @Cacheable(cacheNames = "allFeaturedCourses", key = "'all'", unless = "#result == null || #result.isEmpty()")
    public List<CourseVO> getAllFeaturedCourses() {
        QueryWrapper<Course> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_featured", true)
                   .in("status", "active", "full")
                   .eq("is_deleted", false)
                   .orderByDesc("created_at");

        List<Course> courses = courseMapper.selectList(queryWrapper);
        return assembleCourseResponses(courses);
    }

    private Page<CourseVO> doGetFeaturedCourses(int page, int size, Long subjectId) {
        Page<Course> pageParam = new Page<>(page, size);
        QueryWrapper<Course> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("is_featured", true)
                   .in("status", "active", "full")
                   .eq("is_deleted", false);

        if (subjectId != null) {
            queryWrapper.eq("subject_id", subjectId);
        }


        queryWrapper.orderByDesc("created_at");

        Page<Course> coursePage = courseMapper.selectPage(pageParam, queryWrapper);

        // 批量装配，消除 N+1
        Page<CourseVO> responsePage = new Page<>(coursePage.getCurrent(), coursePage.getSize(), coursePage.getTotal());
        List<CourseVO> responseList = assembleCourseResponses(coursePage.getRecords());
        responsePage.setRecords(responseList);

        return responsePage;
    }


    // 批量组装 CourseVO，避免 convertToCourseResponse 的逐条查询开销
    private List<CourseVO> assembleCourseResponses(List<Course> courses) {
        if (courses == null || courses.isEmpty()) return new ArrayList<>();
        // 1) 收集ID集合
        List<Long> teacherIds = courses.stream().map(Course::getTeacherId).collect(Collectors.toList());
        List<Long> subjectIds = courses.stream().map(Course::getSubjectId).collect(Collectors.toList());
        // 注意：如需扩展按课程ID批量加载关联数据，可在此使用课程ID集合

        // 2) 批量查询教师
        Map<Long, String> teacherNameMap = new HashMap<>();
        Map<Long, Teacher> teacherMap = new HashMap<>();
        if (!teacherIds.isEmpty()) {
            QueryWrapper<Teacher> tq = new QueryWrapper<>();
            tq.in("id", teacherIds);
            tq.eq("is_deleted", false);
            for (Teacher t : teacherMapper.selectList(tq)) {
                teacherNameMap.put(t.getId(), t.getRealName());
                teacherMap.put(t.getId(), t);
            }
        }

        // 3) 批量查询科目
        Map<Long, String> subjectNameMap = new HashMap<>();
        if (!subjectIds.isEmpty()) {
            QueryWrapper<Subject> sq = new QueryWrapper<>();
            sq.in("id", subjectIds);
            sq.eq("is_deleted", false);
            for (Subject s : subjectMapper.selectList(sq)) {
                subjectNameMap.put(s.getId(), s.getName());
            }
        }


        // 5) 组装响应
        List<CourseVO> list = new ArrayList<>();
        for (Course c : courses) {
            String teacherName = teacherNameMap.getOrDefault(c.getTeacherId(), "未知教师");
            String subjectName = subjectNameMap.getOrDefault(c.getSubjectId(), "未知科目");

            CourseVO resp = CourseVO.builder()
                    .id(c.getId())
                    .teacherId(c.getTeacherId())
                    .teacherName(teacherName)
                    .subjectId(c.getSubjectId())
                    .subjectName(subjectName)
                    .title(c.getTitle())
                    .description(truncate(c.getDescription(), 160))
                    .courseType(c.getCourseType())
                    .durationMinutes(c.getDurationMinutes())
                    .status(c.getStatus())
                    .featured(c.getFeatured())
                    .createdAt(c.getCreatedAt())
                    .courseLocation(c.getCourseLocation())
                    .price(c.getPrice())
                    .startDate(c.getStartDate())
                    .endDate(c.getEndDate())
                    .personLimit(c.getPersonLimit())
                    .enrollmentCount(c.getEnrollmentCount())
                    .imageUrl(c.getImageUrl())
                    .build();

            // 授课方式（方案A：从 course_location 推导），线下地点ID无法从名称反推，置空
            resp.setSupportsOnline("线上".equals(c.getCourseLocation()));
            resp.setOfflineLocationId(null);

            // 回显批量列表的周期时间（管理端可能需要）
            if (c.getCourseTimeSlots() != null && !c.getCourseTimeSlots().isEmpty()) {
                try {
                    java.util.List<com.touhouqing.grabteacherbackend.model.dto.TimeSlotDTO> slots =
                            com.touhouqing.grabteacherbackend.util.TimeSlotUtil.fromJsonString(c.getCourseTimeSlots());
                    resp.setCourseTimeSlots(slots);
                } catch (Exception ignored) {}
            }


            // teacher level & schedule display（使用已批量查询的 teacherMap，避免N+1）
            Teacher teacher = teacherMap.get(c.getTeacherId());
            if (teacher != null) {
                resp.setTeacherLevel(teacher.getLevel());
                resp.setScheduleDisplay(buildScheduleDisplay(c, teacher));
            } else {
                resp.setScheduleDisplay(buildScheduleDisplay(c, null));
            }

            resp.setCourseTypeDisplay(resp.getCourseTypeDisplay());
            resp.setStatusDisplay(resp.getStatusDisplay());
            list.add(resp);
        }
        return list;
    }

    /**
     * 设置课程为精选课程
     */
    @Override
    @CacheEvict(cacheNames = {"featuredCourses", "featuredCourseIds", "courseList", "activeCourses", "teacherList", "allFeaturedCourses"}, allEntries = true)
    public void setCourseAsFeatured(Long courseId, boolean featured) {
        Course course = courseMapper.selectById(courseId);
        if (course == null || course.getDeleted()) {
            throw new RuntimeException("课程不存在");
        }

        course.setFeatured(featured);
        courseMapper.updateById(course);

        log.info("课程 {} 精选状态已更新为: {}", courseId, featured);
        try { eventPublisher.publishEvent(new CourseChangedEvent(this, CourseChangedEvent.ChangeType.FEATURED)); } catch (Exception ignore) {}
    }

    /**
     * 批量设置精选课程
     */
    @Override
    @CacheEvict(cacheNames = {"featuredCourses", "featuredCourseIds", "courseList", "activeCourses", "teacherList", "allFeaturedCourses"}, allEntries = true)
    public void batchSetFeaturedCourses(List<Long> courseIds, boolean featured) {
        if (courseIds == null || courseIds.isEmpty()) {
            return;
        }

        // 验证所有课程都存在且未删除，并批量更新
        QueryWrapper<Course> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", courseIds)
                   .eq("is_deleted", false);

        List<Course> courses = courseMapper.selectList(queryWrapper);

        if (courses.isEmpty()) {
            throw new RuntimeException("没有有效的课程可以更新");
        }

        // 批量更新
        for (Course course : courses) {
            course.setFeatured(featured);
            courseMapper.updateById(course);
        }

        log.info("批量更新 {} 个课程的精选状态为: {}", courses.size(), featured);
        try { eventPublisher.publishEvent(new CourseChangedEvent(this, CourseChangedEvent.ChangeType.FEATURED)); } catch (Exception ignore) {}
    }

        // 列表轻载化：截断长文本，避免超大回包
        private String truncate(String s, int max) {
            if (s == null) return null;
            if (max <= 0) return "";
            return s.length() <= max ? s : s.substring(0, max);
        }

        /**
         * 获取所有精选课程ID列表
         */
        @Override
        @Cacheable(cacheNames = "featuredCourseIds", key = "'all'")
        public List<Long> getFeaturedCourseIds() {
            return courseMapper.findFeaturedCourseIds();
        }

        /**
         * 管理端强一致：精选课程ID列表直查 DB
         */
        @Override
        public List<Long> getFeaturedCourseIdsNoCache() {
            return courseMapper.findFeaturedCourseIds();
        }

        // ===== 辅助方法：方案A 时间展示拼接（不改库表）=====
    private String buildScheduleDisplay(Course c, Teacher teacher) {
        StringBuilder sb = new StringBuilder();
        if (c.getStartDate() != null && c.getEndDate() != null) {
            sb.append(String.format("%04d.%02d.%02d-%04d.%02d.%02d",
                    c.getStartDate().getYear(), c.getStartDate().getMonthValue(), c.getStartDate().getDayOfMonth(),
                    c.getEndDate().getYear(), c.getEndDate().getMonthValue(), c.getEndDate().getDayOfMonth()));
        }
        boolean isLarge = "large_class".equalsIgnoreCase(c.getCourseType());
        if (isLarge) {
            String weekly = extractWeeklyFromTeacher(teacher);
            if (!weekly.isEmpty()) {
                if (sb.length() > 0) sb.append("，");
                sb.append(weekly);
            }
        } else {
            if (sb.length() > 0) sb.append("，");
            sb.append("时间可协商");
        }
        return sb.toString();
    }

    private String extractWeeklyFromTeacher(Teacher teacher) {
        if (teacher == null) return "";
        String json = teacher.getAvailableTimeSlots();
        if (json == null || json.isEmpty()) return "";
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\\"weekday\\\":(\\d).*?\\\"timeSlots\\\":\\\\[(\\\"[0-9:]{4,}-[0-9:]{4,}\\\")");
            java.util.regex.Matcher m = p.matcher(json);
            java.util.Map<Integer, Integer> count = new java.util.HashMap<>();
            java.util.Map<Integer, String> firstSlot = new java.util.HashMap<>();
            while (m.find()) {
                String wStr = m.group(1).replaceAll("[^0-9]", "");
                if (wStr.isEmpty()) continue;
                int w = Integer.parseInt(wStr);
                String slotQuoted = m.group(2);
                String slot = slotQuoted != null && slotQuoted.length() >= 2 ? slotQuoted.substring(1, slotQuoted.length() - 1) : slotQuoted;
                count.put(w, count.getOrDefault(w, 0) + 1);
                if (!firstSlot.containsKey(w) && slot != null) firstSlot.put(w, slot);
            }
            int bestW = -1, bestC = -1;
            for (java.util.Map.Entry<Integer, Integer> e : count.entrySet()) {
                if (e.getValue() > bestC) { bestC = e.getValue(); bestW = e.getKey(); }
            }
            if (bestW == -1) return "";
            String weekdayZh;
            switch (bestW) {
                case 1: weekdayZh = "周一"; break;
                case 2: weekdayZh = "周二"; break;
                case 3: weekdayZh = "周三"; break;
                case 4: weekdayZh = "周四"; break;
                case 5: weekdayZh = "周五"; break;
                case 6: weekdayZh = "周六"; break;
                case 0:
                case 7: weekdayZh = "周日"; break;
                default: weekdayZh = ""; break;
            }
            if (weekdayZh.isEmpty()) return "";
            String slot = firstSlot.get(bestW);
            if (slot != null && !slot.isEmpty()) {
                return "每周" + weekdayZh.substring(1) + "，" + slot;
            }
            return "每周" + weekdayZh.substring(1);
        } catch (Exception ignore) {
            return "";
        }
    }
}

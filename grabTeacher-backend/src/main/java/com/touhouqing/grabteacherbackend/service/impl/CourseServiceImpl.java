package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.model.dto.CourseDTO;
import com.touhouqing.grabteacherbackend.model.vo.CourseVO;
import com.touhouqing.grabteacherbackend.model.entity.Course;
import com.touhouqing.grabteacherbackend.model.entity.CourseGrade;
import com.touhouqing.grabteacherbackend.model.entity.Teacher;
import com.touhouqing.grabteacherbackend.model.entity.Subject;
import com.touhouqing.grabteacherbackend.model.entity.Grade;
import com.touhouqing.grabteacherbackend.mapper.CourseMapper;
import com.touhouqing.grabteacherbackend.mapper.CourseGradeMapper;
import com.touhouqing.grabteacherbackend.mapper.TeacherMapper;
import com.touhouqing.grabteacherbackend.mapper.SubjectMapper;
import com.touhouqing.grabteacherbackend.mapper.GradeMapper;
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
    private CourseGradeMapper courseGradeMapper;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private SubjectMapper subjectMapper;

    @Autowired
    private GradeMapper gradeMapper;

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

        Course course = Course.builder()
                .teacherId(teacherId)
                .subjectId(request.getSubjectId())
                .title(request.getTitle())
                .description(request.getDescription())
                .courseType(request.getCourseType())
                .durationMinutes(request.getDurationMinutes())
                .status(courseStatus)
                .deleted(false)
                .imageUrl(request.getImageUrl())
                .price(request.getPrice()) // 所有课程类型都可以设置价格
                .build();

        // 设置大班课专用字段
        if ("large_class".equals(request.getCourseType())) {
            course.setStartDate(request.getStartDate());
            course.setEndDate(request.getEndDate());
            course.setPersonLimit(request.getPersonLimit());
        }

        courseMapper.insert(course);

        // 处理年级数据 - 验证年级是否存在于年级管理表中
        if (StringUtils.hasText(request.getGrade())) {
            String[] grades = request.getGrade().split(",");
            for (String grade : grades) {
                String trimmedGrade = grade.trim();
                if (StringUtils.hasText(trimmedGrade)) {
                    // 验证年级是否存在于年级管理表中
                    Grade gradeEntity = gradeMapper.findByGradeName(trimmedGrade);
                    if (gradeEntity == null) {
                        throw new RuntimeException("年级 '" + trimmedGrade + "' 不存在，请先在年级管理中添加该年级");
                    }

                    CourseGrade courseGrade = new CourseGrade(course.getId(), trimmedGrade);
                    courseGradeMapper.insert(courseGrade);
                }
            }
        }

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

        // 验证课程时长
        Integer durationMinutes = request.getDurationMinutes();
        if (durationMinutes != null) {
            // 如果设置了具体时长，只能是90分钟或120分钟
            if (durationMinutes != 90 && durationMinutes != 120) {
                throw new RuntimeException("课程时长只能选择90分钟（一个半小时）或120分钟（俩小时），或者留空表示灵活时间");
            }
        }
        // 如果为null，表示灵活时间，允许一个半小时或俩小时均可

        course.setSubjectId(request.getSubjectId());
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setCourseType(request.getCourseType());
        course.setDurationMinutes(request.getDurationMinutes());
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

        // 更新年级数据：先删除原有关联，再插入新的关联
        courseGradeMapper.deleteByCourseId(course.getId());
        if (StringUtils.hasText(request.getGrade())) {
            String[] grades = request.getGrade().split(",");
            for (String grade : grades) {
                String trimmedGrade = grade.trim();
                if (StringUtils.hasText(trimmedGrade)) {
                    // 验证年级是否存在于年级管理表中
                    Grade gradeEntity = gradeMapper.findByGradeName(trimmedGrade);
                    if (gradeEntity == null) {
                        throw new RuntimeException("年级 '" + trimmedGrade + "' 不存在，请先在年级管理中添加该年级");
                    }

                    CourseGrade courseGrade = new CourseGrade(course.getId(), trimmedGrade);
                    courseGradeMapper.insert(courseGrade);
                }
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

        // 删除课程年级关联
        courseGradeMapper.deleteByCourseId(id);

        log.info("课程删除成功: {}", course.getTitle());
        try { eventPublisher.publishEvent(new CourseChangedEvent(this, CourseChangedEvent.ChangeType.DELETE)); } catch (Exception ignore) {}
    }

    /**
     * 根据ID查询课程详情
     */
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
                                        Long teacherId, String status, String courseType, String grade) {
        return doGetCourseList(page, size, keyword, subjectId, teacherId, status, courseType, grade);
    }

    // 管理端直查 DB
    @Override
    public Page<CourseVO> getCourseListNoCache(int page, int size, String keyword, Long subjectId,
                                               Long teacherId, String status, String courseType, String grade) {
        return doGetCourseList(page, size, keyword, subjectId, teacherId, status, courseType, grade);
    }

    // 共享查询实现
    private Page<CourseVO> doGetCourseList(int page, int size, String keyword, Long subjectId,
                                           Long teacherId, String status, String courseType, String grade) {
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

        // 如果有年级筛选条件，需要通过关联表查询
        if (StringUtils.hasText(grade)) {
            List<Long> courseIds = courseGradeMapper.findCourseIdsByGrade(grade);
            if (courseIds.isEmpty()) {
                Page<CourseVO> emptyPage = new Page<>(page, size, 0);
                emptyPage.setRecords(new ArrayList<>());
                return emptyPage;
            }
            queryWrapper.in("id", courseIds);
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

        // 获取课程年级信息
        List<CourseGrade> courseGrades = courseGradeMapper.findByCourseId(course.getId());
        String gradeStr = courseGrades.isEmpty() ? "未设置" :
            courseGrades.stream()
                .map(CourseGrade::getGrade)
                .collect(Collectors.joining(","));

        CourseVO response = CourseVO.builder()
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
                .grade(gradeStr) // 从course_grades表获取年级信息
                .price(course.getPrice())
                .startDate(course.getStartDate())
                .endDate(course.getEndDate())
                .personLimit(course.getPersonLimit())
                .imageUrl(course.getImageUrl())
                .build();

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
               key = "'page:' + #page + ':size:' + #size + ':subject:' + (#subjectId ?: 'all') + ':grade:' + (#grade ?: 'all')",
               unless = "#result == null || #result.records.isEmpty()")
    public Page<CourseVO> getFeaturedCourses(int page, int size, Long subjectId, String grade) {
        return doGetFeaturedCourses(page, size, subjectId, grade);
    }

    @Override
    public Page<CourseVO> getFeaturedCoursesNoCache(int page, int size, Long subjectId, String grade) {
        return doGetFeaturedCourses(page, size, subjectId, grade);
    }

    /**
     * 获取所有精选课程列表（不分页，用于首页滚动展示）
     */
    @Override
    @Cacheable(cacheNames = "allFeaturedCourses", key = "'all'", unless = "#result == null || #result.isEmpty()")
    public List<CourseVO> getAllFeaturedCourses() {
        QueryWrapper<Course> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_featured", true)
                   .eq("status", "active")
                   .eq("is_deleted", false)
                   .orderByDesc("created_at");

        List<Course> courses = courseMapper.selectList(queryWrapper);
        return assembleCourseResponses(courses);
    }

    private Page<CourseVO> doGetFeaturedCourses(int page, int size, Long subjectId, String grade) {
        Page<Course> pageParam = new Page<>(page, size);
        QueryWrapper<Course> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("is_featured", true)
                   .eq("status", "active")
                   .eq("is_deleted", false);

        if (subjectId != null) {
            queryWrapper.eq("subject_id", subjectId);
        }

        // 如果指定了年级，改为两段式查询：先查 course_ids 再 IN 过滤，避免 exists 带来的执行波动
        if (grade != null && !grade.trim().isEmpty()) {
            String g = grade.trim();
            List<Long> idsByGrade = courseGradeMapper.findCourseIdsByGrade(g);
            if (idsByGrade == null || idsByGrade.isEmpty()) {
                Page<CourseVO> emptyPage = new Page<>(page, size, 0);
                emptyPage.setRecords(new ArrayList<>());
                return emptyPage;
            }
            queryWrapper.in("id", idsByGrade);
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
        List<Long> courseIds = courses.stream().map(Course::getId).collect(Collectors.toList());

        // 2) 批量查询教师
        Map<Long, String> teacherNameMap = new HashMap<>();
        if (!teacherIds.isEmpty()) {
            QueryWrapper<Teacher> tq = new QueryWrapper<>();
            tq.in("id", teacherIds);
            tq.eq("is_deleted", false);
            for (Teacher t : teacherMapper.selectList(tq)) {
                teacherNameMap.put(t.getId(), t.getRealName());
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

        // 4) 批量查询年级
        Map<Long, String> courseGradesStr = new HashMap<>();
        if (!courseIds.isEmpty()) {
            List<CourseGrade> all = courseGradeMapper.findByCourseIds(courseIds);
            Map<Long, List<String>> tmp = new HashMap<>();
            for (CourseGrade cg : all) {
                tmp.computeIfAbsent(cg.getCourseId(), k -> new ArrayList<>()).add(cg.getGrade());
            }
            for (Map.Entry<Long, List<String>> e : tmp.entrySet()) {
                courseGradesStr.put(e.getKey(), String.join(",", e.getValue()));
            }
        }

        // 5) 组装响应
        List<CourseVO> list = new ArrayList<>();
        for (Course c : courses) {
            String teacherName = teacherNameMap.getOrDefault(c.getTeacherId(), "未知教师");
            String subjectName = subjectNameMap.getOrDefault(c.getSubjectId(), "未知科目");
            String gradeStr = courseGradesStr.getOrDefault(c.getId(), "未设置");

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
                    .grade(gradeStr)
                    .price(c.getPrice())
                    .startDate(c.getStartDate())
                    .endDate(c.getEndDate())
                    .personLimit(c.getPersonLimit())
                    .imageUrl(c.getImageUrl())
                    .build();
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
}

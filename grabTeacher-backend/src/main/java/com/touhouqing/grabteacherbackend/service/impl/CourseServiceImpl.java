package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.dto.CourseRequest;
import com.touhouqing.grabteacherbackend.dto.CourseResponse;
import com.touhouqing.grabteacherbackend.entity.Course;
import com.touhouqing.grabteacherbackend.entity.CourseGrade;
import com.touhouqing.grabteacherbackend.entity.Teacher;
import com.touhouqing.grabteacherbackend.entity.Subject;
import com.touhouqing.grabteacherbackend.entity.Grade;
import com.touhouqing.grabteacherbackend.mapper.CourseMapper;
import com.touhouqing.grabteacherbackend.mapper.CourseGradeMapper;
import com.touhouqing.grabteacherbackend.mapper.TeacherMapper;
import com.touhouqing.grabteacherbackend.mapper.SubjectMapper;
import com.touhouqing.grabteacherbackend.mapper.GradeMapper;
import com.touhouqing.grabteacherbackend.service.CourseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
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

    @Override
    @Transactional
    public Course createCourse(CourseRequest request, Long currentUserId, String userType) {
        log.info("创建课程，用户ID: {}, 用户类型: {}", currentUserId, userType);
        
        // 验证科目是否存在
        Subject subject = subjectMapper.selectById(request.getSubjectId());
        if (subject == null || subject.getIsDeleted()) {
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
            if (teacher == null || teacher.getIsDeleted()) {
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
                .isDeleted(false)
                .build();

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
        return course;
    }

    @Override
    @Transactional
    public Course updateCourse(Long id, CourseRequest request, Long currentUserId, String userType) {
        log.info("更新课程，课程ID: {}, 用户ID: {}, 用户类型: {}", id, currentUserId, userType);
        
        Course course = courseMapper.selectById(id);
        if (course == null || course.getIsDeleted()) {
            throw new RuntimeException("课程不存在");
        }
        
        // 权限检查
        if (!hasPermissionToManageCourse(id, currentUserId, userType)) {
            throw new RuntimeException("没有权限操作此课程");
        }
        
        // 验证科目是否存在
        Subject subject = subjectMapper.selectById(request.getSubjectId());
        if (subject == null || subject.getIsDeleted()) {
            throw new RuntimeException("科目不存在");
        }
        
        // 管理员可以修改教师，教师不能修改
        if ("admin".equals(userType) && request.getTeacherId() != null) {
            Teacher teacher = teacherMapper.selectById(request.getTeacherId());
            if (teacher == null || teacher.getIsDeleted()) {
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

        course.setSubjectId(request.getSubjectId());
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setCourseType(request.getCourseType());
        course.setDurationMinutes(request.getDurationMinutes());

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

        courseMapper.updateById(course);

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
        return course;
    }

    @Override
    @Transactional
    public void deleteCourse(Long id, Long currentUserId, String userType) {
        log.info("删除课程，课程ID: {}, 用户ID: {}, 用户类型: {}", id, currentUserId, userType);
        
        Course course = courseMapper.selectById(id);
        if (course == null || course.getIsDeleted()) {
            throw new RuntimeException("课程不存在");
        }
        
        // 权限检查
        if (!hasPermissionToManageCourse(id, currentUserId, userType)) {
            throw new RuntimeException("没有权限操作此课程");
        }

        course.setIsDeleted(true);
        course.setDeletedAt(LocalDateTime.now());
        courseMapper.updateById(course);

        // 删除课程年级关联
        courseGradeMapper.deleteByCourseId(id);

        log.info("课程删除成功: {}", course.getTitle());
    }

    @Override
    public CourseResponse getCourseById(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null || course.getIsDeleted()) {
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

    @Override
    public Page<CourseResponse> getCourseList(int page, int size, String keyword, Long subjectId,
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
            // 先从course_grades表中查询符合年级条件的课程ID
            List<Long> courseIds = courseGradeMapper.findCourseIdsByGrade(grade);
            if (courseIds.isEmpty()) {
                // 如果没有找到符合条件的课程，返回空结果
                Page<CourseResponse> emptyPage = new Page<>(page, size, 0);
                emptyPage.setRecords(new ArrayList<>());
                return emptyPage;
            }
            queryWrapper.in("id", courseIds);
        }

        queryWrapper.orderByDesc("created_at");

        Page<Course> coursePage = courseMapper.selectPage(pageParam, queryWrapper);

        // 转换为CourseResponse
        Page<CourseResponse> responsePage = new Page<>(coursePage.getCurrent(), coursePage.getSize(), coursePage.getTotal());
        List<CourseResponse> responseList = coursePage.getRecords().stream()
                .map(this::convertToCourseResponse)
                .collect(Collectors.toList());
        responsePage.setRecords(responseList);

        return responsePage;
    }

    @Override
    public List<CourseResponse> getTeacherCourses(Long teacherId, Long currentUserId, String userType) {
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
            if (teacher == null || teacher.getIsDeleted() || !teacher.getIsVerified()) {
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

        return courses.stream()
                .map(this::convertToCourseResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseResponse> getActiveCourses() {
        List<Course> courses = courseMapper.findActiveCourses();
        return courses.stream()
                .map(this::convertToCourseResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateCourseStatus(Long id, String status, Long currentUserId, String userType) {
        log.info("更新课程状态，课程ID: {}, 新状态: {}, 用户ID: {}", id, status, currentUserId);

        Course course = courseMapper.selectById(id);
        if (course == null || course.getIsDeleted()) {
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
    }

    @Override
    public boolean hasPermissionToManageCourse(Long courseId, Long currentUserId, String userType) {
        if ("admin".equals(userType)) {
            return true; // 管理员有所有权限
        }

        if ("teacher".equals(userType)) {
            Course course = courseMapper.selectById(courseId);
            if (course == null || course.getIsDeleted()) {
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
    private CourseResponse convertToCourseResponse(Course course) {
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

        CourseResponse response = CourseResponse.builder()
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
                .createdAt(course.getCreatedAt())
                .grade(gradeStr) // 从course_grades表获取年级信息
                .build();

        // 设置显示名称
        response.setCourseTypeDisplay(response.getCourseTypeDisplay());
        response.setStatusDisplay(response.getStatusDisplay());

        return response;
    }
}

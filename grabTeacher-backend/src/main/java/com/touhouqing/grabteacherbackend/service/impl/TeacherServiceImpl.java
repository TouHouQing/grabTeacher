package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.dto.TeacherInfoRequest;
import com.touhouqing.grabteacherbackend.dto.TeacherMatchRequest;
import com.touhouqing.grabteacherbackend.dto.TeacherMatchResponse;
import com.touhouqing.grabteacherbackend.entity.Course;
import com.touhouqing.grabteacherbackend.entity.Subject;
import com.touhouqing.grabteacherbackend.entity.Teacher;
import com.touhouqing.grabteacherbackend.mapper.CourseMapper;
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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {

    private final TeacherMapper teacherMapper;
    private final SubjectService subjectService;
    private final CourseMapper courseMapper;

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
}
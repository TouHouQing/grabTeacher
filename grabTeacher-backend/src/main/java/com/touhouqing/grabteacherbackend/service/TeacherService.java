package com.touhouqing.grabteacherbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.dto.TeacherInfoRequest;
import com.touhouqing.grabteacherbackend.entity.Teacher;
import com.touhouqing.grabteacherbackend.mapper.TeacherMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class TeacherService {

    private static final Logger logger = LoggerFactory.getLogger(TeacherService.class);

    @Autowired
    private TeacherMapper teacherMapper;

    /**
     * 根据用户ID获取教师信息
     */
    public Teacher getTeacherByUserId(Long userId) {
        QueryWrapper<Teacher> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("is_deleted", false);
        return teacherMapper.selectOne(queryWrapper);
    }

    /**
     * 根据ID获取教师信息
     */
    public Teacher getTeacherById(Long teacherId) {
        return teacherMapper.selectById(teacherId);
    }

    /**
     * 获取教师列表
     */
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

        teacherMapper.updateById(teacher);
        logger.info("更新教师信息成功: userId={}", userId);

        return teacher;
    }
}
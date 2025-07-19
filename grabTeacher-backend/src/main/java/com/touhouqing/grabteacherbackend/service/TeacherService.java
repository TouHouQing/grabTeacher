package com.touhouqing.grabteacherbackend.service;

import com.touhouqing.grabteacherbackend.dto.TeacherInfoRequest;
import com.touhouqing.grabteacherbackend.entity.Teacher;

import java.util.List;

public interface TeacherService {
    
    /**
     * 根据用户ID获取教师信息
     */
    Teacher getTeacherByUserId(Long userId);
    
    /**
     * 根据ID获取教师信息
     */
    Teacher getTeacherById(Long teacherId);
    
    /**
     * 获取教师列表
     */
    List<Teacher> getTeacherList(int page, int size, String subject, String keyword);
    
    /**
     * 更新教师信息
     */
    Teacher updateTeacherInfo(Long userId, TeacherInfoRequest request);
}
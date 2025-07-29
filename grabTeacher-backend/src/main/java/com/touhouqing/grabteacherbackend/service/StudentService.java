package com.touhouqing.grabteacherbackend.service;

import com.touhouqing.grabteacherbackend.dto.StudentInfoRequest;
import com.touhouqing.grabteacherbackend.entity.Student;
import java.util.Map;

public interface StudentService {
    
    /**
     * 根据用户ID获取学生信息
     */
    Student getStudentByUserId(Long userId);
    
    /**
     * 更新学生信息
     */
    Student updateStudentInfo(Long userId, StudentInfoRequest request);

    /**
     * 获取学生控制台统计数据
     */
    Map<String, Object> getStudentStatistics(Long userId);
} 
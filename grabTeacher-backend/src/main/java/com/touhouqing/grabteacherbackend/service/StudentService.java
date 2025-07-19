package com.touhouqing.grabteacherbackend.service;

import com.touhouqing.grabteacherbackend.dto.StudentInfoRequest;
import com.touhouqing.grabteacherbackend.entity.Student;

public interface StudentService {
    
    /**
     * 根据用户ID获取学生信息
     */
    Student getStudentByUserId(Long userId);
    
    /**
     * 更新学生信息
     */
    Student updateStudentInfo(Long userId, StudentInfoRequest request);
} 
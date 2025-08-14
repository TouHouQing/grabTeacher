package com.touhouqing.grabteacherbackend.service;

import com.touhouqing.grabteacherbackend.model.dto.StudentInfoDTO;
import com.touhouqing.grabteacherbackend.model.vo.StudentProfileVO;
import com.touhouqing.grabteacherbackend.model.entity.Student;
import java.util.Map;

public interface StudentService {
    
    /**
     * 根据用户ID获取学生信息
     */
    Student getStudentByUserId(Long userId);

    /**
     * 根据用户ID获取学生个人信息（包含出生年月）
     */
    StudentProfileVO getStudentProfileByUserId(Long userId);

    /**
     * 更新学生信息
     */
    Student updateStudentInfo(Long userId, StudentInfoDTO request);

    /**
     * 获取学生控制台统计数据
     */
    Map<String, Object> getStudentStatistics(Long userId);
} 
package com.touhouqing.grabteacherbackend.service;

import com.touhouqing.grabteacherbackend.model.dto.StudentInfoDTO;
import com.touhouqing.grabteacherbackend.model.vo.StudentProfileVO;
import com.touhouqing.grabteacherbackend.model.entity.Student;
import java.util.Map;
import java.math.BigDecimal;

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

    /**
     * 管理员更新学生信息（包括余额）
     */
    Student updateStudentInfoByAdmin(Long userId, StudentInfoDTO request);

    /**
     * 更新学生余额（用于预约扣费和退费）
     */
    boolean updateStudentBalance(Long userId, BigDecimal amount, String reason);

    /**
     * 更新学生余额（支持关联预约ID和操作员ID）
     */
    boolean updateStudentBalance(Long userId, BigDecimal amount, String reason, Long bookingId, Long operatorId);

    /**
     * 检查学生余额是否足够
     */
    boolean checkBalance(Long userId, BigDecimal amount);
} 
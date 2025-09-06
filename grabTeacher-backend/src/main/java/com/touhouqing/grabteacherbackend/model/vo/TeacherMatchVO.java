package com.touhouqing.grabteacherbackend.model.vo;

import com.touhouqing.grabteacherbackend.model.dto.TimeSlotDTO;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * 教师匹配响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherMatchVO {
    
    /**
     * 教师ID
     */
    private Long id;
    
    /**
     * 教师姓名
     */
    private String name;
    
    /**
     * 科目
     */
    private String subject;
    
    
    /**
     * 教学经验年数
     */
    private Integer experience;
    
    /**
     * 描述/介绍
     */
    private String description;
    
    /**
     * 头像URL
     */
    private String avatar;
    
    /**
     * 标签列表
     */
    private List<String> tags;
    
    /**
     * 课程安排
     */
    private List<String> schedule;
    
    /**
     * 匹配分数 (0-100)
     */
    private Integer matchScore;
    
    /**
     * 每小时收费
     */
    private BigDecimal hourlyRate;
    
    /**
     * 教育背景
     */
    private String educationBackground;
    
    /**
     * 专业领域
     */
    private String specialties;
    
    /**
     * 是否已认证
     */
    private Boolean isVerified;

    /**
     * 教师性别
     */
    private String gender;

    /**
     * 可上课时间安排详情
     */
    private List<TimeSlotDTO> availableTimeSlots;

    /**
     * 时间匹配度 (0-100)
     */
    private Integer timeMatchScore;
}

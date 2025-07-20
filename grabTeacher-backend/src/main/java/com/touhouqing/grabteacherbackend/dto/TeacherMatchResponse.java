package com.touhouqing.grabteacherbackend.dto;

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
public class TeacherMatchResponse {
    
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
     * 年级
     */
    private String grade;
    
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
}

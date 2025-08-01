package com.touhouqing.grabteacherbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 教师详情响应DTO（用于学生查看教师详情）
 * 包含教师基本信息、用户信息、科目信息和年级信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "教师详情响应")
public class TeacherDetailResponse {
    
    @Schema(description = "教师ID", example = "1")
    private Long id;
    
    @Schema(description = "用户ID", example = "1")
    private Long userId;
    
    @Schema(description = "真实姓名", example = "张老师")
    private String realName;
    
    @Schema(description = "用户名", example = "zhangteacher")
    private String username;
    
    @Schema(description = "邮箱", example = "zhang@example.com")
    private String email;
    
    @Schema(description = "电话", example = "+65 8888 8888")
    private String phone;
    
    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatarUrl;
    
    @Schema(description = "教育背景", example = "北京大学数学系硕士")
    private String educationBackground;
    
    @Schema(description = "教学经验年数", example = "5")
    private Integer teachingExperience;
    
    @Schema(description = "专业特长", example = "高考数学,竞赛辅导")
    private String specialties;
    
    @Schema(description = "教授科目名称列表", example = "[\"数学\", \"物理\"]")
    private List<String> subjects;
    
    @Schema(description = "教授年级列表", example = "[\"初一\", \"初二\", \"初三\"]")
    private List<String> grades;
    
    @Schema(description = "小时收费", example = "100.00")
    private BigDecimal hourlyRate;
    
    @Schema(description = "个人介绍", example = "专业数学教师，擅长高考辅导")
    private String introduction;
    
    @Schema(description = "视频介绍URL", example = "https://example.com/video.mp4")
    private String videoIntroUrl;
    
    @Schema(description = "性别", example = "不愿透露", allowableValues = {"男", "女", "不愿透露"})
    private String gender;

    @Schema(description = "可上课时间安排")
    private List<TimeSlotDTO> availableTimeSlots;

    @Schema(description = "是否已认证", example = "true")
    private Boolean isVerified;

    @Schema(description = "是否已删除", example = "false")
    private Boolean isDeleted;

    @Schema(description = "删除时间")
    private LocalDateTime deletedAt;
}

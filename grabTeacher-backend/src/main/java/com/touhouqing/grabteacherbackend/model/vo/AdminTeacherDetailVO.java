package com.touhouqing.grabteacherbackend.model.vo;

import com.touhouqing.grabteacherbackend.model.dto.TimeSlotDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员端教师详情响应VO
 * 包含教师基本信息、用户信息、科目信息等完整信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "管理员端教师详情响应")
public class AdminTeacherDetailVO {
    
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
    
    @Schema(description = "出生年月", example = "1990-01")
    private String birthDate;
    
    @Schema(description = "教育背景", example = "北京大学数学系硕士")
    private String educationBackground;
    
    @Schema(description = "教学经验年数", example = "5")
    private Integer teachingExperience;
    
    @Schema(description = "专业特长", example = "高考数学,竞赛辅导")
    private String specialties;
    
    @Schema(description = "教授科目ID列表", example = "[1, 2, 3]")
    private List<Long> subjectIds;
    
    @Schema(description = "小时收费", example = "100.00")
    private BigDecimal hourlyRate;
    
    @Schema(description = "教师评分", example = "4.5")
    private BigDecimal rating;
    
    @Schema(description = "个人介绍", example = "专业数学教师，擅长高考辅导")
    private String introduction;
    
    @Schema(description = "视频介绍URL", example = "https://example.com/video.mp4")
    private String videoIntroUrl;
    
    @Schema(description = "性别", example = "不愿透露", allowableValues = {"男", "女", "不愿透露"})
    private String gender;

    @Schema(description = "教师级别", allowableValues = {"王牌", "金牌", "银牌", "铜牌"})
    private String level;

    @Schema(description = "可上课时间安排")
    private List<TimeSlotDTO> availableTimeSlots;

    @Schema(description = "是否已认证", example = "true")
    private Boolean verified;

    @Schema(description = "是否为精选教师", example = "false")
    private Boolean featured;

    @Schema(description = "本月剩余调课次数", example = "3")
    private Integer adjustmentTimes;

    @Schema(description = "本月课时（小时）", example = "12.5")
    private BigDecimal currentHours;

    @Schema(description = "上月课时（小时）", example = "30.0")
    private BigDecimal lastHours;

    @Schema(description = "是否已删除", example = "false")
    private Boolean deleted;

    @Schema(description = "删除时间")
    private LocalDateTime deletedAt;
}

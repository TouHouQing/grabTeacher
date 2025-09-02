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
 * 教师个人信息响应DTO
 * 包含教师基本信息和科目信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "教师个人信息响应")
public class TeacherProfileVO {

    @Schema(description = "教师ID", example = "1")
    private Long id;

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "真实姓名", example = "张老师")
    private String realName;

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

    @Schema(description = "个人介绍", example = "专业数学教师，擅长高考辅导")
    private String introduction;

    @Schema(description = "视频介绍URL", example = "https://example.com/video.mp4")
    private String videoIntroUrl;

    @Schema(description = "性别", example = "不愿透露", allowableValues = {"男", "女", "不愿透露"})
    private String gender;


    @Schema(description = "头像URL")
    private String avatarUrl;

    @Schema(description = "可上课时间安排")
    private List<TimeSlotDTO> availableTimeSlots;

    @Schema(description = "是否已认证", example = "true")
    private Boolean verified;

    @Schema(description = "本月剩余调课次数", example = "3")
    private Integer adjustmentTimes;

    @Schema(description = "是否已删除", example = "false")
    private Boolean deleted;

    @Schema(description = "删除时间")
    private LocalDateTime deletedAt;
}

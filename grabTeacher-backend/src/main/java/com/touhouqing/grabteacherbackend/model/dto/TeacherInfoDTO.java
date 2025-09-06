package com.touhouqing.grabteacherbackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherInfoDTO {
    private String realName;
    private String birthDate;
    @Schema(description = "学历", allowableValues = {"专科及以下", "本科", "硕士", "博士"})
    @Pattern(regexp = "^(专科及以下|本科|硕士|博士)$", message = "学历仅限：专科及以下、本科、硕士、博士")
    private String educationBackground;
    private Integer teachingExperience;
    private String specialties;

    @Schema(description = "教学科目ID列表", example = "[1, 2, 3]")
    private List<Long> subjectIds;

    private BigDecimal hourlyRate;
    private String introduction;
    private String videoIntroUrl;

    @Schema(description = "性别", example = "不愿透露", allowableValues = {"男", "女", "不愿透露"})
    private String gender;

    @Schema(description = "可上课时间安排")
    private List<TimeSlotDTO> availableTimeSlots;


    @Schema(description = "头像URL")
    private String avatarUrl;

    // 管理员添加教师时需要的账号信息
    @Schema(description = "用户名（管理员添加教师时必填）")
    private String username;

    @Schema(description = "邮箱（管理员添加教师时必填）")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "本月调课次数（管理员可编辑）")
    private Integer adjustmentTimes;

    @Schema(description = "本月课时（小时）")
    private BigDecimal currentHours;

    @Schema(description = "上月课时（小时）")
    private BigDecimal lastHours;

    @Schema(description = "教师评分", example = "4.5")
    private BigDecimal rating;
}
package com.touhouqing.grabteacherbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherInfoRequestDTO {
    private String realName;
    private String birthDate;
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

    // 管理员添加教师时需要的账号信息
    @Schema(description = "用户名（管理员添加教师时必填）")
    private String username;

    @Schema(description = "邮箱（管理员添加教师时必填）")
    private String email;

    @Schema(description = "手机号")
    private String phone;
}
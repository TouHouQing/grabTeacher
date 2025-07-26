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
public class TeacherInfoRequest {
    private String realName;
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
} 
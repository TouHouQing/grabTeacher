package com.touhouqing.grabteacherbackend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherInfoRequest {
    private String realName;
    private String educationBackground;
    private Integer teachingExperience;
    private String specialties;
    private String subjects;
    private BigDecimal hourlyRate;
    private String introduction;
    private String videoIntroUrl;
} 
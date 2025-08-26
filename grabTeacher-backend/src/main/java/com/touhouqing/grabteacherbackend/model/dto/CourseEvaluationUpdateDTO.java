package com.touhouqing.grabteacherbackend.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CourseEvaluationUpdateDTO {
    private Long id; // 由路径参数提供，不能在DTO上做@NotNull校验（校验发生在赋值之前）
    // IDs 改为可选
    private Long teacherId;
    private Long studentId;
    private Long courseId;

    // 名称字段必填
    @NotBlank
    private String teacherName;
    @NotBlank
    private String studentName;
    @NotBlank
    private String courseName;

    @NotBlank
    @Size(max = 255)
    private String studentComment;

    @NotNull
    @DecimalMin(value = "0")
    @DecimalMax(value = "5")
    @Digits(integer = 1, fraction = 2)
    private BigDecimal rating;

    private Boolean isFeatured;
}



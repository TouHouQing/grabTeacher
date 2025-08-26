package com.touhouqing.grabteacherbackend.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CourseEvaluationCreateDTO {
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

    // 是否精选，可选；默认后端按false处理
    private Boolean isFeatured;
}



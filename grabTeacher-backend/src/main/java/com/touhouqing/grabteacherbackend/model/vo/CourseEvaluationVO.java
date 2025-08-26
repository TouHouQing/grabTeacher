package com.touhouqing.grabteacherbackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CourseEvaluationVO implements Serializable {
    private Long id;
    private Long teacherId;
    private Long studentId;
    private Long courseId;
    private String teacherName;
    private String studentName;
    private String courseName;
    private String studentComment;
    private BigDecimal rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isFeatured;
}



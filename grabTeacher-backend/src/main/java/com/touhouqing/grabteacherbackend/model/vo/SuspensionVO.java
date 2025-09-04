package com.touhouqing.grabteacherbackend.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SuspensionVO {
    private Long id;
    private Long enrollmentId;
    private Long studentId;
    private Long teacherId;
    private String reason;
    private String status;
    private String statusDisplay;
    private Long adminId;
    private String adminNotes;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 展示信息
    private String courseTitle;
    private String subjectName;
    private String teacherName;
    private String studentName;
}



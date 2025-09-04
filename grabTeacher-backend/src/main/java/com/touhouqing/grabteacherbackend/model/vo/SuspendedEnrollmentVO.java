package com.touhouqing.grabteacherbackend.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SuspendedEnrollmentVO {
    private Long id;
    private Long studentId;
    private String studentName;
    private Long teacherId;
    private String teacherName;
    private Long courseId;
    private String courseTitle;
    private String subjectName;
    private Integer totalTimes;
    private Integer completedTimes;
}



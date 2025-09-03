package com.touhouqing.grabteacherbackend.model.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class CourseScheduleDetail {
    private Long id;
    private Long enrollmentId;
    private LocalDate scheduledDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer sessionNumber;
    private String scheduleStatus;

    private Long teacherId;
    private Long studentId;
    private Long courseId;
}



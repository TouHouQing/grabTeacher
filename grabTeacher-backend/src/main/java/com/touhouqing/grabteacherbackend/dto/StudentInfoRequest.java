package com.touhouqing.grabteacherbackend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentInfoRequest {
    private String realName;
    private String gradeLevel;
    private String subjectsInterested;
    private String learningGoals;
    private String preferredTeachingStyle;
    private String budgetRange;
} 
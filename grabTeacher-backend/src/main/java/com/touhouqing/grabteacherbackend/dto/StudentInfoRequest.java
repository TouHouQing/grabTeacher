package com.touhouqing.grabteacherbackend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentInfoRequest {
    private String realName;
    private String gradeLevel;
    private String subjectsInterested; // 保留兼容性，但优先使用subjectIds
    private List<Long> subjectIds; // 新增：感兴趣的科目ID列表
    private String learningGoals;
    private String preferredTeachingStyle;
    private String budgetRange;
    private String gender;
}
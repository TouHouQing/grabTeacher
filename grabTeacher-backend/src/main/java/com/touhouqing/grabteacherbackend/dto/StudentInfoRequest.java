package com.touhouqing.grabteacherbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentInfoRequest {
    private String realName;
    private String birthDate;
    private String gradeLevel;
    private String subjectsInterested; // 保留兼容性，但优先使用subjectIds
    private List<Long> subjectIds; // 新增：感兴趣的科目ID列表
    private String learningGoals;
    private String preferredTeachingStyle;
    private String budgetRange;

    @Schema(description = "性别", example = "不愿透露", allowableValues = {"男", "女", "不愿透露"})
    private String gender;

    // 管理员添加学生时需要的账号信息
    @Schema(description = "用户名（管理员添加学生时必填）")
    private String username;

    @Schema(description = "邮箱（管理员添加学生时必填）")
    private String email;

    @Schema(description = "手机号")
    private String phone;
}
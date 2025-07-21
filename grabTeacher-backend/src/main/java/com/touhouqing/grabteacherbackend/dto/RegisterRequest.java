package com.touhouqing.grabteacherbackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    private String username;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度不能少于6位")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    @NotNull(message = "用户类型不能为空")
    private UserType userType;

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    private String phone;

    // 学生额外信息
    private String gradeLevel;
    private String subjectsInterested;
    private String learningGoals;
    private String preferredTeachingStyle;
    private String budgetRange;
    private String gender;

    // 教师额外信息
    private String educationBackground;
    private Integer teachingExperience;
    private String specialties;
    private String subjects;
    private BigDecimal hourlyRate;
    private String introduction;

    // 枚举类型
    public enum UserType {
        student, teacher, admin
    }
} 
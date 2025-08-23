package com.touhouqing.grabteacherbackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("students")
public class Student {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @NotBlank(message = "真实姓名不能为空")
    @TableField("real_name")
    private String realName;

    @TableField("grade_level")
    private String gradeLevel;
    
    @TableField("subjects_interested")
    private String subjectsInterested;
    
    @TableField("learning_goals")
    private String learningGoals;
    
    @TableField("preferred_teaching_style")
    private String preferredTeachingStyle;
    
    @TableField("budget_range")
    private String budgetRange;
    
    @TableField("balance")
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @TableField("gender")
    private String gender;

    @TableField("is_deleted")
    @Builder.Default
    private Boolean deleted = false;
    
    @TableField("deleted_at")
    private LocalDateTime deletedAt;

    // 非持久化字段：用户头像URL，用于回显
    @TableField(exist = false)
    private String avatarUrl;

    // 保留必要的构造函数
    public Student(Long userId, String realName) {
        this.userId = userId;
        this.realName = realName;
        this.deleted = false;
    }
}
package com.touhouqing.grabteacherbackend.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

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

    @TableField("is_deleted")
    @TableLogic
    @Builder.Default
    private Boolean isDeleted = false;
    
    @TableField("deleted_at")
    private LocalDateTime deletedAt;

    // 保留必要的构造函数
    public Student(Long userId, String realName) {
        this.userId = userId;
        this.realName = realName;
        this.isDeleted = false;
    }
} 
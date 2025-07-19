package com.touhouqing.grabteacherbackend.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("teachers")
public class Teacher {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @NotBlank(message = "真实姓名不能为空")
    @TableField("real_name")
    private String realName;

    @TableField("education_background")
    private String educationBackground;

    @TableField("teaching_experience")
    private Integer teachingExperience;

    private String specialties;

    private String subjects;

    @TableField("hourly_rate")
    private BigDecimal hourlyRate;

    private String introduction;

    @TableField("video_intro_url")
    private String videoIntroUrl;

    @TableField("is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    @TableField("is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;

    // 保留必要的构造函数
    public Teacher(Long userId, String realName) {
        this.userId = userId;
        this.realName = realName;
        this.isVerified = false;
        this.isDeleted = false;
    }
} 
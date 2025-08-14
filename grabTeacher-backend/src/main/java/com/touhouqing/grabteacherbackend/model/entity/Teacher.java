package com.touhouqing.grabteacherbackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @TableField("hourly_rate")
    private BigDecimal hourlyRate;

    private String introduction;

    @TableField("video_intro_url")
    private String videoIntroUrl;

    @Schema(description = "性别", example = "不愿透露", allowableValues = {"男", "女", "不愿透露"})
    private String gender;

    @TableField("available_time_slots")
    @Schema(description = "可上课时间安排", example = "[{\"weekday\":1,\"timeSlots\":[\"08:00-09:00\",\"10:00-11:00\"]},{\"weekday\":2,\"timeSlots\":[\"14:00-15:00\"]}]")
    private String availableTimeSlots;

    @TableField("is_verified")
    @Builder.Default
    private Boolean verified = false;

    @TableField("is_featured")
    @Builder.Default
    private Boolean featured = false;

    @TableField("is_deleted")
    @Builder.Default
    private Boolean deleted = false;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;

    // 保留必要的构造函数
    public Teacher(Long userId, String realName) {
        this.userId = userId;
        this.realName = realName;
        this.verified = false;
        this.deleted = false;
    }
} 
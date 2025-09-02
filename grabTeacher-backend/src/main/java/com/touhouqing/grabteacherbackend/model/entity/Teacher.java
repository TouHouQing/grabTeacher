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

    @Schema(description = "教师基础评分", example = "4.5")
    @TableField("base_rating")
    private BigDecimal baseRating;

    @Schema(description = "教师评分", example = "4.5")
    @TableField("rating")
    private BigDecimal rating;

    @TableField("education_background")
    private String educationBackground;

    @TableField("teaching_experience")
    private Integer teachingExperience;

    @Schema(description = "教师专长", example = "高中数学")
    @TableField("specialties")
    private String specialties;

    @TableField("hourly_rate")
    private BigDecimal hourlyRate;

    @Schema(description = "教师介绍", example = "我是一个专业的教师，擅长高中数学")
    @TableField("introduction")
    private String introduction;

    @Schema(description = "教师视频介绍URL", example = "https://example.com/teacher-video.mp4")
    @TableField("video_intro_url")
    private String videoIntroUrl;

    @Schema(description = "性别", example = "不愿透露", allowableValues = {"男", "女", "不愿透露"})
    @TableField("gender")
    private String gender;

    @TableField("available_time_slots")
    @Schema(description = "可上课时间安排", example = "[{\"weekday\":1,\"timeSlots\":[\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":6,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\"]}]")
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

    // 非持久化字段：用户头像URL，用于回显
    @TableField(exist = false)
    private String avatarUrl;

    @Schema(description = "本月课时（小时）", example = "12.5")
    @TableField("current_hours")
    @Builder.Default
    private BigDecimal currentHours = BigDecimal.ZERO;

    @Schema(description = "上月课时（小时）", example = "30.0")
    @TableField("last_hours")
    @Builder.Default
    private BigDecimal lastHours = BigDecimal.ZERO;

    // 保留必要的构造函数
    public Teacher(Long userId, String realName) {
        this.userId = userId;
        this.realName = realName;
        this.verified = false;
        this.deleted = false;
    }
}
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
import java.util.List;

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

    @Schema(description = "性别", example = "不愿透露", allowableValues = {"男", "女", "不愿透露"})
    @TableField("gender")
    private String gender;


    @Schema(description = "教师级别（从教师级别表选择）")
    @TableField("level")
    private String level;

    @TableField("supports_online")
    @Schema(description = "是否支持线上授课", example = "true")
    private Boolean supportsOnline;

    @TableField("teaching_locations")
    @Schema(description = "授课地点ID列表，逗号分隔；为空代表无线下地点")
    private String teachingLocations;

    // 兼容旧字段：周模板可用时间，已废弃；为避免无列导致SQL错误，标记为非持久化
    @TableField(exist = false)
    @Schema(description = "[Deprecated] 周模板可上课时间，仅用于向后兼容前端旧接口", example = "[{\"weekday\":1,\"timeSlots\":[\"17:00-19:00\",\"19:00-21:00\"]}]")
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

    // 非持久化字段：教师教授的科目ID列表，用于回显
    @TableField(exist = false)
    private List<Long> subjectIds;

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
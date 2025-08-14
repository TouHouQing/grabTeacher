package com.touhouqing.grabteacherbackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("courses")
@Schema(description = "课程实体")
public class Course {
    @TableId(type = IdType.AUTO)
    @Schema(description = "课程ID", example = "1")
    private Long id;

    @NotNull(message = "教师ID不能为空")
    @TableField("teacher_id")
    @Schema(description = "授课教师ID", example = "1", required = true)
    private Long teacherId;

    @NotNull(message = "科目ID不能为空")
    @TableField("subject_id")
    @Schema(description = "课程科目ID", example = "1", required = true)
    private Long subjectId;

    @NotBlank(message = "课程标题不能为空")
    @Size(max = 200, message = "课程标题长度不能超过200个字符")
    @Schema(description = "课程标题", example = "高中数学 - 函数与导数专题", required = true)
    private String title;

    @Schema(description = "课程详细描述", example = "本课程深入讲解高中数学中的函数与导数知识点，适合高二、高三学生")
    private String description;

    @NotNull(message = "课程类型不能为空")
    @TableField("course_type")
    @Schema(description = "课程类型", example = "one_on_one", allowableValues = {"one_on_one", "large_class"}, required = true)
    private String courseType;

    @NotNull(message = "课程时长不能为空")
    @Min(value = 1, message = "课程时长必须大于0分钟")
    @TableField("duration_minutes")
    @Schema(description = "单次课程时长（分钟）", example = "120", required = true)
    private Integer durationMinutes;

    @Builder.Default
    @Schema(description = "课程状态", example = "pending", allowableValues = {"active", "inactive", "full", "pending"})
    private String status = "pending";

    @TableField("is_featured")
    @Builder.Default
    @Schema(description = "是否为精选课程，在最新课程页面展示", example = "false")
    private Boolean featured = false;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    @Schema(description = "课程创建时间")
    private LocalDateTime createdAt;

    @TableField("is_deleted")
    @Builder.Default
    @Schema(description = "是否删除", example = "false", hidden = true)
    private Boolean deleted = false;

    @TableField("deleted_at")
    @Schema(description = "删除时间", hidden = true)
    private LocalDateTime deletedAt;

    // 保留必要的构造函数
    public Course(Long teacherId, Long subjectId, String title, String courseType, Integer durationMinutes) {
        this.teacherId = teacherId;
        this.subjectId = subjectId;
        this.title = title;
        this.courseType = courseType;
        this.durationMinutes = durationMinutes;
        this.status = "pending";
        this.featured = false;
        this.deleted = false;
    }
}

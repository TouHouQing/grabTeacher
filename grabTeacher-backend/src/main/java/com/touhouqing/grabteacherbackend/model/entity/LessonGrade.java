package com.touhouqing.grabteacherbackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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
@TableName("lesson_grades")
@Schema(description = "课程成绩实体")
public class LessonGrade {
    
    @TableId(type = IdType.AUTO)
    @Schema(description = "成绩记录ID", example = "1")
    private Long id;

    @NotNull(message = "课程安排ID不能为空")
    @TableField("schedule_id")
    @Schema(description = "课程安排ID，关联schedules表", example = "1", required = true)
    private Long scheduleId;

    @NotNull(message = "学生ID不能为空")
    @TableField("student_id")
    @Schema(description = "学生ID，关联students表", example = "1", required = true)
    private Long studentId;

    @DecimalMin(value = "0.00", message = "成绩不能小于0")
    @DecimalMax(value = "100.00", message = "成绩不能大于100")
    @TableField("score")
    @Schema(description = "本节课成绩分数", example = "85.5")
    private BigDecimal score;

    @TableField("teacher_comment")
    @Schema(description = "教师对本节课的评价和建议", example = "学生表现良好，需要加强练习")
    private String teacherComment;

    @TableField(value = "graded_at", fill = FieldFill.INSERT)
    @Schema(description = "成绩录入时间")
    private LocalDateTime gradedAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @TableField("is_deleted")
    @Builder.Default
    @Schema(description = "是否删除", example = "false", hidden = true)
    private Boolean deleted = false;

    @TableField("deleted_at")
    @Schema(description = "删除时间", hidden = true)
    private LocalDateTime deletedAt;
}

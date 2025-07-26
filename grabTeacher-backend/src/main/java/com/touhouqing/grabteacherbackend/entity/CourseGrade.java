package com.touhouqing.grabteacherbackend.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 课程年级关联实体
 * 用于存储课程与年级的多对多关系
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("course_grades")
@Schema(description = "课程年级关联实体")
public class CourseGrade {
    
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID", example = "1")
    private Long id;

    @NotNull(message = "课程ID不能为空")
    @TableField("course_id")
    @Schema(description = "课程ID", example = "1", required = true)
    private Long courseId;

    @NotBlank(message = "年级不能为空")
    @Schema(description = "年级", example = "小学一年级", required = true)
    private String grade;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    // 构造函数
    public CourseGrade(Long courseId, String grade) {
        this.courseId = courseId;
        this.grade = grade;
    }
}

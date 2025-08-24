package com.touhouqing.grabteacherbackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "更新课程成绩请求DTO")
public class LessonGradeUpdateDTO {

    @NotNull(message = "成绩记录ID不能为空")
    @Schema(description = "成绩记录ID", example = "1", required = true)
    private Long id;

    @DecimalMin(value = "0.00", message = "成绩不能小于0")
    @DecimalMax(value = "100.00", message = "成绩不能大于100")
    @Schema(description = "本节课成绩分数", example = "85.5")
    private BigDecimal score;

    @Schema(description = "教师对本节课的评价和建议", example = "学生表现良好，需要加强练习")
    private String teacherComment;
}

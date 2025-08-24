package com.touhouqing.grabteacherbackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "课程成绩查询请求DTO")
public class LessonGradeQueryDTO {

    @Schema(description = "学生ID", example = "1")
    private Long studentId;

    @Schema(description = "教师ID", example = "1")
    private Long teacherId;

    @Schema(description = "课程ID", example = "1")
    private Long courseId;

    @Schema(description = "科目名称", example = "数学")
    private String subjectName;

    @Schema(description = "开始日期", example = "2024-01-01")
    private LocalDate startDate;

    @Schema(description = "结束日期", example = "2024-12-31")
    private LocalDate endDate;

    @Builder.Default
    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Builder.Default
    @Schema(description = "每页大小", example = "20")
    private Integer size = 20;
}

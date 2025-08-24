package com.touhouqing.grabteacherbackend.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "课程成绩响应VO")
public class LessonGradeVO {

    @Schema(description = "成绩记录ID", example = "1")
    private Long id;

    @Schema(description = "课程安排ID", example = "1")
    private Long scheduleId;

    @Schema(description = "学生ID", example = "1")
    private Long studentId;

    @Schema(description = "学生姓名", example = "张三")
    private String studentName;

    @Schema(description = "教师ID", example = "1")
    private Long teacherId;

    @Schema(description = "教师姓名", example = "李老师")
    private String teacherName;

    @Schema(description = "课程ID", example = "1")
    private Long courseId;

    @Schema(description = "课程标题", example = "高中数学 - 函数与导数专题")
    private String courseTitle;

    @Schema(description = "科目名称", example = "数学")
    private String subjectName;

    @Schema(description = "上课日期", example = "2024-01-15")
    private LocalDate scheduledDate;

    @Schema(description = "开始时间", example = "14:00")
    private LocalTime startTime;

    @Schema(description = "结束时间", example = "16:00")
    private LocalTime endTime;

    @Schema(description = "本节课成绩分数", example = "85.5")
    private BigDecimal score;

    @Schema(description = "教师对本节课的评价和建议", example = "学生表现良好，需要加强练习")
    private String teacherComment;

    @Schema(description = "成绩录入时间")
    private LocalDateTime gradedAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}

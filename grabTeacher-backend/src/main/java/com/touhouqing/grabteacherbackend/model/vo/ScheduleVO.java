package com.touhouqing.grabteacherbackend.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * 课程安排响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "课程安排响应数据")
public class ScheduleVO {
    
    @Schema(description = "排课ID", example = "1")
    private Long id;

    @Schema(description = "教师ID", example = "1")
    private Long teacherId;

    @Schema(description = "教师姓名", example = "李老师")
    private String teacherName;

    @Schema(description = "学生ID", example = "1")
    private Long studentId;

    @Schema(description = "学生姓名", example = "张三")
    private String studentName;

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

    @Schema(description = "课程时长（分钟）", example = "120")
    private Integer durationMinutes;

    @Schema(description = "总课程次数", example = "12")
    private Integer totalTimes;

    @Schema(description = "课程状态", example = "progressing", allowableValues = {"progressing", "completed", "cancelled"})
    private String status;

    @Schema(description = "教师课后备注和反馈", example = "学生表现良好，需要加强练习")
    private String teacherNotes;

    @Schema(description = "学生课后反馈", example = "老师讲解很清楚，收获很大")
    private String studentFeedback;

    @Schema(description = "排课创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "关联预约申请ID", example = "1")
    private Long bookingRequestId;

    @Schema(description = "预约来源", example = "request", allowableValues = {"request", "admin"})
    private String bookingSource;

    @Schema(description = "是否为试听课", example = "false")
    private Boolean isTrial;

    @Schema(description = "课程序号（在周期性课程中的第几次课）", example = "1")
    private Integer sessionNumber;

    @Schema(description = "课程类型", example = "one_on_one", allowableValues = {"one_on_one", "large_class"})
    private String courseType;
}

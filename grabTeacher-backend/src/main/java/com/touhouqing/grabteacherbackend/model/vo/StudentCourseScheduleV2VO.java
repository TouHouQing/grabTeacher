package com.touhouqing.grabteacherbackend.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "学生端课程卡片内节次（V2）")
public class StudentCourseScheduleV2VO {
    @Schema(description = "排课ID")
    private Long id;

    @Schema(description = "上课日期")
    private LocalDate scheduledDate;

    @Schema(description = "开始时间")
    private LocalTime startTime;

    @Schema(description = "结束时间")
    private LocalTime endTime;

    @Schema(description = "课程序号")
    private Integer sessionNumber;

    @Schema(description = "状态：progressing/completed/cancelled 等")
    private String status;

    @Schema(description = "教师ID")
    private Long teacherId;

    @Schema(description = "学生ID")
    private Long studentId;

    @Schema(description = "课程ID")
    private Long courseId;

    @Schema(description = "报名ID")
    private Long enrollmentId;

    @Schema(description = "预约ID")
    private Long bookingRequestId;

    @Schema(description = "本节课时长(分钟)")
    private Integer durationMinutes;
}

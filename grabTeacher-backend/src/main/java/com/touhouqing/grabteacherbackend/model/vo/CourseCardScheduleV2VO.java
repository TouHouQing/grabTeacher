package com.touhouqing.grabteacherbackend.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 学生端“我的课程”卡片内的排课条目（V2）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "课程卡片内的单条课程安排（V2）")
public class CourseCardScheduleV2VO {
    @Schema(description = "排课ID")
    private Long id;

    @Schema(description = "上课日期")
    private LocalDate scheduledDate;

    @Schema(description = "开始时间")
    private LocalTime startTime;

    @Schema(description = "结束时间")
    private LocalTime endTime;

    @Schema(description = "课程序号（在该报名下的第几次课）")
    private Integer sessionNumber;

    @Schema(description = "状态：progressing/completed/cancelled 等（与旧前端口径兼容）")
    private String status;
}


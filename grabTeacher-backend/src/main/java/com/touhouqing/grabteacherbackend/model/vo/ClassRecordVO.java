package com.touhouqing.grabteacherbackend.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "上课记录VO")
public class ClassRecordVO {

    @Schema(description = "课程安排ID")
    private Long scheduleId;

    @Schema(description = "上课日期")
    private LocalDate scheduledDate;

    @Schema(description = "上课时间")
    private LocalTime startTime;

    @Schema(description = "结束时间")
    private LocalTime endTime;

    @Schema(description = "课程名称")
    private String courseName;

    @Schema(description = "学生姓名")
    private String studentName;

    @Schema(description = "课程类型")
    private String courseType;

    @Schema(description = "教师姓名")
    private String teacherName;

    @Schema(description = "第几次课")
    private Integer sessionNumber;

    @Schema(description = "是否为试听课")
    private Boolean isTrial;

    @Schema(description = "教师课后备注")
    private String teacherNotes;

    @Schema(description = "学生课后反馈")
    private String studentFeedback;

    @Schema(description = "课程时长（分钟）")
    private Integer durationMinutes;

    @Schema(description = "教师时薪（仅一对一&非试听），单位：M豆/小时")
    private BigDecimal teacherHourlyRate;
}

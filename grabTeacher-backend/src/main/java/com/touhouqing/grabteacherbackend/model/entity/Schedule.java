package com.touhouqing.grabteacherbackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("schedules")
@Schema(description = "课程安排实体")
public class Schedule {
    
    @TableId(type = IdType.AUTO)
    @Schema(description = "排课ID", example = "1")
    private Long id;

    @NotNull(message = "教师ID不能为空")
    @TableField("teacher_id")
    @Schema(description = "授课教师ID", example = "1", required = true)
    private Long teacherId;

    @NotNull(message = "学生ID不能为空")
    @TableField("student_id")
    @Schema(description = "学生ID", example = "1", required = true)
    private Long studentId;

    @NotNull(message = "课程ID不能为空")
    @TableField("course_id")
    @Schema(description = "课程ID", example = "1", required = true)
    private Long courseId;

    @NotNull(message = "上课日期不能为空")
    @TableField("scheduled_date")
    @Schema(description = "上课日期", example = "2024-01-15", required = true)
    private LocalDate scheduledDate;

    @NotNull(message = "开始时间不能为空")
    @TableField("start_time")
    @Schema(description = "开始时间", example = "14:00", required = true)
    private LocalTime startTime;

    @NotNull(message = "结束时间不能为空")
    @TableField("end_time")
    @Schema(description = "结束时间", example = "16:00", required = true)
    private LocalTime endTime;

    @TableField("total_times")
    @Schema(description = "总课程次数", example = "12")
    private Integer totalTimes;

    @Builder.Default
    @Schema(description = "课程状态", example = "progressing", allowableValues = {"progressing", "completed", "cancelled"})
    private String status = "progressing";

    @TableField("teacher_notes")
    @Schema(description = "教师课后备注和反馈", example = "学生表现良好，需要加强练习")
    private String teacherNotes;

    @TableField("student_feedback")
    @Schema(description = "学生课后反馈", example = "老师讲解很清楚，收获很大")
    private String studentFeedback;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    @Schema(description = "排课创建时间")
    private LocalDateTime createdAt;

    @TableField("booking_request_id")
    @Schema(description = "关联预约申请ID", example = "1")
    private Long bookingRequestId;

    @Builder.Default
    @TableField("booking_source")
    @Schema(description = "预约来源", example = "request", allowableValues = {"request", "admin"})
    private String bookingSource = "request";

    @TableField("is_deleted")
    @Builder.Default
    @Schema(description = "是否删除", example = "false", hidden = true)
    private Boolean deleted = false;

    @TableField("deleted_at")
    @Schema(description = "删除时间", hidden = true)
    private LocalDateTime deletedAt;

    @TableField("recurring_weekdays")
    @Schema(description = "周期性预约的星期几，逗号分隔", example = "1,3,5")
    private String recurringWeekdays;

    @TableField("recurring_time_slots")
    @Schema(description = "周期性预约的时间段，逗号分隔", example = "14:00-16:00,18:00-20:00")
    private String recurringTimeSlots;

    // 新增字段：是否为试听课
    @TableField("is_trial")
    @Builder.Default
    @Schema(description = "是否为试听课", example = "false")
    private Boolean trial = false;

    // 新增字段：课程序号（在周期性课程中的第几次课）
    @TableField("session_number")
    @Schema(description = "课程序号（在周期性课程中的第几次课）", example = "1")
    private Integer sessionNumber;

    // 保留必要的构造函数
    public Schedule(Long teacherId, Long studentId, Long courseId, LocalDate scheduledDate, 
                   LocalTime startTime, LocalTime endTime) {
        this.teacherId = teacherId;
        this.studentId = studentId;
        this.courseId = courseId;
        this.scheduledDate = scheduledDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = "progressing";
        this.bookingSource = "request";
        this.deleted = false;
        this.trial = false;
    }
}

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
@TableName("booking_requests")
@Schema(description = "预约申请实体")
public class BookingRequest {

    @TableId(type = IdType.AUTO)
    @Schema(description = "预约申请ID", example = "1")
    private Long id;

    @NotNull(message = "学生ID不能为空")
    @TableField("student_id")
    @Schema(description = "学生ID", example = "1", required = true)
    private Long studentId;

    @NotNull(message = "教师ID不能为空")
    @TableField("teacher_id")
    @Schema(description = "教师ID", example = "1", required = true)
    private Long teacherId;

    @TableField("course_id")
    @Schema(description = "课程ID，可为空(自定义预约)", example = "1")
    private Long courseId;

    @NotNull(message = "预约类型不能为空")
    @TableField("booking_type")
    @Schema(description = "预约类型", example = "single", allowableValues = {"single", "recurring"}, required = true)
    private String bookingType;

    @TableField("requested_date")
    @Schema(description = "请求的上课日期(单次预约)", example = "2024-01-15")
    private LocalDate requestedDate;

    @TableField("requested_start_time")
    @Schema(description = "请求的开始时间(单次预约)", example = "14:00")
    private LocalTime requestedStartTime;

    @TableField("requested_end_time")
    @Schema(description = "请求的结束时间(单次预约)", example = "16:00")
    private LocalTime requestedEndTime;

    @TableField("recurring_weekdays")
    @Schema(description = "周期性预约的星期几，逗号分隔", example = "1,3,5")
    private String recurringWeekdays;

    @TableField("recurring_time_slots")
    @Schema(description = "周期性预约的时间段，逗号分隔", example = "14:00-16:00,18:00-20:00")
    private String recurringTimeSlots;

    @TableField("start_date")
    @Schema(description = "周期性预约开始日期", example = "2024-01-15")
    private LocalDate startDate;

    @TableField("end_date")
    @Schema(description = "周期性预约结束日期", example = "2024-06-15")
    private LocalDate endDate;

    @TableField("total_times")
    @Schema(description = "总课程次数", example = "12")
    private Integer totalTimes;

    @TableField("student_requirements")
    @Schema(description = "学生需求说明", example = "希望重点提高数学成绩")
    private String studentRequirements;


    @TableField(exist = false)
    @Schema(description = "年级（不入库，仅用于流程传递）", example = "高一")
    private String grade;


    @Builder.Default
    @Schema(description = "申请状态", example = "pending", allowableValues = {"pending", "approved", "rejected", "cancelled"})
    private String status = "pending";

    @TableField("teacher_reply")
    @Schema(description = "教师回复内容", example = "同意预约，期待与您的合作")
    private String teacherReply;

    @TableField("admin_notes")
    @Schema(description = "管理员备注", example = "特殊情况处理")
    private String adminNotes;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    @Schema(description = "申请时间")
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @TableField("approved_at")
    @Schema(description = "批准时间")
    private LocalDateTime approvedAt;

    @TableField("is_deleted")
    @Builder.Default
    @Schema(description = "是否删除", example = "false", hidden = true)
    private Boolean deleted = false;

    @TableField("deleted_at")
    @Schema(description = "删除时间", hidden = true)
    private LocalDateTime deletedAt;

    // 新增字段：是否为免费试听课
    @TableField("is_trial")
    @Builder.Default
    @Schema(description = "是否为免费试听课（试听课次数有限）", example = "false")
    private Boolean isTrial = false;

    // 新增字段：试听课时长（分钟）
    @TableField("trial_duration_minutes")
    @Schema(description = "试听课时长（分钟）", example = "30")
    private Integer trialDurationMinutes;

    // 保留必要的构造函数
    public BookingRequest(Long studentId, Long teacherId, String bookingType) {
        this.studentId = studentId;
        this.teacherId = teacherId;
        this.bookingType = bookingType;
        this.status = "pending";
        this.deleted = false;
        this.isTrial = false;
    }
}

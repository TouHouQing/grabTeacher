package com.touhouqing.grabteacherbackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 调课申请实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("reschedule_requests")
@Schema(description = "调课申请")
public class RescheduleRequest {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "调课申请ID", example = "1")
    private Long id;

    @TableField("schedule_id")
    @Schema(description = "原课程安排ID", example = "1", required = true)
    private Long scheduleId;

    @TableField("applicant_id")
    @Schema(description = "申请人ID(学生或教师)", example = "1", required = true)
    private Long applicantId;

    @TableField("applicant_type")
    @Schema(description = "申请人类型", example = "student", allowableValues = {"student", "teacher"}, required = true)
    private String applicantType;

    @TableField("request_type")
    @Schema(description = "申请类型", example = "single", allowableValues = {"single", "recurring", "cancel"}, required = true)
    private String requestType;

    @TableField("original_date")
    @Schema(description = "原定日期", example = "2024-01-15", required = true)
    private LocalDate originalDate;

    @TableField("original_start_time")
    @Schema(description = "原定开始时间", example = "14:00", required = true)
    private LocalTime originalStartTime;

    @TableField("original_end_time")
    @Schema(description = "原定结束时间", example = "16:00", required = true)
    private LocalTime originalEndTime;

    @TableField("new_date")
    @Schema(description = "新日期", example = "2024-01-16")
    private LocalDate newDate;

    @TableField("new_start_time")
    @Schema(description = "新开始时间", example = "15:00")
    private LocalTime newStartTime;

    @TableField("new_end_time")
    @Schema(description = "新结束时间", example = "17:00")
    private LocalTime newEndTime;

    @TableField("new_weekly_schedule")
    @Schema(description = "新的周期性安排", example = "周一,周三 14:00-16:00;周五 18:00-20:00")
    private String newWeeklySchedule;

    @TableField("reason")
    @Schema(description = "调课原因", example = "临时有事无法参加", required = true)
    private String reason;

    @TableField("urgency_level")
    @Schema(description = "紧急程度", example = "medium", allowableValues = {"low", "medium", "high"})
    @Builder.Default
    private String urgencyLevel = "medium";

    @TableField("advance_notice_hours")
    @Schema(description = "提前通知小时数", example = "24")
    private Integer advanceNoticeHours;

    @TableField("status")
    @Schema(description = "申请状态", example = "pending", allowableValues = {"pending", "approved", "rejected", "cancelled"})
    @Builder.Default
    private String status = "pending";

    @TableField("reviewer_id")
    @Schema(description = "审核人ID", example = "1")
    private Long reviewerId;

    @TableField("reviewer_type")
    @Schema(description = "审核人类型", example = "teacher", allowableValues = {"teacher", "student", "admin"})
    private String reviewerType;

    @TableField("review_notes")
    @Schema(description = "审核备注", example = "同意调课申请")
    private String reviewNotes;

    @TableField("compensation_amount")
    @Schema(description = "补偿金额(如需要)", example = "0.00")
    @Builder.Default
    private BigDecimal compensationAmount = BigDecimal.ZERO;

    @TableField("affects_future_sessions")
    @Schema(description = "是否影响后续课程", example = "false")
    @Builder.Default
    private Boolean affectsFutureSessions = false;

    @TableField("reviewed_at")
    @Schema(description = "审核时间")
    private LocalDateTime reviewedAt;



    @TableField("created_at")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @TableField("is_deleted")
    @Builder.Default
    @Schema(description = "是否删除", example = "false", hidden = true)
    private Boolean deleted = false;

    @TableField("deleted_at")
    @Schema(description = "删除时间", hidden = true)
    private LocalDateTime deletedAt;
}

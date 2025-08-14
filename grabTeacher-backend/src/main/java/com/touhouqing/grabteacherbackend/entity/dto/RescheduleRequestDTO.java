package com.touhouqing.grabteacherbackend.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 调课申请请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "调课申请请求参数")
public class RescheduleRequestDTO {

    @NotNull(message = "课程安排ID不能为空")
    @Schema(description = "原课程安排ID", example = "1")
    private Long scheduleId;

    @NotBlank(message = "申请类型不能为空")
    @Schema(description = "申请类型", example = "single", allowableValues = {"single", "recurring", "cancel"})
    private String requestType;

    // 单次调课相关字段
    @Schema(description = "新日期(单次调课)", example = "2024-01-16")
    private LocalDate newDate;

    @Schema(description = "新开始时间(单次调课)", example = "15:00")
    private LocalTime newStartTime;

    @Schema(description = "新结束时间(单次调课)", example = "17:00")
    private LocalTime newEndTime;

    // 周期性调课相关字段
    @Schema(description = "新的周期性安排的星期几列表", example = "[1, 3, 5]")
    private List<Integer> newRecurringWeekdays;

    @Schema(description = "新的周期性安排的时间段列表", example = "[\"14:00-16:00\", \"18:00-20:00\"]")
    private List<String> newRecurringTimeSlots;

    @NotBlank(message = "调课原因不能为空")
    @Schema(description = "调课原因", example = "临时有事无法参加")
    private String reason;

    @Schema(description = "紧急程度", example = "medium", allowableValues = {"low", "medium", "high"})
    @Builder.Default()
    private String urgencyLevel = "medium";

    @Schema(description = "提前通知小时数", example = "24")
    private Integer advanceNoticeHours;

    @Schema(description = "是否影响后续课程", example = "false")
    private Boolean affectsFutureSessions;

    @Schema(description = "备注信息", example = "希望老师能够同意调课申请")
    private String notes;
}

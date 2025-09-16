package com.touhouqing.grabteacherbackend.model.dto;

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
public class RescheduleApplyDTO {

    @NotNull(message = "课程安排ID不能为空")
    @Schema(description = "原课程安排ID", example = "1")
    private Long scheduleId;

    @NotBlank(message = "申请类型不能为空")
    @Schema(description = "申请类型", example = "reschedule", allowableValues = {"reschedule", "cancel"})
    private String requestType;

    // 单次调课相关字段
    @Schema(description = "新日期(单次调课)", example = "2024-01-16")
    private LocalDate newDate;

    @Schema(description = "新开始时间(单次调课)", example = "15:00")
    private LocalTime newStartTime;

    @Schema(description = "新结束时间(单次调课)", example = "17:00")
    private LocalTime newEndTime;

    // 教师端多选候选新时间（学生端忽略）
    @Schema(description = "候选新时间列表（教师端可多选）")
    private List<CandidateTimeDTO> candidateSessions;


    @NotBlank(message = "调课原因不能为空")
    @Schema(description = "调课原因", example = "临时有事无法参加")
    private String reason;

    @Schema(description = "紧急程度", example = "medium", allowableValues = {"low", "medium", "high"})
    @Builder.Default()
    private String urgencyLevel = "medium";

    @Schema(description = "提前通知小时数", example = "24")
    private Integer advanceNoticeHours;



    @Schema(description = "备注信息", example = "希望老师能够同意调课申请")
    private String notes;
}

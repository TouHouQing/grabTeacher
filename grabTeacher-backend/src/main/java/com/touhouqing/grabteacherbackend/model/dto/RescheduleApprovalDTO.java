package com.touhouqing.grabteacherbackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 调课申请审批DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "调课申请审批参数")
public class RescheduleApprovalDTO {

    @NotBlank(message = "审批状态不能为空")
    @Schema(description = "审批状态", example = "approved", allowableValues = {"approved", "rejected"})
    private String status;

    @NotBlank(message = "审批原因不能为空")
    @Schema(description = "审核备注", example = "同意调课申请，请按时参加新的课程安排")
    private String reviewNotes;

    @Schema(description = "补偿金额(如需要)", example = "0.00")
    private BigDecimal compensationAmount;

    @Schema(description = "是否影响后续课程", example = "false")
    private Boolean affectsFutureSessions;

    @Schema(description = "管理员最终选择的新日期（可选，若提供则覆盖申请中的新时间）", example = "2025-09-20")
    private LocalDate selectedNewDate;

    @Schema(description = "管理员最终选择的新开始时间（可选）", example = "15:00")
    private LocalTime selectedNewStartTime;

    @Schema(description = "管理员最终选择的新结束时间（可选）", example = "17:00")
    private LocalTime selectedNewEndTime;
}

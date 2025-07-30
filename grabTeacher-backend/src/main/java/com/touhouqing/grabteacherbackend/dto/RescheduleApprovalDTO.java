package com.touhouqing.grabteacherbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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
    @Schema(description = "审批状态", example = "approved", allowableValues = {"approved", "rejected"}, required = true)
    private String status;

    @Schema(description = "审核备注", example = "同意调课申请，请按时参加新的课程安排")
    private String reviewNotes;

    @Schema(description = "补偿金额(如需要)", example = "0.00")
    private BigDecimal compensationAmount;

    @Schema(description = "是否影响后续课程", example = "false")
    private Boolean affectsFutureSessions;
}

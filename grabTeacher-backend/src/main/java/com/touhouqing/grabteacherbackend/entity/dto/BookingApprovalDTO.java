package com.touhouqing.grabteacherbackend.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * 预约审批DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "预约审批请求参数")
public class BookingApprovalDTO {
    
    @NotBlank(message = "审批状态不能为空")
    @Schema(description = "审批状态", example = "approved", allowableValues = {"approved", "rejected"}, required = true)
    private String status;

    @Schema(description = "教师回复内容", example = "同意预约，期待与您的合作")
    private String teacherReply;

    @Schema(description = "管理员备注", example = "特殊情况处理")
    private String adminNotes;
}

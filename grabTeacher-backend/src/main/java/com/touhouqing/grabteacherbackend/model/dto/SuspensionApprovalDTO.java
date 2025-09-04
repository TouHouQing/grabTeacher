package com.touhouqing.grabteacherbackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SuspensionApprovalDTO {
    @NotNull
    @Schema(description = "审批状态", allowableValues = {"approved", "rejected"})
    private String status;

    @NotBlank
    @Schema(description = "审批备注")
    private String reviewNotes;
}



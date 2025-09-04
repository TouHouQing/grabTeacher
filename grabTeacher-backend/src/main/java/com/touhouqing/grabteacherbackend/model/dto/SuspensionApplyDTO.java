package com.touhouqing.grabteacherbackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SuspensionApplyDTO {
    @NotNull
    @Schema(description = "报名ID")
    private Long enrollmentId;

    @Schema(description = "停课原因")
    private String reason;
}



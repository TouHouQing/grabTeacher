package com.touhouqing.grabteacherbackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SuspensionApplyDTO {
    @NotNull
    @Schema(description = "报名ID")
    private Long enrollmentId;

    @NotNull
    @Schema(description = "请假开始日期（至少从一周后开始）")
    private LocalDate startDate;

    @NotNull
    @Schema(description = "请假结束日期（至少覆盖两周，>= 开始日期+13天）")
    private LocalDate endDate;

    @Schema(description = "请假原因")
    private String reason;
}

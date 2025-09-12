package com.touhouqing.grabteacherbackend.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "某日某基础时段的状态")
public class CalendarSlotStatusVO {
    @Schema(description = "日期", example = "2025-09-20")
    private LocalDate date;

    @Schema(description = "基础时段，如 08:00-10:00")
    private String slot;

    @Schema(description = "状态：available|busy_formal|busy_trial_base|partial_trial|pending_trial")
    private String status;

    @Schema(description = "提示信息，可选")
    private String tips;
}


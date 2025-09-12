package com.touhouqing.grabteacherbackend.model.vo;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "\u6708\u5386")
public class MonthlyCalendarVO {
    @Schema(description = "Teacher ID")
    private Long teacherId;

    @Schema(description = "Year", example = "2025")
    private Integer year;

    @Schema(description = "Month", example = "9")
    private Integer month;

    @ArraySchema(arraySchema = @Schema(description = "Daily 6 base-slot statuses"))
    private List<CalendarSlotStatusVO> slots;
}


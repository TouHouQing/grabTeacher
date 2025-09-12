package com.touhouqing.grabteacherbackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "某一天的可上课时间段")
public class DailyTimeSlotDTO {
    @Schema(description = "日期", example = "2025-09-20")
    private LocalDate date;

    @Schema(description = "当日可选2小时基础时间段", example = "[\"08:00-10:00\",\"10:00-12:00\"]")
    @JsonAlias({"slots","time_slots"})
    private List<String> timeSlots;
}


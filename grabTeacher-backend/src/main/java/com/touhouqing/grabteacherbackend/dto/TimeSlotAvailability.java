package com.touhouqing.grabteacherbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "时间段可用性信息")
public class TimeSlotAvailability {

    @Schema(description = "星期几", example = "1", allowableValues = {"0", "1", "2", "3", "4", "5", "6"})
    private Integer weekday; // 0=周日, 1=周一, ..., 6=周六

    @Schema(description = "时间段", example = "14:00-16:00")
    private String timeSlot;

    @Schema(description = "是否可用", example = "true")
    private Boolean available;

    @Schema(description = "可用性百分比", example = "0.8")
    private Double availabilityRate;

    @Schema(description = "冲突日期列表")
    private List<String> conflictDates;

    @Schema(description = "最早可用日期", example = "2024-02-01")
    private LocalDate availableFromDate;

    @Schema(description = "冲突结束日期", example = "2024-01-31")
    private LocalDate conflictEndDate;

    @Schema(description = "冲突原因", example = "已有其他学生预约")
    private String conflictReason;

    @Schema(description = "详细说明", example = "该时间段在未来12周内有3个日期冲突")
    private String description;
}

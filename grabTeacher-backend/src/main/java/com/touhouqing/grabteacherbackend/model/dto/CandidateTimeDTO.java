package com.touhouqing.grabteacherbackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 候选新时间条目（教师可多选）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "教师端调课候选新时间条目")
public class CandidateTimeDTO {

    @Schema(description = "日期", example = "2025-09-20")
    private LocalDate date;

    @Schema(description = "开始时间", example = "15:00")
    private LocalTime startTime;

    @Schema(description = "结束时间", example = "17:00")
    private LocalTime endTime;
}


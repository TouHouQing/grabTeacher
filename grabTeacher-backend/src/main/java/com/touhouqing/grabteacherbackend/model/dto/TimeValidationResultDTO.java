package com.touhouqing.grabteacherbackend.model.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 时间验证结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "时间验证结果")
public class TimeValidationResultDTO {

    @Schema(description = "验证是否通过", example = "true")
    private Boolean valid;

    @Schema(description = "验证消息", example = "时间设置验证通过")
    private String message;

    @Schema(description = "冲突的时间段列表")
    private List<TimeConflictInfo> conflicts;

    @Schema(description = "建议的时间段列表")
    private List<TimeSlotDTO> suggestions;

    @Schema(description = "总体匹配度（0-100）", example = "85")
    private Integer overallMatchScore;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "时间冲突信息")
    public static class TimeConflictInfo {

        @Schema(description = "星期几", example = "1")
        private Integer weekday;

        @Schema(description = "时间段", example = "14:00-16:00")
        private String timeSlot;

        @Schema(description = "冲突日期列表")
        private List<String> conflictDates;

        @Schema(description = "冲突原因", example = "已有其他学生预约")
        private String conflictReason;

        @Schema(description = "冲突严重程度", example = "HIGH", allowableValues = {"LOW", "MEDIUM", "HIGH"})
        private String severity;

        @Schema(description = "建议解决方案", example = "建议调整到15:00-17:00时间段")
        private String suggestion;
    }
}

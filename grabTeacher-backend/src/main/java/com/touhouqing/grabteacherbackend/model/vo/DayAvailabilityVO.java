package com.touhouqing.grabteacherbackend.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "某日可选性（基础2小时段 + 30分钟试听段）")
public class DayAvailabilityVO {
    @Schema(description = "日期(ISO)")
    private String date;

    @Schema(description = "基础2小时段(用于正式课)")
    private List<BaseSlot> baseSlots;

    @Schema(description = "30分钟试听段，仅生成请求的分段范围内")
    private List<TrialSlot> trialSlots;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BaseSlot {
        private String slot; // HH:mm-HH:mm
        private boolean formalAvailable;
        private List<String> reasons; // scheduledTrial, pendingTrial, teacherUnavailable, busy
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrialSlot {
        private String slot; // HH:mm-HH:mm (30min)
        private boolean trialAvailable;
        private List<String> reasons; // busyScheduled, duplicateTrialSlot, teacherUnavailable
    }
}


package com.touhouqing.grabteacherbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * 可上课时间响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "可上课时间响应")
public class AvailableTimeResponseDTO {
    
    @Schema(description = "教师ID", example = "1")
    private Long teacherId;
    
    @Schema(description = "教师姓名", example = "张老师")
    private String teacherName;
    
    @Schema(description = "可上课时间安排列表")
    private List<TimeSlotDTO> availableTimeSlots;
    
    @Schema(description = "总时间段数量", example = "15")
    private Integer totalTimeSlots;
    
    @Schema(description = "是否包含工作日时间", example = "true")
    private Boolean hasWeekdaySlots;
    
    @Schema(description = "是否包含周末时间", example = "true")
    private Boolean hasWeekendSlots;
    
    @Schema(description = "最后更新时间", example = "2025-07-28 10:30:00")
    private String lastUpdated;
    
    /**
     * 计算并设置统计信息
     */
    public void calculateStats() {
        if (availableTimeSlots != null) {
            this.totalTimeSlots = availableTimeSlots.stream()
                .mapToInt(slot -> slot.getTimeSlots() != null ? slot.getTimeSlots().size() : 0)
                .sum();
            
            this.hasWeekdaySlots = availableTimeSlots.stream().anyMatch(TimeSlotDTO::isWeekday);
            this.hasWeekendSlots = availableTimeSlots.stream().anyMatch(TimeSlotDTO::isWeekend);
        } else {
            this.totalTimeSlots = 0;
            this.hasWeekdaySlots = false;
            this.hasWeekendSlots = false;
        }
    }
}

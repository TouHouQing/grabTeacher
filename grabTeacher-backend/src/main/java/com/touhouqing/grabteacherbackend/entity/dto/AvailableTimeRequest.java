package com.touhouqing.grabteacherbackend.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 可上课时间设置请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "可上课时间设置请求")
public class AvailableTimeRequest {
    
    @NotNull(message = "教师ID不能为空")
    @Schema(description = "教师ID", example = "1")
    private Long teacherId;

    @Valid
    @Schema(description = "可上课时间安排列表，如果为空或null则表示所有时间都可以上课")
    private List<TimeSlotDTO> availableTimeSlots;
    
    /**
     * 获取所有可用的时间段数量
     */
    public int getTotalTimeSlots() {
        return availableTimeSlots != null ? 
            availableTimeSlots.stream()
                .mapToInt(slot -> slot.getTimeSlots() != null ? slot.getTimeSlots().size() : 0)
                .sum() : 0;
    }
    
    /**
     * 检查是否包含工作日时间
     */
    public boolean hasWeekdaySlots() {
        return availableTimeSlots != null && 
            availableTimeSlots.stream().anyMatch(TimeSlotDTO::isWeekday);
    }
    
    /**
     * 检查是否包含周末时间
     */
    public boolean hasWeekendSlots() {
        return availableTimeSlots != null && 
            availableTimeSlots.stream().anyMatch(TimeSlotDTO::isWeekend);
    }
}

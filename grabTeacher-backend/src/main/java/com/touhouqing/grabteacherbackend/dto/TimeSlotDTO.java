package com.touhouqing.grabteacherbackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import java.util.List;

/**
 * 时间段DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "时间段信息")
public class TimeSlotDTO {
    
    @NotNull(message = "星期几不能为空")
    @Min(value = 1, message = "星期几必须在1-7之间")
    @Max(value = 7, message = "星期几必须在1-7之间")
    @Schema(description = "星期几", example = "1", allowableValues = {"1", "2", "3", "4", "5", "6", "7"})
    private Integer weekday;
    
    @Schema(description = "时间段列表", example = "[\"08:00-09:00\", \"10:00-11:00\", \"14:00-15:00\"]")
    private List<@Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]-([01]?[0-9]|2[0-3]):[0-5][0-9]$", 
                          message = "时间格式必须为HH:mm-HH:mm") String> timeSlots;
    
    /**
     * 获取星期几的中文名称
     */
    @JsonIgnore
    public String getWeekdayName() {
        String[] weekdays = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        return weekday != null && weekday >= 1 && weekday <= 7 ? weekdays[weekday] : "未知";
    }
    
    /**
     * 检查是否为工作日
     */
    @JsonIgnore
    public boolean isWeekday() {
        return weekday != null && weekday >= 1 && weekday <= 5;
    }

    /**
     * 检查是否为周末
     */
    @JsonIgnore
    public boolean isWeekend() {
        return weekday != null && (weekday == 6 || weekday == 7);
    }
}

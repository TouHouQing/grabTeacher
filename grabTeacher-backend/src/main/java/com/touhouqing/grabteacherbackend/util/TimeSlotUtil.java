package com.touhouqing.grabteacherbackend.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.touhouqing.grabteacherbackend.dto.TimeSlotDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 时间安排工具类
 */
@Slf4j
@Component
public class TimeSlotUtil {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern TIME_SLOT_PATTERN = Pattern.compile("^([01]?[0-9]|2[0-3]):[0-5][0-9]-([01]?[0-9]|2[0-3]):[0-5][0-9]$");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    /**
     * 将TimeSlotDTO列表转换为JSON字符串
     */
    public static String toJsonString(List<TimeSlotDTO> timeSlots) {
        if (timeSlots == null || timeSlots.isEmpty()) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(timeSlots);
        } catch (JsonProcessingException e) {
            log.error("转换时间安排为JSON失败", e);
            return null;
        }
    }
    
    /**
     * 将JSON字符串转换为TimeSlotDTO列表
     */
    public static List<TimeSlotDTO> fromJsonString(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            return objectMapper.readValue(jsonString, new TypeReference<List<TimeSlotDTO>>() {});
        } catch (JsonProcessingException e) {
            log.error("解析时间安排JSON失败: {}", jsonString, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 验证时间段格式是否正确
     */
    public static boolean isValidTimeSlot(String timeSlot) {
        if (timeSlot == null || !TIME_SLOT_PATTERN.matcher(timeSlot).matches()) {
            return false;
        }
        
        String[] times = timeSlot.split("-");
        if (times.length != 2) {
            return false;
        }
        
        try {
            LocalTime startTime = LocalTime.parse(times[0], TIME_FORMATTER);
            LocalTime endTime = LocalTime.parse(times[1], TIME_FORMATTER);
            return startTime.isBefore(endTime);
        } catch (DateTimeParseException e) {
            return false;
        }
    }
    
    /**
     * 验证TimeSlotDTO列表是否有效
     * 注意：空列表或null被认为是有效的（表示清空时间设置或所有时间都可用）
     */
    public static boolean isValidTimeSlots(List<TimeSlotDTO> timeSlots) {
        if (timeSlots == null || timeSlots.isEmpty()) {
            return true; // 空列表或null是有效的，表示清空时间设置
        }

        for (TimeSlotDTO timeSlot : timeSlots) {
            if (timeSlot.getWeekday() == null ||
                timeSlot.getWeekday() < 1 ||
                timeSlot.getWeekday() > 7) {
                return false;
            }

            if (timeSlot.getTimeSlots() != null) {
                for (String slot : timeSlot.getTimeSlots()) {
                    if (!isValidTimeSlot(slot)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }
    
    /**
     * 获取默认的可上课时间安排
     */
    public static List<TimeSlotDTO> getDefaultTimeSlots() {
        List<TimeSlotDTO> defaultSlots = new ArrayList<>();

        // 工作日晚上时间 (1=周一, 2=周二, ..., 5=周五)
        for (int weekday = 1; weekday <= 5; weekday++) {
            defaultSlots.add(TimeSlotDTO.builder()
                .weekday(weekday)
                .timeSlots(List.of("18:00-19:00", "19:00-20:00", "20:00-21:00"))
                .build());
        }

        // 周末时间 (6=周六, 7=周日)
        for (int weekday = 6; weekday <= 7; weekday++) {
            defaultSlots.add(TimeSlotDTO.builder()
                .weekday(weekday)
                .timeSlots(List.of("09:00-10:00", "10:00-11:00", "14:00-15:00", "15:00-16:00", "16:00-17:00"))
                .build());
        }

        return defaultSlots;
    }

    /**
     * 计算两个时间段的重叠程度（0-100分）
     */
    public static int calculateTimeOverlapScore(String timeSlot1, String timeSlot2) {
        if (!isValidTimeSlot(timeSlot1) || !isValidTimeSlot(timeSlot2)) {
            return 0;
        }

        String[] times1 = timeSlot1.split("-");
        String[] times2 = timeSlot2.split("-");

        try {
            LocalTime start1 = LocalTime.parse(times1[0], TIME_FORMATTER);
            LocalTime end1 = LocalTime.parse(times1[1], TIME_FORMATTER);
            LocalTime start2 = LocalTime.parse(times2[0], TIME_FORMATTER);
            LocalTime end2 = LocalTime.parse(times2[1], TIME_FORMATTER);

            // 计算重叠时间
            LocalTime overlapStart = start1.isAfter(start2) ? start1 : start2;
            LocalTime overlapEnd = end1.isBefore(end2) ? end1 : end2;

            if (overlapStart.isBefore(overlapEnd)) {
                // 有重叠
                long overlapMinutes = java.time.Duration.between(overlapStart, overlapEnd).toMinutes();
                long totalMinutes1 = java.time.Duration.between(start1, end1).toMinutes();
                long totalMinutes2 = java.time.Duration.between(start2, end2).toMinutes();

                // 计算重叠度：重叠时间 / 较短时间段的长度
                long shorterDuration = Math.min(totalMinutes1, totalMinutes2);
                double overlapRatio = (double) overlapMinutes / shorterDuration;

                return (int) Math.round(overlapRatio * 100);
            } else {
                return 0; // 无重叠
            }
        } catch (DateTimeParseException e) {
            return 0;
        }
    }

    /**
     * 将前端星期几格式转换为后端格式
     * 前端：0=周日, 1=周一, ..., 6=周六
     * 后端：1=周一, 2=周二, ..., 7=周日
     */
    public static int convertFrontendWeekdayToBackend(int frontendWeekday) {
        if (frontendWeekday == 0) {
            return 7; // 周日
        } else {
            return frontendWeekday; // 周一到周六
        }
    }

    /**
     * 将后端星期几格式转换为前端格式
     * 后端：1=周一, 2=周二, ..., 7=周日
     * 前端：0=周日, 1=周一, ..., 6=周六
     */
    public static int convertBackendWeekdayToFrontend(int backendWeekday) {
        if (backendWeekday == 7) {
            return 0; // 周日
        } else {
            return backendWeekday; // 周一到周六
        }
    }
    
    /**
     * 检查两个时间段是否有冲突
     */
    public static boolean hasTimeConflict(String timeSlot1, String timeSlot2) {
        if (!isValidTimeSlot(timeSlot1) || !isValidTimeSlot(timeSlot2)) {
            return false;
        }
        
        String[] times1 = timeSlot1.split("-");
        String[] times2 = timeSlot2.split("-");
        
        try {
            LocalTime start1 = LocalTime.parse(times1[0], TIME_FORMATTER);
            LocalTime end1 = LocalTime.parse(times1[1], TIME_FORMATTER);
            LocalTime start2 = LocalTime.parse(times2[0], TIME_FORMATTER);
            LocalTime end2 = LocalTime.parse(times2[1], TIME_FORMATTER);
            
            // 检查是否有重叠
            return start1.isBefore(end2) && start2.isBefore(end1);
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}

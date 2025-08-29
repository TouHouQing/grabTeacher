package com.touhouqing.grabteacherbackend.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.touhouqing.grabteacherbackend.model.dto.TimeSlotDTO;
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
    
    private static final ObjectMapper objectMapper = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        return mapper;
    }
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
            // 手动解析JSON来避免Jackson反序列化问题
            // 这是为了解决weekday字段在自动反序列化时丢失的问题
            JsonNode rootNode = objectMapper.readTree(jsonString);
            List<TimeSlotDTO> result = new ArrayList<>();

            for (JsonNode node : rootNode) {
                Integer weekday = node.has("weekday") ? node.get("weekday").asInt() : null;
                List<String> timeSlots = new ArrayList<>();

                if (node.has("timeSlots")) {
                    JsonNode timeSlotsNode = node.get("timeSlots");
                    for (JsonNode timeSlotNode : timeSlotsNode) {
                        timeSlots.add(timeSlotNode.asText());
                    }
                }

                TimeSlotDTO dto = TimeSlotDTO.builder()
                    .weekday(weekday)
                    .timeSlots(timeSlots)
                    .build();

                result.add(dto);
            }

            return result;
        } catch (JsonProcessingException e) {
            log.error("解析时间安排JSON失败: {}", jsonString, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 验证时间段格式是否正确
     */
    public static boolean isValidTimeSlot(String timeSlot) {
        if (timeSlot == null || timeSlot.trim().isEmpty()) {
            return false;
        }

        timeSlot = timeSlot.trim();

        // 检查基本格式
        if (!TIME_SLOT_PATTERN.matcher(timeSlot).matches()) {
            log.debug("时间段格式不匹配正则表达式: {}", timeSlot);
            return false;
        }

        String[] times = timeSlot.split("-");
        if (times.length != 2) {
            log.debug("时间段分割后长度不为2: {}", timeSlot);
            return false;
        }

        try {
            LocalTime startTime = LocalTime.parse(times[0].trim(), TIME_FORMATTER);
            LocalTime endTime = LocalTime.parse(times[1].trim(), TIME_FORMATTER);
            boolean isValid = startTime.isBefore(endTime);
            if (!isValid) {
                log.debug("开始时间不早于结束时间: {} -> {}", startTime, endTime);
            }
            return isValid;
        } catch (DateTimeParseException e) {
            log.debug("时间解析失败: {}, error: {}", timeSlot, e.getMessage());
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

        try {
            for (TimeSlotDTO timeSlot : timeSlots) {
                // 检查weekday是否有效
                if (timeSlot.getWeekday() == null ||
                    timeSlot.getWeekday() < 1 ||
                    timeSlot.getWeekday() > 7) {
                    log.warn("无效的星期几: {}", timeSlot.getWeekday());
                    return false;
                }

                // 检查时间段是否有效
                if (timeSlot.getTimeSlots() != null) {
                    for (String slot : timeSlot.getTimeSlots()) {
                        if (slot != null && !slot.trim().isEmpty() && !isValidTimeSlot(slot)) {
                            log.warn("无效的时间段格式: {}", slot);
                            return false;
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            log.error("验证时间段时发生异常", e);
            return false;
        }
    }

    /**
     * 尝试修复和验证时间段数据
     * 如果数据有问题，尝试修复或返回默认值
     */
    public static List<TimeSlotDTO> sanitizeTimeSlots(List<TimeSlotDTO> timeSlots) {
        if (timeSlots == null || timeSlots.isEmpty()) {
            return getDefaultTimeSlots();
        }

        List<TimeSlotDTO> sanitized = new ArrayList<>();

        for (TimeSlotDTO timeSlot : timeSlots) {
            if (timeSlot.getWeekday() != null &&
                timeSlot.getWeekday() >= 1 &&
                timeSlot.getWeekday() <= 7) {

                List<String> validSlots = new ArrayList<>();
                if (timeSlot.getTimeSlots() != null) {
                    for (String slot : timeSlot.getTimeSlots()) {
                        if (slot != null && !slot.trim().isEmpty() && isValidTimeSlot(slot)) {
                            validSlots.add(slot.trim());
                        }
                    }
                }

                // 如果这个星期几有有效的时间段，就添加
                if (!validSlots.isEmpty()) {
                    sanitized.add(TimeSlotDTO.builder()
                        .weekday(timeSlot.getWeekday())
                        .timeSlots(validSlots)
                        .build());
                }
            }
        }

        // 如果没有任何有效的时间段，返回默认值
        return sanitized.isEmpty() ? getDefaultTimeSlots() : sanitized;
    }

    /**
     * 获取默认的可上课时间安排 - 固定为6个系统上课时间
     */
    public static List<TimeSlotDTO> getDefaultTimeSlots() {
        List<TimeSlotDTO> defaultSlots = new ArrayList<>();

        // 工作日时间 (1=周一, 2=周二, ..., 5=周五)
        for (int weekday = 1; weekday <= 5; weekday++) {
            defaultSlots.add(TimeSlotDTO.builder()
                .weekday(weekday)
                .timeSlots(List.of("17:00-19:00", "19:00-21:00"))
                .build());
        }

        // 周末时间 (6=周六, 7=周日)
        for (int weekday = 6; weekday <= 7; weekday++) {
            defaultSlots.add(TimeSlotDTO.builder()
                .weekday(weekday)
                .timeSlots(List.of("08:00-10:00", "10:00-12:00", "13:00-15:00", "15:00-17:00"))
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

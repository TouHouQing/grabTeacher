package com.touhouqing.grabteacherbackend.service.impl;

import com.touhouqing.grabteacherbackend.dto.TimeSlotDTO;
import com.touhouqing.grabteacherbackend.dto.TimeValidationResult;
import com.touhouqing.grabteacherbackend.entity.Teacher;
import com.touhouqing.grabteacherbackend.mapper.ScheduleMapper;
import com.touhouqing.grabteacherbackend.mapper.TeacherMapper;
import com.touhouqing.grabteacherbackend.service.TimeValidationService;
import com.touhouqing.grabteacherbackend.util.TimeSlotUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 时间验证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimeValidationServiceImpl implements TimeValidationService {

    private final TeacherMapper teacherMapper;
    private final ScheduleMapper scheduleMapper;

    @Override
    public TimeValidationResult validateTeacherAvailableTime(Long teacherId, 
                                                           List<TimeSlotDTO> availableTimeSlots,
                                                           LocalDate startDate, 
                                                           LocalDate endDate) {
        log.info("验证教师可用时间设置，教师ID: {}", teacherId);

        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            return TimeValidationResult.builder()
                    .valid(false)
                    .message("教师不存在")
                    .conflicts(new ArrayList<>())
                    .suggestions(new ArrayList<>())
                    .overallMatchScore(0)
                    .build();
        }

        List<TimeValidationResult.TimeConflictInfo> conflicts = new ArrayList<>();
        int totalSlots = 0;
        int conflictSlots = 0;

        // 检查每个时间段是否与现有课程冲突
        for (TimeSlotDTO timeSlot : availableTimeSlots) {
            if (timeSlot.getTimeSlots() != null) {
                for (String time : timeSlot.getTimeSlots()) {
                    totalSlots++;
                    
                    List<String> conflictDates = checkTimeSlotConflicts(
                        teacherId, timeSlot.getWeekday(), time, startDate, endDate);
                    
                    if (!conflictDates.isEmpty()) {
                        conflictSlots++;
                        
                        TimeValidationResult.TimeConflictInfo conflict = 
                            TimeValidationResult.TimeConflictInfo.builder()
                                .weekday(timeSlot.getWeekday())
                                .timeSlot(time)
                                .conflictDates(conflictDates)
                                .conflictReason("与现有课程时间冲突")
                                .severity(getSeverity(conflictDates.size()))
                                .suggestion(generateSuggestion(time))
                                .build();
                        
                        conflicts.add(conflict);
                    }
                }
            }
        }

        // 计算总体匹配度
        int overallMatchScore = totalSlots > 0 ? 
            (int) Math.round(((double) (totalSlots - conflictSlots) / totalSlots) * 100) : 100;

        // 生成建议
        List<TimeSlotDTO> suggestions = generateSuggestions(teacherId, availableTimeSlots, startDate, endDate);

        boolean valid = conflicts.isEmpty() || overallMatchScore >= 70;
        String message = valid ? 
            "时间设置验证通过" : 
            String.format("发现 %d 个时间冲突，建议调整相关时间段", conflicts.size());

        return TimeValidationResult.builder()
                .valid(valid)
                .message(message)
                .conflicts(conflicts)
                .suggestions(suggestions)
                .overallMatchScore(overallMatchScore)
                .build();
    }

    @Override
    public List<TimeSlotDTO> getRecommendedTimeSlots(Long teacherId, LocalDate startDate, LocalDate endDate) {
        log.info("获取教师时间设置建议，教师ID: {}", teacherId);

        List<TimeSlotDTO> recommendations = new ArrayList<>();
        
        // 基于历史数据和常见时间段生成建议（完整的9:00-21:00时间段）
        List<String> commonTimeSlots = List.of(
            "09:00-10:00", "10:00-11:00", "11:00-12:00", "12:00-13:00",
            "13:00-14:00", "14:00-15:00", "15:00-16:00", "16:00-17:00",
            "17:00-18:00", "18:00-19:00", "19:00-20:00", "20:00-21:00"
        );

        // 为每个星期几生成建议
        for (int weekday = 1; weekday <= 7; weekday++) {
            List<String> availableSlots = new ArrayList<>();
            
            for (String timeSlot : commonTimeSlots) {
                List<String> conflicts = checkTimeSlotConflicts(teacherId, weekday, timeSlot, startDate, endDate);
                if (conflicts.size() <= 2) { // 冲突较少的时间段
                    availableSlots.add(timeSlot);
                }
            }
            
            if (!availableSlots.isEmpty()) {
                recommendations.add(TimeSlotDTO.builder()
                    .weekday(weekday)
                    .timeSlots(availableSlots)
                    .build());
            }
        }

        return recommendations;
    }

    @Override
    public TimeValidationResult validateStudentBookingTime(Long teacherId,
                                                          List<Integer> studentPreferredWeekdays,
                                                          List<String> studentPreferredTimeSlots) {
        log.info("验证学生预约时间，教师ID: {}, 学生偏好: 星期{}, 时间段{}", 
                teacherId, studentPreferredWeekdays, studentPreferredTimeSlots);

        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            return TimeValidationResult.builder()
                    .valid(false)
                    .message("教师不存在")
                    .build();
        }

        // 获取教师可用时间
        List<TimeSlotDTO> teacherAvailableSlots = TimeSlotUtil.fromJsonString(teacher.getAvailableTimeSlots());
        
        List<TimeValidationResult.TimeConflictInfo> conflicts = new ArrayList<>();
        int matchCount = 0;
        int totalChecks = 0;

        // 检查学生偏好时间是否与教师可用时间匹配
        for (Integer weekday : studentPreferredWeekdays) {
            for (String timeSlot : studentPreferredTimeSlots) {
                totalChecks++;
                
                boolean matched = teacherAvailableSlots.stream()
                    .anyMatch(slot -> weekday.equals(slot.getWeekday()) && 
                             slot.getTimeSlots() != null && 
                             slot.getTimeSlots().contains(timeSlot));
                
                if (matched) {
                    matchCount++;
                } else {
                    TimeValidationResult.TimeConflictInfo conflict = 
                        TimeValidationResult.TimeConflictInfo.builder()
                            .weekday(weekday)
                            .timeSlot(timeSlot)
                            .conflictReason("教师该时间段不可用")
                            .severity("MEDIUM")
                            .suggestion("建议选择其他时间段或联系教师协商")
                            .build();
                    conflicts.add(conflict);
                }
            }
        }

        int matchScore = totalChecks > 0 ? (matchCount * 100) / totalChecks : 0;
        boolean valid = matchScore >= 50; // 50%以上匹配认为可接受

        return TimeValidationResult.builder()
                .valid(valid)
                .message(valid ? "时间匹配良好" : "时间匹配度较低，建议调整")
                .conflicts(conflicts)
                .overallMatchScore(matchScore)
                .build();
    }

    @Override
    public TimeValidationResult validateRecurringBookingTime(Long teacherId,
                                                           List<Integer> studentPreferredWeekdays,
                                                           List<String> studentPreferredTimeSlots,
                                                           LocalDate startDate,
                                                           LocalDate endDate,
                                                           Integer totalTimes) {
        log.info("验证周期性预约时间，教师ID: {}, 学生偏好: 星期{}, 时间段{}, 日期范围: {} - {}, 总次数: {}",
                teacherId, studentPreferredWeekdays, studentPreferredTimeSlots, startDate, endDate, totalTimes);

        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            return TimeValidationResult.builder()
                    .valid(false)
                    .message("教师不存在")
                    .conflicts(new ArrayList<>())
                    .overallMatchScore(0)
                    .build();
        }

        // 获取教师可用时间设置
        List<TimeSlotDTO> teacherAvailableSlots = TimeSlotUtil.fromJsonString(teacher.getAvailableTimeSlots());

        List<TimeValidationResult.TimeConflictInfo> conflicts = new ArrayList<>();
        int totalPossibleSlots = 0;
        int availableSlots = 0;

        // 遍历日期范围内的每一天
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            int weekday = currentDate.getDayOfWeek().getValue(); // 1=周一, 7=周日

            // 检查这一天是否在学生选择的星期几中
            if (studentPreferredWeekdays.contains(weekday)) {
                // 检查每个时间段
                for (String timeSlot : studentPreferredTimeSlots) {
                    totalPossibleSlots++;

                    // 1. 检查教师是否在这个星期几的这个时间段可用
                    boolean teacherAvailable = teacherAvailableSlots.stream()
                            .anyMatch(slot -> weekday == slot.getWeekday() &&
                                     slot.getTimeSlots() != null &&
                                     slot.getTimeSlots().contains(timeSlot));

                    if (!teacherAvailable) {
                        conflicts.add(TimeValidationResult.TimeConflictInfo.builder()
                                .weekday(weekday)
                                .timeSlot(timeSlot)
                                .conflictDates(List.of(currentDate.toString()))
                                .conflictReason("教师该时间段不可用")
                                .severity("HIGH")
                                .suggestion("选择教师可用的时间段")
                                .build());
                        continue;
                    }

                    // 2. 检查具体日期是否有课程冲突
                    String[] times = timeSlot.split("-");
                    if (times.length == 2) {
                        try {
                            LocalTime startTime = LocalTime.parse(times[0]);
                            LocalTime endTime = LocalTime.parse(times[1]);

                            int conflictCount = scheduleMapper.countConflictingSchedules(
                                    teacherId, currentDate, startTime, endTime);

                            if (conflictCount > 0) {
                                conflicts.add(TimeValidationResult.TimeConflictInfo.builder()
                                        .weekday(weekday)
                                        .timeSlot(timeSlot)
                                        .conflictDates(List.of(currentDate.toString()))
                                        .conflictReason("该时间段已有其他课程安排")
                                        .severity("HIGH")
                                        .suggestion("选择其他时间或日期")
                                        .build());
                            } else {
                                availableSlots++;
                            }
                        } catch (Exception e) {
                            log.error("解析时间段失败: {}", timeSlot, e);
                        }
                    }
                }
            }
            currentDate = currentDate.plusDays(1);
        }

        // 计算匹配度
        int overallMatchScore = totalPossibleSlots > 0 ?
                (availableSlots * 100) / totalPossibleSlots : 0;

        boolean valid = overallMatchScore >= 70; // 70%以上认为可接受

        String message;
        if (overallMatchScore >= 90) {
            message = String.format("时间匹配度很高 (%d%%)，大部分时间段都可预约", overallMatchScore);
        } else if (overallMatchScore >= 70) {
            message = String.format("时间匹配度良好 (%d%%)，部分时间段可预约", overallMatchScore);
        } else if (overallMatchScore > 0) {
            message = String.format("时间匹配度较低 (%d%%)，建议调整时间选择", overallMatchScore);
        } else {
            message = "所选时间段教师不可预约，请重新选择";
        }

        return TimeValidationResult.builder()
                .valid(valid)
                .message(message)
                .conflicts(conflicts)
                .overallMatchScore(overallMatchScore)
                .build();
    }

    /**
     * 检查特定时间段的冲突
     */
    private List<String> checkTimeSlotConflicts(Long teacherId, int weekday, String timeSlot, 
                                               LocalDate startDate, LocalDate endDate) {
        List<String> conflictDates = new ArrayList<>();
        
        String[] times = timeSlot.split("-");
        if (times.length != 2) {
            return conflictDates;
        }
        
        try {
            LocalTime startTime = LocalTime.parse(times[0]);
            LocalTime endTime = LocalTime.parse(times[1]);
            
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                int currentWeekday = currentDate.getDayOfWeek().getValue();
                if (currentWeekday == 7) currentWeekday = 7; // 周日
                
                if (currentWeekday == weekday) {
                    int conflictCount = scheduleMapper.countConflictingSchedules(
                        teacherId, currentDate, startTime, endTime);
                    if (conflictCount > 0) {
                        conflictDates.add(currentDate.toString());
                    }
                }
                currentDate = currentDate.plusDays(1);
            }
        } catch (Exception e) {
            log.error("检查时间冲突时发生错误", e);
        }
        
        return conflictDates;
    }

    /**
     * 获取冲突严重程度
     */
    private String getSeverity(int conflictCount) {
        if (conflictCount >= 5) return "HIGH";
        if (conflictCount >= 2) return "MEDIUM";
        return "LOW";
    }

    /**
     * 生成时间调整建议
     */
    private String generateSuggestion(String originalTime) {
        String[] times = originalTime.split("-");
        if (times.length == 2) {
            try {
                LocalTime start = LocalTime.parse(times[0]);
                LocalTime end = LocalTime.parse(times[1]);
                
                // 建议前后调整1小时
                LocalTime newStart = start.plusHours(1);
                LocalTime newEnd = end.plusHours(1);
                
                return String.format("建议调整到 %s-%s", newStart, newEnd);
            } catch (Exception e) {
                return "建议选择其他时间段";
            }
        }
        return "建议选择其他时间段";
    }

    /**
     * 生成时间设置建议
     */
    private List<TimeSlotDTO> generateSuggestions(Long teacherId, List<TimeSlotDTO> originalSlots,
                                                 LocalDate startDate, LocalDate endDate) {
        // 基于冲突情况生成优化建议
        List<TimeSlotDTO> suggestions = new ArrayList<>();
        
        for (TimeSlotDTO slot : originalSlots) {
            List<String> optimizedTimeSlots = new ArrayList<>();
            
            if (slot.getTimeSlots() != null) {
                for (String timeSlot : slot.getTimeSlots()) {
                    List<String> conflicts = checkTimeSlotConflicts(
                        teacherId, slot.getWeekday(), timeSlot, startDate, endDate);
                    
                    if (conflicts.isEmpty()) {
                        optimizedTimeSlots.add(timeSlot);
                    } else {
                        // 尝试生成替代时间段
                        String alternativeTime = generateAlternativeTime(timeSlot);
                        if (alternativeTime != null) {
                            List<String> altConflicts = checkTimeSlotConflicts(
                                teacherId, slot.getWeekday(), alternativeTime, startDate, endDate);
                            if (altConflicts.size() < conflicts.size()) {
                                optimizedTimeSlots.add(alternativeTime);
                            }
                        }
                    }
                }
            }
            
            if (!optimizedTimeSlots.isEmpty()) {
                suggestions.add(TimeSlotDTO.builder()
                    .weekday(slot.getWeekday())
                    .timeSlots(optimizedTimeSlots)
                    .build());
            }
        }
        
        return suggestions;
    }

    /**
     * 生成替代时间段
     */
    private String generateAlternativeTime(String originalTime) {
        String[] times = originalTime.split("-");
        if (times.length == 2) {
            try {
                LocalTime start = LocalTime.parse(times[0]);
                LocalTime end = LocalTime.parse(times[1]);
                
                // 尝试前后各1小时的时间段
                LocalTime newStart = start.minusHours(1);
                LocalTime newEnd = end.minusHours(1);
                
                if (newStart.isAfter(LocalTime.of(8, 0))) {
                    return String.format("%s-%s", newStart, newEnd);
                }
                
                newStart = start.plusHours(1);
                newEnd = end.plusHours(1);
                
                if (newEnd.isBefore(LocalTime.of(22, 0))) {
                    return String.format("%s-%s", newStart, newEnd);
                }
            } catch (Exception e) {
                log.error("生成替代时间段失败", e);
            }
        }
        return null;
    }
}

package com.touhouqing.grabteacherbackend.service.impl;

import com.touhouqing.grabteacherbackend.model.dto.TimeSlotDTO;
import com.touhouqing.grabteacherbackend.model.dto.TimeValidationResultDTO;
import com.touhouqing.grabteacherbackend.model.entity.Teacher;
import com.touhouqing.grabteacherbackend.mapper.ScheduleMapper;
import com.touhouqing.grabteacherbackend.mapper.TeacherMapper;
import com.touhouqing.grabteacherbackend.model.entity.Schedule;
import com.touhouqing.grabteacherbackend.service.TimeValidationService;
import com.touhouqing.grabteacherbackend.service.TeacherScheduleCacheService;
import com.touhouqing.grabteacherbackend.util.TimeSlotUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * 时间验证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimeValidationServiceImpl implements TimeValidationService {

    private final TeacherMapper teacherMapper;
    private final ScheduleMapper scheduleMapper;
    private final TeacherScheduleCacheService teacherScheduleCacheService;

    @Override
    public TimeValidationResultDTO validateTeacherAvailableTime(Long teacherId,
                                                                List<TimeSlotDTO> availableTimeSlots,
                                                                LocalDate startDate,
                                                                LocalDate endDate) {
        log.info("验证教师可用时间设置，教师ID: {}", teacherId);

        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            return TimeValidationResultDTO.builder()
                    .valid(false)
                    .message("教师不存在")
                    .conflicts(new ArrayList<>())
                    .suggestions(new ArrayList<>())
                    .overallMatchScore(0)
                    .build();
        }

        List<TimeValidationResultDTO.TimeConflictInfo> conflicts = new ArrayList<>();
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
                        
                        TimeValidationResultDTO.TimeConflictInfo conflict =
                            TimeValidationResultDTO.TimeConflictInfo.builder()
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

        return TimeValidationResultDTO.builder()
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
        
        // 基于历史数据和常见时间段生成建议（固定为6个系统上课时间）
        List<String> commonTimeSlots = List.of(
            "08:00-10:00", "10:00-12:00", "13:00-15:00",
            "15:00-17:00", "17:00-19:00", "19:00-21:00"
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
    public TimeValidationResultDTO validateStudentBookingTime(Long teacherId,
                                                              List<Integer> studentPreferredWeekdays,
                                                              List<String> studentPreferredTimeSlots) {
        log.info("验证学生预约时间，教师ID: {}, 学生偏好: 星期{}, 时间段{}", 
                teacherId, studentPreferredWeekdays, studentPreferredTimeSlots);

        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            return TimeValidationResultDTO.builder()
                    .valid(false)
                    .message("教师不存在")
                    .build();
        }

        // 获取教师可用时间
        List<TimeSlotDTO> teacherAvailableSlots = TimeSlotUtil.fromJsonString(teacher.getAvailableTimeSlots());
        
        List<TimeValidationResultDTO.TimeConflictInfo> conflicts = new ArrayList<>();
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
                    TimeValidationResultDTO.TimeConflictInfo conflict =
                        TimeValidationResultDTO.TimeConflictInfo.builder()
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

        return TimeValidationResultDTO.builder()
                .valid(valid)
                .message(valid ? "时间匹配良好" : "时间匹配度较低，建议调整")
                .conflicts(conflicts)
                .overallMatchScore(matchScore)
                .build();
    }

    @Override
    public TimeValidationResultDTO validateRecurringBookingTime(Long teacherId,
                                                                List<Integer> studentPreferredWeekdays,
                                                                List<String> studentPreferredTimeSlots,
                                                                LocalDate startDate,
                                                                LocalDate endDate,
                                                                Integer totalTimes) {
        log.info("验证周期性预约时间，教师ID: {}, 学生偏好: 星期{}, 时间段{}, 日期范围: {} - {}, 总次数: {}",
                teacherId, studentPreferredWeekdays, studentPreferredTimeSlots, startDate, endDate, totalTimes);

        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            return TimeValidationResultDTO.builder()
                    .valid(false)
                    .message("教师不存在")
                    .conflicts(new ArrayList<>())
                    .overallMatchScore(0)
                    .build();
        }

        // 获取教师可用时间设置
        List<TimeSlotDTO> teacherAvailableSlots = TimeSlotUtil.fromJsonString(teacher.getAvailableTimeSlots());

        List<TimeValidationResultDTO.TimeConflictInfo> conflicts = new ArrayList<>();
        int totalPossibleSlots = 0;
        int availableSlots = 0;

        // 一次性预载范围内所有天的忙时，避免循环中多次 DB 查询
        Map<LocalDate, List<String>> busyMap = buildBusyMapAndBackfill(teacherId, startDate, endDate);

        // 将教师可用时段构建为 Map<weekday, Set<String>>，O(1) 判断
        Map<Integer, Set<String>> availMap = new HashMap<>();
        for (TimeSlotDTO s : teacherAvailableSlots) {
            availMap.computeIfAbsent(s.getWeekday(), k -> new HashSet<>() )
                    .addAll(s.getTimeSlots() == null ? Collections.emptyList() : s.getTimeSlots());
        }

        // 预解析学生偏好时间段为分钟整数，避免多次 LocalTime.parse
        Map<String, int[]> prefSlotMinutes = new HashMap<>();
        for (String ts : studentPreferredTimeSlots) {
            int[] mm = parseSlotToMinutes(ts);
            if (mm != null) prefSlotMinutes.put(ts, mm);
        }

        // 将 busyMap 转为按天的分钟区间列表，并排序，便于快速判断
        Map<LocalDate, List<int[]>> busyMinutesMap = new HashMap<>();
        for (Map.Entry<LocalDate, List<String>> e : busyMap.entrySet()) {
            List<int[]> list = new ArrayList<>();
            for (String s : e.getValue()) {
                int[] mm = parseSlotToMinutes(s);
                if (mm != null) list.add(mm);
            }
            list.sort(Comparator.comparingInt(a -> a[0]));
            busyMinutesMap.put(e.getKey(), list);
        }

        // 遍历日期范围内的每一天
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            int weekday = currentDate.getDayOfWeek().getValue(); // 1=周一, 7=周日

            if (studentPreferredWeekdays.contains(weekday)) {
                List<int[]> dayBusy = busyMinutesMap.getOrDefault(currentDate, Collections.emptyList());
                for (String timeSlot : studentPreferredTimeSlots) {
                    totalPossibleSlots++;

                    // 1. 教师该星期几是否可用
                    Set<String> set = availMap.get(weekday);
                    boolean isAvailable = false;
                    
                    if (set != null) {
                        // 直接匹配时间段
                        if (set.contains(timeSlot)) {
                            isAvailable = true;
                        } else {
                            // 如果直接匹配失败，检查是否是1.5小时时间段需要映射到2小时时间段
                            // 这里需要检查时间段是否在教师可用的2小时时间段内
                            for (String availableSlot : set) {
                                if (isTimeSlotContained(timeSlot, availableSlot)) {
                                    isAvailable = true;
                                    break;
                                }
                            }
                        }
                    }
                    
                    if (!isAvailable) {
                        conflicts.add(TimeValidationResultDTO.TimeConflictInfo.builder()
                                .weekday(weekday)
                                .timeSlot(timeSlot)
                                .conflictDates(List.of(currentDate.toString()))
                                .conflictReason("教师该时间段不可用")
                                .severity("HIGH")
                                .suggestion("选择教师可用的时间段")
                                .build());
                        continue;
                    }

                    // 2. 用分钟区间快速判断冲突
                    int[] mm = prefSlotMinutes.get(timeSlot);
                    boolean conflict = hasOverlap(dayBusy, mm);
                    if (conflict) {
                        conflicts.add(TimeValidationResultDTO.TimeConflictInfo.builder()
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

        return TimeValidationResultDTO.builder()
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
        Map<LocalDate, List<String>> busyMap = buildBusyMapAndBackfill(teacherId, startDate, endDate);
        int[] mm = parseSlotToMinutes(timeSlot);
        List<String> conflictDates = new ArrayList<>();
        LocalDate d = startDate;
        while (!d.isAfter(endDate)) {
            int w = d.getDayOfWeek().getValue();
            if (w == weekday) {
                List<int[]> dayBusy = new ArrayList<>();
                for (String b : busyMap.getOrDefault(d, Collections.emptyList())) {
                    int[] bb = parseSlotToMinutes(b);
                    if (bb != null) dayBusy.add(bb);
                }
                dayBusy.sort(Comparator.comparingInt(a -> a[0]));
                if (hasOverlap(dayBusy, mm)) conflictDates.add(d.toString());
            }
            d = d.plusDays(1);
        }
        return conflictDates;
    }

    // 一次性构建范围 busyMap 并批量回填 Redis
    private Map<LocalDate, List<String>> buildBusyMapAndBackfill(Long teacherId, LocalDate startDate, LocalDate endDate) {
        List<Schedule> range =
                scheduleMapper.findByTeacherIdAndDateRange(teacherId, startDate, endDate);
        Map<LocalDate, List<String>> busyMap = new HashMap<>();
        for (Schedule s : range) {
            LocalDate d = s.getScheduledDate();
            String slot = s.getStartTime().toString() + "-" + s.getEndTime().toString();
            busyMap.computeIfAbsent(d, k -> new ArrayList<>()).add(slot);
        }
        LocalDate d = startDate;
        while (!d.isAfter(endDate)) { busyMap.putIfAbsent(d, new ArrayList<>()); d = d.plusDays(1); }
        try { teacherScheduleCacheService.putBusySlotsBatch(teacherId, busyMap); } catch (Exception ignore) {}
        return busyMap;
    }

    // 快速区间重叠判断：dayBusy 已按 start 升序
    private boolean hasOverlap(List<int[]> dayBusy, int[] mm) {
        if (mm == null) return false;
        int s = mm[0], e = mm[1];
        for (int[] b : dayBusy) {
            if (b[0] >= e) break; // 后续都在右侧
            if (b[1] > s && b[0] < e) return true;
        }
        return false;
    }

    /**
     * 检查请求的时间段是否在教师可预约时间段内
     * @param requestedTimeSlot 请求的时间段，格式：HH:mm-HH:mm
     * @param availableTimeSlot 教师可预约时间段，格式：HH:mm-HH:mm
     * @return 如果请求时间段完全在可预约时间段内，返回true
     */
    private boolean isTimeSlotContained(String requestedTimeSlot, String availableTimeSlot) {
        if (requestedTimeSlot == null || availableTimeSlot == null) {
            return false;
        }

        try {
            String[] requestedTimes = requestedTimeSlot.split("-");
            String[] availableTimes = availableTimeSlot.split("-");

            if (requestedTimes.length != 2 || availableTimes.length != 2) {
                return false;
            }

            LocalTime requestedStart = LocalTime.parse(requestedTimes[0]);
            LocalTime requestedEnd = LocalTime.parse(requestedTimes[1]);
            LocalTime availableStart = LocalTime.parse(availableTimes[0]);
            LocalTime availableEnd = LocalTime.parse(availableTimes[1]);

            // 检查请求的时间段是否完全在可预约时间段内
            // 例如：08:00-09:30 应该在 08:00-10:00 内
            boolean isContained = !requestedStart.isBefore(availableStart) && !requestedEnd.isAfter(availableEnd);
            
            return isContained;
        } catch (Exception e) {
            log.error("时间段比较失败: requested={}, available={}", requestedTimeSlot, availableTimeSlot, e);
            return false;
        }
    }

    // 解析 "HH:mm-HH:mm" 到 [startMin, endMin]
    private int[] parseSlotToMinutes(String slot) {
        if (slot == null || slot.length() < 11) return null;
        try {
            int h1 = Integer.parseInt(slot.substring(0, 2));
            int m1 = Integer.parseInt(slot.substring(3, 5));
            int h2 = Integer.parseInt(slot.substring(6, 8));
            int m2 = Integer.parseInt(slot.substring(9, 11));
            int s = h1 * 60 + m1;
            int e = h2 * 60 + m2;
            if (e > s) return new int[]{s, e};
        } catch (Exception ignore) {}
        return null;
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

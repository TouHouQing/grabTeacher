package com.touhouqing.grabteacherbackend.service.impl;

import com.touhouqing.grabteacherbackend.mapper.BookingRequestMapper;
import com.touhouqing.grabteacherbackend.mapper.CourseScheduleMapper;
import com.touhouqing.grabteacherbackend.mapper.TeacherDailyAvailabilityMapper;
import com.touhouqing.grabteacherbackend.model.entity.BookingRequest;
import com.touhouqing.grabteacherbackend.model.entity.CourseSchedule;
import com.touhouqing.grabteacherbackend.model.entity.TeacherDailyAvailability;
import com.touhouqing.grabteacherbackend.model.vo.CalendarSlotStatusVO;
import com.touhouqing.grabteacherbackend.model.vo.MonthlyCalendarVO;
import com.touhouqing.grabteacherbackend.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarServiceImpl implements CalendarService {

    private final CourseScheduleMapper courseScheduleMapper;
    private final BookingRequestMapper bookingRequestMapper;
    private final TeacherDailyAvailabilityMapper teacherDailyAvailabilityMapper;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String[] BASE_SLOTS = new String[]{
            "08:00-10:00", "10:00-12:00", "13:00-15:00",
            "15:00-17:00", "17:00-19:00", "19:00-21:00"
    };

    private static LocalTime[] parseSlot(String slot) {
        String[] ss = slot.split("-");
        return new LocalTime[]{LocalTime.parse(ss[0]), LocalTime.parse(ss[1])};
    }

    @Override
    @Cacheable(cacheNames = "teacherMonthlyCalendar", key = "'t:' + #teacherId + ':' + #year + ':' + #month")
    public MonthlyCalendarVO getTeacherMonthlyCalendar(Long teacherId, int year, int month) {
        LocalDate first = LocalDate.of(year, month, 1);
        LocalDate last = first.withDayOfMonth(first.lengthOfMonth());

        // 0) 读取日历可用性（仅当日历有配置时，未包含的基础段视为 unavailable）
        java.util.List<TeacherDailyAvailability> dailyList = teacherDailyAvailabilityMapper.selectList(
                new QueryWrapper<TeacherDailyAvailability>()
                        .eq("teacher_id", teacherId)
                        .between("available_date", first, last)
        );
        Map<LocalDate, java.util.Set<String>> dailyAvail = new HashMap<>();
        for (TeacherDailyAvailability tda : dailyList) {
            if (tda.getTimeSlotsJson() != null && !tda.getTimeSlotsJson().isBlank()) {
                try {
                    java.util.List<String> slots = MAPPER.readValue(tda.getTimeSlotsJson(), new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>(){});
                    dailyAvail.put(tda.getAvailableDate(), new java.util.HashSet<>(slots));
                } catch (Exception e) {
                    log.warn("解析日历可用性JSON失败 date={}, err={}", tda.getAvailableDate(), e.getMessage());
                }
            } else {
                dailyAvail.put(tda.getAvailableDate(), java.util.Collections.emptySet());
            }
        }

        // 1) 批量取当月排课（包含是否试听）
        List<CourseSchedule> schedules = courseScheduleMapper.findByTeacherIdAndDateRange(teacherId, first, last);
        Map<LocalDate, List<CourseSchedule>> schedulesByDate = new HashMap<>();
        for (CourseSchedule cs : schedules) {
            schedulesByDate.computeIfAbsent(cs.getScheduledDate(), k -> new ArrayList<>()).add(cs);
        }

        // 2) 批量取当月待处理预约（含试听/正式）
        List<BookingRequest> pendings = bookingRequestMapper.findPendingByTeacherAndDateRange(teacherId, first, last);
        Map<LocalDate, List<BookingRequest>> pendingByDate = new HashMap<>();
        for (BookingRequest br : pendings) {
            LocalDate d;
            if ("single".equalsIgnoreCase(br.getBookingType())) {
                d = br.getRequestedDate();
                if (d != null && !d.isBefore(first) && !d.isAfter(last)) {
                    pendingByDate.computeIfAbsent(d, k -> new ArrayList<>()).add(br);
                }
            } else if ("calendar".equalsIgnoreCase(br.getBookingType())) {
                // calendar类型：解析selectedSessionsJson获取具体日期和时间
                if (br.getSelectedSessionsJson() != null && !br.getSelectedSessionsJson().trim().isEmpty()) {
                    try {
                        ObjectMapper objectMapper = new ObjectMapper();
                        List<Map<String, Object>> sessions = objectMapper.readValue(
                            br.getSelectedSessionsJson(), 
                            new TypeReference<List<Map<String, Object>>>() {}
                        );
                        
                        for (Map<String, Object> session : sessions) {
                            String dateStr = (String) session.get("date");
                            if (dateStr != null) {
                                LocalDate sessionDate = LocalDate.parse(dateStr);
                                if (!sessionDate.isBefore(first) && !sessionDate.isAfter(last)) {
                                    pendingByDate.computeIfAbsent(sessionDate, k -> new ArrayList<>()).add(br);
                                }
                            }
                        }
                    } catch (Exception e) {
                        // JSON解析失败时静默处理，不影响其他逻辑
                    }
                }
            } else {
                // 其他类型：使用startDate和endDate
                if (br.getStartDate() != null && br.getEndDate() != null) {
                    LocalDate sd = br.getStartDate().isBefore(first) ? first : br.getStartDate();
                    LocalDate ed = br.getEndDate().isAfter(last) ? last : br.getEndDate();
                    for (LocalDate cur = sd; !cur.isAfter(ed); cur = cur.plusDays(1)) {
                        pendingByDate.computeIfAbsent(cur, k -> new ArrayList<>()).add(br);
                    }
                }
            }
        }

        List<CalendarSlotStatusVO> res = new ArrayList<>();
        for (LocalDate d = first; !d.isAfter(last); d = d.plusDays(1)) {
            final LocalDate day = d;

            List<CourseSchedule> daySch = schedulesByDate.getOrDefault(day, Collections.emptyList());
            List<BookingRequest> dayPend = pendingByDate.getOrDefault(day, Collections.emptyList());

            for (String slot : BASE_SLOTS) {
                LocalTime[] tt = parseSlot(slot);
                LocalTime baseStart = tt[0];
                LocalTime baseEnd = tt[1];

                // 基于教师日历可用性（仅用于在无任何占用/待审批时决定 available/unavailable）
                boolean hasDaily = dailyAvail.containsKey(day);
                boolean allowedByDaily = hasDaily && dailyAvail.getOrDefault(day, java.util.Collections.emptySet()).contains(slot);

                String tips = null;

                // 是否有正式课占用（已排程）
                boolean hasFormal = daySch.stream().anyMatch(cs -> {
                    Boolean trial = cs.getTrial();
                    if (Boolean.TRUE.equals(trial)) return false;
                    // overlap
                    return cs.getStartTime().isBefore(baseEnd) && baseStart.isBefore(cs.getEndTime());
                });

                // 是否有试听课占用（未取消，任意30分钟与基础段重叠）
                boolean anyTrial = daySch.stream().anyMatch(cs -> Boolean.TRUE.equals(cs.getTrial())
                        && !"cancelled".equalsIgnoreCase(cs.getScheduleStatus())
                        && cs.getScheduledDate().equals(day)
                        && cs.getStartTime().isBefore(baseEnd) && baseStart.isBefore(cs.getEndTime()));

                // 是否存在待审批的预约（单次或日历）
                boolean hasPendingBooking = false;
                if (!dayPend.isEmpty()) {
                    for (BookingRequest br : dayPend) {
                        if (!"pending".equalsIgnoreCase(br.getStatus())) continue; // 只处理待审批

                        if ("single".equalsIgnoreCase(br.getBookingType())) {
                            if (day.equals(br.getRequestedDate())
                                    && br.getRequestedStartTime() != null && br.getRequestedEndTime() != null
                                    && br.getRequestedStartTime().isBefore(baseEnd) && baseStart.isBefore(br.getRequestedEndTime())) {
                                hasPendingBooking = true; break;
                            }
                        } else if ("calendar".equalsIgnoreCase(br.getBookingType())) {
                            String json = br.getSelectedSessionsJson();
                            if (json != null && !json.isEmpty()) {
                                try {
                                    java.util.List<java.util.Map<String, String>> arr = MAPPER.readValue(json,
                                            new com.fasterxml.jackson.core.type.TypeReference<java.util.List<java.util.Map<String, String>>>(){});
                                    if (arr != null) {
                                        for (var m : arr) {
                                            String dateStr = m.get("date");
                                            String startStr = m.get("startTime");
                                            String endStr = m.get("endTime");
                                            if (startStr == null) startStr = m.get("start");
                                            if (endStr == null) endStr = m.get("end");
                                            if (dateStr != null && startStr != null && endStr != null) {
                                                LocalDate d0 = LocalDate.parse(dateStr);
                                                if (day.equals(d0)) {
                                                    LocalTime s0 = LocalTime.parse(startStr);
                                                    LocalTime e0 = LocalTime.parse(endStr);
                                                    if (s0.isBefore(baseEnd) && baseStart.isBefore(e0)) { hasPendingBooking = true; break; }
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    log.warn("解析日历预约JSON失败: {}", e.getMessage());
                                }
                            }
                        }
                        if (hasPendingBooking) break;
                    }
                }

                String status;
                if (hasFormal || anyTrial) {
                    status = "busy_formal"; // 统一标红：已有课程（正式或试听）
                } else if (hasPendingBooking) {
                    status = "unavailable"; // 待审批占位：保持灰色禁用
                } else if (allowedByDaily) {
                    status = "available";
                } else {
                    status = "unavailable";
                }

                res.add(CalendarSlotStatusVO.builder()
                        .date(day)
                        .slot(slot)
                        .status(status)
                        .tips(tips)
                        .build());
            }
        }

        return MonthlyCalendarVO.builder()
                .teacherId(teacherId)
                .year(year)
                .month(month)
                .slots(res)
                .build();
    }
}


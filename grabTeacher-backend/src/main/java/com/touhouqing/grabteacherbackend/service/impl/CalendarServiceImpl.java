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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
            } else {
                // 周期：粗略映射到范围内的每一天（仅用于标记，真实校验仍由提交接口处理）
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

                // 基于教师日历可用性设置初始状态
                boolean hasDaily = dailyAvail.containsKey(day);
                boolean allowedByDaily = hasDaily && dailyAvail.getOrDefault(day, java.util.Collections.emptySet()).contains(slot);

                String status = allowedByDaily ? "available" : "unavailable";
                String tips = null;

                // A. 正式课占用（已排程）
                boolean hasFormal = daySch.stream().anyMatch(cs -> {
                    Boolean trial = cs.getTrial();
                    if (Boolean.TRUE.equals(trial)) return false;
                    // overlap
                    return cs.getStartTime().isBefore(baseEnd) && baseStart.isBefore(cs.getEndTime());
                });
                if (hasFormal) {
                    status = "busy_formal"; // 该基础段内已有正式课，禁选（优先级最高）
                }

                // B. 试听占用影响（新版规则：基础段内任意30分钟试听均阻断正式课，含1.5h与2h）
                if ("available".equals(status)) {
                    boolean anyTrial = daySch.stream().anyMatch(cs -> Boolean.TRUE.equals(cs.getTrial())
                            && !"cancelled".equalsIgnoreCase(cs.getScheduleStatus())
                            && cs.getScheduledDate().equals(day)
                            && cs.getStartTime().isBefore(baseEnd) && baseStart.isBefore(cs.getEndTime()));
                    if (anyTrial) {
                        status = "busy_trial_base"; // 基础段内有试听：正式课禁用
                        tips = "该基础段内存在试听，正式课禁用";
                    }
                }

                // C. 待处理试听（同样阻断正式课）
                if ("available".equals(status)) {
                    boolean anyPendingTrial = dayPend.stream().anyMatch(br -> Boolean.TRUE.equals(br.getIsTrial())
                            && "pending".equalsIgnoreCase(br.getStatus())
                            && "single".equalsIgnoreCase(br.getBookingType())
                            && day.equals(br.getRequestedDate())
                            && br.getRequestedStartTime() != null && br.getRequestedEndTime() != null
                            && br.getRequestedStartTime().isBefore(baseEnd) && baseStart.isBefore(br.getRequestedEndTime()));
                    if (anyPendingTrial) {
                        status = "busy_trial_base";
                        tips = (tips == null ? "" : tips + "; ") + "基础段内有待审批试听，正式课禁用";
                    }
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


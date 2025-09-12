package com.touhouqing.grabteacherbackend.service.impl;

import com.touhouqing.grabteacherbackend.mapper.BookingRequestMapper;
import com.touhouqing.grabteacherbackend.mapper.CourseScheduleMapper;


import com.touhouqing.grabteacherbackend.model.entity.CourseSchedule;

import com.touhouqing.grabteacherbackend.model.vo.DayAvailabilityVO;
import com.touhouqing.grabteacherbackend.service.AvailabilityService;

import lombok.RequiredArgsConstructor;
import com.touhouqing.grabteacherbackend.service.TeacherDailyAvailabilityService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvailabilityServiceImpl implements AvailabilityService {

    private final CourseScheduleMapper courseScheduleMapper;
    private final BookingRequestMapper bookingRequestMapper;
    private final TeacherDailyAvailabilityService dailyService;

    @Override
    public DayAvailabilityVO getDayAvailability(Long teacherId, LocalDate date, String segment) {
        // 1) 准备基础2小时段列表
        List<String> baseSlots = Arrays.asList("08:00-10:00", "10:00-12:00", "13:00-15:00", "15:00-17:00", "17:00-19:00", "19:00-21:00");

        // 2) 拉取该日所有已排程（新表）到内存，构造 busyMap（字符串slot）
        List<CourseSchedule> schedules = courseScheduleMapper.findByTeacherIdAndDateRange(teacherId, date, date);
        Set<String> scheduledSlots = schedules.stream()
                .map(s -> s.getStartTime().toString() + "-" + s.getEndTime().toString())
                .collect(Collectors.toSet());

        // 3) 取教师该日的“按日历”可预约设置
        int weekday = date.getDayOfWeek().getValue();
        Map<LocalDate, List<String>> daily = dailyService.getDailyAvailability(teacherId, date, date);
        List<String> teacherDaySlots = daily.getOrDefault(date, Collections.emptyList());

        // 4) 计算基础2小时段可用性（正式课）：
        List<DayAvailabilityVO.BaseSlot> base = new ArrayList<>();
        for (String bs : baseSlots) {
            String[] t = bs.split("-");
            LocalTime bStart = LocalTime.parse(t[0]);
            LocalTime bEnd = LocalTime.parse(t[1]);
            List<String> reasons = new ArrayList<>();

            // 教师未设置该基础段为可预约（基础段必须被包含在某个可预约段内）
            boolean teacherContains = teacherDaySlots.stream().anyMatch(x -> isContained(bs, x));
            if (!teacherContains) reasons.add("teacherUnavailable");

            // 该基础段内存在已排程试听（包括30分钟试听课映射到2小时基础区间）
            if (hasScheduledTrialInBase(teacherId, date, bStart, bEnd)) reasons.add("scheduledTrial");
            // 该基础段内存在 pending 试听（包括30分钟试听课映射到2小时基础区间）
            if (bookingRequestMapper.countPendingTrialConflictsInBaseSlot(teacherId, date, bStart, bEnd) > 0) reasons.add("pendingTrial");

            // 该基础段内存在待处理的正式课预约
            if (bookingRequestMapper.countPendingFormalConflictsInBaseSlot(teacherId, date, bStart, bEnd, weekday) > 0) reasons.add("pendingFormal");

            // 该基础段是否与任一已排程课程冲突（busy）
            boolean busy = scheduledSlots.stream().anyMatch(s -> hasOverlap(bs, s));
            if (busy) reasons.add("busy");

            boolean formalAvailable = reasons.isEmpty();
            base.add(DayAvailabilityVO.BaseSlot.builder().slot(bs).formalAvailable(formalAvailable).reasons(reasons).build());
        }

        // 5) 生成试听30分钟段（按 segment 分段生成）
        List<String> trialSegments = buildTrialSegments(segment);
        List<DayAvailabilityVO.TrialSlot> trial = new ArrayList<>();
        for (String seg : trialSegments) {
            List<String> seg30 = generate30MinSlots(seg);
            for (String s : seg30) {
                // 仅生成在教师可预约时间内的
                boolean inTeacher = teacherDaySlots.stream().anyMatch(x -> isContained(s, x));
                if (!inTeacher) {
                    trial.add(DayAvailabilityVO.TrialSlot.builder().slot(s).trialAvailable(false).reasons(Collections.singletonList("teacherUnavailable")).build());
                    continue;
                }
                List<String> reasons = new ArrayList<>();
                // 与已排程课程冲突则不可用
                if (scheduledSlots.stream().anyMatch(x -> hasOverlap(s, x))) reasons.add("busyScheduled");
                // 完全相同30分钟是否已存在：已排程试听 或 pending 试听
                String[] tt = s.split("-");
                LocalTime st = LocalTime.parse(tt[0]);
                LocalTime ed = LocalTime.parse(tt[1]);
                boolean exactScheduled = isExactScheduledTrial(schedules, st, ed);
                boolean exactPending = bookingRequestMapper.countPendingExactTrialSlot(teacherId, date, st, ed) > 0;
                if (exactScheduled || exactPending) reasons.add("duplicateTrialSlot");

                // 检查是否有待处理的预约冲突（包括正式课和试听课）
                boolean hasPendingBooking = bookingRequestMapper.countPendingConflictsInTimeSlot(teacherId, date, st, ed, weekday) > 0;
                if (hasPendingBooking) reasons.add("pendingBooking");

                // 检查该30分钟试听课是否会影响基础2小时区间的可用性
                // 如果8:00-8:30有试听课，那么8:00-10:00的基础区间应该被标记为不可用（用于正式课）
                // 但是8:30-10:00之间还可以预约试听课
                String baseSlot = getBaseSlotForTrialSlot(s);
                if (baseSlot != null) {
                    String[] baseTimes = baseSlot.split("-");
                    LocalTime baseStart = LocalTime.parse(baseTimes[0]);
                    LocalTime baseEnd = LocalTime.parse(baseTimes[1]);

                    // 检查该基础区间是否已经被试听课占用
                    boolean hasTrialInBase = hasScheduledTrialInBase(teacherId, date, baseStart, baseEnd) ||
                                           bookingRequestMapper.countPendingTrialConflictsInBaseSlot(teacherId, date, baseStart, baseEnd) > 0;

                    if (hasTrialInBase) {
                        // 如果基础区间已被试听课占用，检查当前30分钟段是否在占用范围内
                        if (isTrialSlotInOccupiedBase(s, baseSlot)) {
                            reasons.add("baseSlotOccupiedByTrial");
                        }
                    }
                }

                boolean ok = reasons.isEmpty();
                trial.add(DayAvailabilityVO.TrialSlot.builder().slot(s).trialAvailable(ok).reasons(reasons).build());
            }
        }

        return DayAvailabilityVO.builder()
                .date(date.toString())
                .baseSlots(base)
                .trialSlots(trial)
                .build();
    }

    private boolean hasScheduledTrialInBase(Long teacherId, LocalDate date, LocalTime baseStart, LocalTime baseEnd) {
        // 复用已有 mapper 统计已排程试听在基础段中的数量
        return courseScheduleMapper.countTrialConflictsInBaseSlot(teacherId, date, baseStart, baseEnd) > 0;
    }

    private boolean isExactScheduledTrial(List<CourseSchedule> schedules, LocalTime st, LocalTime ed) {
        for (CourseSchedule cs : schedules) {
            if (cs.getStartTime() != null && cs.getEndTime() != null && cs.getStartTime().equals(st) && cs.getEndTime().equals(ed)) {
                return true;
            }
        }
        return false;
    }

    private boolean isContained(String child, String parent) {
        String[] c = child.split("-");
        String[] p = parent.split("-");
        LocalTime cs = LocalTime.parse(c[0]);
        LocalTime ce = LocalTime.parse(c[1]);
        LocalTime ps = LocalTime.parse(p[0]);
        LocalTime pe = LocalTime.parse(p[1]);
        return (cs.equals(ps) || cs.isAfter(ps)) && (ce.equals(pe) || ce.isBefore(pe));
    }

    private boolean hasOverlap(String a, String b) {
        String[] A = a.split("-");
        String[] B = b.split("-");
        LocalTime as = LocalTime.parse(A[0]);
        LocalTime ae = LocalTime.parse(A[1]);
        LocalTime bs = LocalTime.parse(B[0]);
        LocalTime be = LocalTime.parse(B[1]);
        return as.isBefore(be) && bs.isBefore(ae);
    }

    private List<String> buildTrialSegments(String segment) {
        // 支持 morning/afternoon/evening 三段；为空则返回全部三段
        List<String> all = Arrays.asList("08:00-12:00", "13:00-17:00", "17:00-21:00");
        if (segment == null || segment.isEmpty()) return all;
        switch (segment) {
            case "morning": return Collections.singletonList("08:00-12:00");
            case "afternoon": return Collections.singletonList("13:00-17:00");
            case "evening": return Collections.singletonList("17:00-21:00");
            default: return all;
        }
    }

    private List<String> generate30MinSlots(String range) {
        String[] t = range.split("-");
        LocalTime s = LocalTime.parse(t[0]);
        LocalTime e = LocalTime.parse(t[1]);
        List<String> res = new ArrayList<>();
        LocalTime cur = s;
        while (cur.isBefore(e)) {
            LocalTime nx = cur.plusMinutes(30);
            if (nx.isAfter(e)) break;
            res.add(cur.toString() + "-" + nx.toString());
            cur = nx;
        }
        return res;
    }

    /**
     * 根据30分钟试听课时间段获取对应的基础2小时区间
     */
    private String getBaseSlotForTrialSlot(String trialSlot) {
        String[] times = trialSlot.split("-");
        LocalTime start = LocalTime.parse(times[0]);
        LocalTime end = LocalTime.parse(times[1]);

        // 基础2小时时间段：08:00-10:00, 10:00-12:00, 13:00-15:00, 15:00-17:00, 17:00-19:00, 19:00-21:00
        String[] baseSlots = {
            "08:00-10:00", "10:00-12:00", "13:00-15:00",
            "15:00-17:00", "17:00-19:00", "19:00-21:00"
        };

        for (String baseSlot : baseSlots) {
            String[] baseTimes = baseSlot.split("-");
            LocalTime baseStart = LocalTime.parse(baseTimes[0]);
            LocalTime baseEnd = LocalTime.parse(baseTimes[1]);

            // 检查30分钟试听课是否在基础区间内
            if ((start.isAfter(baseStart) || start.equals(baseStart)) &&
                (end.isBefore(baseEnd) || end.equals(baseEnd))) {
                return baseSlot;
            }
        }

        return null;
    }

    /**
     * 检查30分钟试听课时间段是否在已被占用的基础区间内
     * 如果8:00-8:30有试听课，那么8:00-10:00的基础区间被占用
     * 但是8:30-10:00之间还可以预约试听课
     */
    private boolean isTrialSlotInOccupiedBase(String trialSlot, String baseSlot) {
        String[] trialTimes = trialSlot.split("-");
        String[] baseTimes = baseSlot.split("-");

        LocalTime trialStart = LocalTime.parse(trialTimes[0]);
        LocalTime baseStart = LocalTime.parse(baseTimes[0]);

        // 如果试听课的开始时间等于基础区间的开始时间，则认为该基础区间被占用
        // 例如：8:00-8:30的试听课会占用8:00-10:00的基础区间
        if (trialStart.equals(baseStart)) {
            return true;
        }

        return false;
    }
}


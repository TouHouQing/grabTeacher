package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.touhouqing.grabteacherbackend.mapper.TeacherDailyAvailabilityMapper;
import com.touhouqing.grabteacherbackend.model.dto.DailyTimeSlotDTO;
import com.touhouqing.grabteacherbackend.model.entity.TeacherDailyAvailability;
import com.touhouqing.grabteacherbackend.service.TeacherDailyAvailabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherDailyAvailabilityServiceImpl implements TeacherDailyAvailabilityService {

    private final TeacherDailyAvailabilityMapper dailyMapper;
    private static final ObjectMapper OM = new ObjectMapper();

    private static final Set<String> BASE_SLOTS = new LinkedHashSet<>(Arrays.asList(
            "08:00-10:00", "10:00-12:00", "13:00-15:00",
            "15:00-17:00", "17:00-19:00", "19:00-21:00"
    ));

    @Override
    @Transactional
    public void setDailyAvailability(Long teacherId, List<DailyTimeSlotDTO> items, boolean overwrite) {
        if (teacherId == null) throw new RuntimeException("教师ID不能为空");
        if (items == null) items = Collections.emptyList();

        // 参数校验 & 归一化
        for (DailyTimeSlotDTO it : items) {
            if (it.getDate() == null) throw new RuntimeException("存在缺少日期的项");
            if (it.getTimeSlots() == null) it.setTimeSlots(Collections.emptyList());
            for (String slot : it.getTimeSlots()) {
                if (!BASE_SLOTS.contains(slot)) {
                    throw new RuntimeException("非法时间段: " + slot + ", 只允许: " + BASE_SLOTS);
                }
            }
        }

        if (items.isEmpty()) {
            log.info("教师{}未提交任何日期，忽略", teacherId);
            return;
        }

        // 预构建基础段顺序索引，确保排序稳定
        final List<String> baseOrder = new ArrayList<>(BASE_SLOTS);

        if (overwrite) {
            // 覆盖：直接删除这些日期旧记录，然后写入新的（非空才插入）
            Set<LocalDate> dates = items.stream().map(DailyTimeSlotDTO::getDate).collect(Collectors.toSet());
            QueryWrapper<TeacherDailyAvailability> del = new QueryWrapper<>();
            del.eq("teacher_id", teacherId).in("available_date", dates);
            dailyMapper.delete(del);

            for (DailyTimeSlotDTO it : items) {
                List<String> in = it.getTimeSlots();
                if (in == null || in.isEmpty()) {
                    // 空集表示该日不可上课，不插入记录
                    continue;
                }
                // 去重并按基础段顺序排序
                Set<String> uniq = new LinkedHashSet<>(in);
                List<String> normalized = baseOrder.stream().filter(uniq::contains).collect(Collectors.toList());
                try {
                    String json = OM.writeValueAsString(normalized);
                    TeacherDailyAvailability rec = TeacherDailyAvailability.builder()
                            .teacherId(teacherId)
                            .availableDate(it.getDate())
                            .timeSlotsJson(json)
                            .deleted(false)
                            .build();
                    dailyMapper.insert(rec);
                } catch (Exception e) {
                    throw new RuntimeException("时间段序列化失败");
                }
            }
        } else {
            // 合并：与已存在记录做并集（保持基础段顺序）
            Set<LocalDate> dates = items.stream().map(DailyTimeSlotDTO::getDate).collect(Collectors.toSet());
            QueryWrapper<TeacherDailyAvailability> qw = new QueryWrapper<>();
            qw.eq("teacher_id", teacherId).in("available_date", dates);
            List<TeacherDailyAvailability> existing = dailyMapper.selectList(qw);
            Map<LocalDate, List<String>> existMap = new HashMap<>();
            for (TeacherDailyAvailability rec : existing) {
                try {
                    List<String> arr = OM.readValue(rec.getTimeSlotsJson(), new com.fasterxml.jackson.core.type.TypeReference<List<String>>(){});
                    existMap.put(rec.getAvailableDate(), arr);
                } catch (Exception e) {
                    existMap.put(rec.getAvailableDate(), Collections.emptyList());
                }
            }

            for (DailyTimeSlotDTO it : items) {
                List<String> cur = it.getTimeSlots();
                List<String> prev = existMap.getOrDefault(it.getDate(), Collections.emptyList());
                // 合并并去重
                LinkedHashSet<String> mergedSet = new LinkedHashSet<>();
                if (prev != null) mergedSet.addAll(prev);
                if (cur != null) mergedSet.addAll(cur);
                // 过滤非法并按基础段顺序排序
                List<String> merged = baseOrder.stream().filter(mergedSet::contains).collect(Collectors.toList());

                // 先删后插（空集则仅删除代表该日不可上课）
                QueryWrapper<TeacherDailyAvailability> delOne = new QueryWrapper<>();
                delOne.eq("teacher_id", teacherId).eq("available_date", it.getDate());
                dailyMapper.delete(delOne);

                if (!merged.isEmpty()) {
                    try {
                        String json = OM.writeValueAsString(merged);
                        TeacherDailyAvailability rec = TeacherDailyAvailability.builder()
                                .teacherId(teacherId)
                                .availableDate(it.getDate())
                                .timeSlotsJson(json)
                                .deleted(false)
                                .build();
                        dailyMapper.insert(rec);
                    } catch (Exception e) {
                        throw new RuntimeException("时间段序列化失败");
                    }
                }
            }
        }

        log.info("已设置教师{}在{}天的日历可上课时间 (overwrite={})", teacherId, items.size(), overwrite);
    }

    @Override
    public Map<LocalDate, List<String>> getDailyAvailability(Long teacherId, LocalDate start, LocalDate end) {
        if (teacherId == null) throw new RuntimeException("教师ID不能为空");
        if (start == null || end == null || end.isBefore(start)) throw new RuntimeException("日期范围不合法");

        QueryWrapper<TeacherDailyAvailability> qw = new QueryWrapper<>();
        qw.eq("teacher_id", teacherId)
          .eq("is_deleted", false)
          .between("available_date", start, end)
          .orderByAsc("available_date");
        List<TeacherDailyAvailability> list = dailyMapper.selectList(qw);

        Map<LocalDate, List<String>> map = new LinkedHashMap<>();
        for (TeacherDailyAvailability rec : list) {
            try {
                List<String> slots = OM.readValue(rec.getTimeSlotsJson(), new TypeReference<List<String>>(){});
                map.put(rec.getAvailableDate(), slots);
            } catch (Exception e) {
                map.put(rec.getAvailableDate(), Collections.emptyList());
            }
        }
        return map;
    }
}


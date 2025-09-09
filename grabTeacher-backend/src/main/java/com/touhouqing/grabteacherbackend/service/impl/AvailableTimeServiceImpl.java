package com.touhouqing.grabteacherbackend.service.impl;

import com.touhouqing.grabteacherbackend.model.dto.AvailableTimeQueryDTO;
import com.touhouqing.grabteacherbackend.model.vo.AvailableTimeVO;
import com.touhouqing.grabteacherbackend.model.dto.TimeSlotDTO;
import com.touhouqing.grabteacherbackend.model.entity.Teacher;
import com.touhouqing.grabteacherbackend.mapper.TeacherMapper;
import com.touhouqing.grabteacherbackend.service.AvailableTimeService;
import com.touhouqing.grabteacherbackend.util.TimeSlotUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.touhouqing.grabteacherbackend.service.CacheKeyEvictor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.CacheManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 可上课时间服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AvailableTimeServiceImpl implements AvailableTimeService {

    private final TeacherMapper teacherMapper;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CacheKeyEvictor cacheKeyEvictor;
    private final CacheManager cacheManager;

    @Override
    @Cacheable(cacheNames = "teacherAvailableTime", key = "#teacherId")
    public AvailableTimeVO getTeacherAvailableTime(Long teacherId) {
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            throw new RuntimeException("教师不存在");
        }

        List<TimeSlotDTO> timeSlots = TimeSlotUtil.fromJsonString(teacher.getAvailableTimeSlots());

        AvailableTimeVO response = AvailableTimeVO.builder()
                .teacherId(teacher.getId())
                .teacherName(teacher.getRealName())
                .availableTimeSlots(timeSlots)
                .lastUpdated(LocalDateTime.now().format(FORMATTER))
                .build();

        log.info("构建的响应对象: {}", response);

        // 计算统计信息
        response.calculateStats();

        return response;
    }

    @Override
    @Transactional
    public AvailableTimeVO setTeacherAvailableTime(AvailableTimeQueryDTO request) {
        Teacher teacher = teacherMapper.selectById(request.getTeacherId());
        if (teacher == null) {
            throw new RuntimeException("教师不存在");
        }

        // 处理可上课时间
        if (request.getAvailableTimeSlots() == null || request.getAvailableTimeSlots().isEmpty()) {
            // 如果没有提供可上课时间或为空，设置为null（表示所有时间都可以）
            teacher.setAvailableTimeSlots(null);
            log.info("教师清空可上课时间设置，默认所有时间可用: teacherId={}", teacher.getId());
        } else {
            // 验证时间安排格式
            if (!TimeSlotUtil.isValidTimeSlots(request.getAvailableTimeSlots())) {
                throw new RuntimeException("时间安排格式不正确");
            }

            // TODO: 添加时间冲突验证
            // 可以在这里调用 TimeValidationService 来验证时间设置是否与现有课程冲突

            // 转换为JSON并保存
            String timeSlotsJson = TimeSlotUtil.toJsonString(request.getAvailableTimeSlots());
            teacher.setAvailableTimeSlots(timeSlotsJson);

            log.info("教师可上课时间更新成功: teacherId={}, totalSlots={}",
                    teacher.getId(), request.getTotalTimeSlots());
        }

        teacherMapper.updateById(teacher);

        // 可上课时间变化：清理教师课表/可用性缓存 + 清 teacherAvailableTime
        try { cacheKeyEvictor.evictTeacherScheduleAndAvailability(teacher.getId()); } catch (Exception ignore) {}
        try {
            org.springframework.cache.Cache c = cacheManager.getCache("teacherAvailableTime");
            if (c != null) { c.evict(teacher.getId()); }
        } catch (Exception ignore) {}

        // 返回更新后的信息（直查 DB，避免命中旧缓存）
        Teacher fresh = teacherMapper.selectById(request.getTeacherId());
        List<TimeSlotDTO> timeSlots = TimeSlotUtil.fromJsonString(fresh.getAvailableTimeSlots());
        AvailableTimeVO response = AvailableTimeVO.builder()
                .teacherId(fresh.getId())
                .teacherName(fresh.getRealName())
                .availableTimeSlots(timeSlots)
                .lastUpdated(LocalDateTime.now().format(FORMATTER))
                .build();
        response.calculateStats();
        return response;
    }

    @Override
    public Long getTeacherIdByUserId(Long userId) {
        Teacher teacher = teacherMapper.findByUserId(userId);
        return teacher != null ? teacher.getId() : null;
    }

    @Override
    public java.util.List<AvailableTimeVO> getTeachersAvailableTime(java.util.List<Long> teacherIds) {
        java.util.List<AvailableTimeVO> result = new java.util.ArrayList<>();
        if (teacherIds == null || teacherIds.isEmpty()) {
            return result;
        }
        // 批量查询，避免 N 次 DB 请求
        java.util.List<Teacher> teachers = teacherMapper.selectBatchIds(teacherIds);
        for (Teacher t : teachers) {
            java.util.List<TimeSlotDTO> timeSlots = TimeSlotUtil.fromJsonString(t.getAvailableTimeSlots());
            AvailableTimeVO vo = AvailableTimeVO.builder()
                    .teacherId(t.getId())
                    .teacherName(t.getRealName())
                    .availableTimeSlots(timeSlots)
                    .lastUpdated(java.time.LocalDateTime.now().format(FORMATTER))
                    .build();
            vo.calculateStats();
            result.add(vo);
        }
        return result;
    }

}



package com.touhouqing.grabteacherbackend.service.impl;

import com.touhouqing.grabteacherbackend.dto.AvailableTimeRequest;
import com.touhouqing.grabteacherbackend.dto.AvailableTimeResponse;
import com.touhouqing.grabteacherbackend.dto.TimeSlotDTO;
import com.touhouqing.grabteacherbackend.entity.Teacher;
import com.touhouqing.grabteacherbackend.mapper.TeacherMapper;
import com.touhouqing.grabteacherbackend.service.AvailableTimeService;
import com.touhouqing.grabteacherbackend.util.TimeSlotUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    public AvailableTimeResponse getTeacherAvailableTime(Long teacherId) {
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            throw new RuntimeException("教师不存在");
        }

        List<TimeSlotDTO> timeSlots = TimeSlotUtil.fromJsonString(teacher.getAvailableTimeSlots());
        
        AvailableTimeResponse response = AvailableTimeResponse.builder()
                .teacherId(teacher.getId())
                .teacherName(teacher.getRealName())
                .availableTimeSlots(timeSlots)
                .lastUpdated(LocalDateTime.now().format(FORMATTER))
                .build();
        
        // 计算统计信息
        response.calculateStats();
        
        return response;
    }

    @Override
    @Transactional
    public AvailableTimeResponse setTeacherAvailableTime(AvailableTimeRequest request) {
        Teacher teacher = teacherMapper.selectById(request.getTeacherId());
        if (teacher == null) {
            throw new RuntimeException("教师不存在");
        }

        // 验证时间安排格式
        if (!TimeSlotUtil.isValidTimeSlots(request.getAvailableTimeSlots())) {
            throw new RuntimeException("时间安排格式不正确");
        }

        // 转换为JSON并保存
        String timeSlotsJson = TimeSlotUtil.toJsonString(request.getAvailableTimeSlots());
        teacher.setAvailableTimeSlots(timeSlotsJson);
        
        teacherMapper.updateById(teacher);
        
        log.info("教师可上课时间更新成功: teacherId={}, totalSlots={}", 
                teacher.getId(), request.getTotalTimeSlots());

        // 返回更新后的信息
        return getTeacherAvailableTime(request.getTeacherId());
    }

    @Override
    public Long getTeacherIdByUserId(Long userId) {
        Teacher teacher = teacherMapper.findByUserId(userId);
        return teacher != null ? teacher.getId() : null;
    }
}

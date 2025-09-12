package com.touhouqing.grabteacherbackend.service;

import com.touhouqing.grabteacherbackend.model.dto.DailyTimeSlotDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface TeacherDailyAvailabilityService {
    void setDailyAvailability(Long teacherId, List<DailyTimeSlotDTO> items, boolean overwrite);
    Map<LocalDate, List<String>> getDailyAvailability(Long teacherId, LocalDate start, LocalDate end);
}


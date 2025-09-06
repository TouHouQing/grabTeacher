package com.touhouqing.grabteacherbackend.service;

import com.touhouqing.grabteacherbackend.model.vo.DayAvailabilityVO;

import java.time.LocalDate;

public interface AvailabilityService {
    DayAvailabilityVO getDayAvailability(Long teacherId, LocalDate date, String segment);
}


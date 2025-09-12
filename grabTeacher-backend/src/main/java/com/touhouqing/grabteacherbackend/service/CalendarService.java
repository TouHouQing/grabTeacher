package com.touhouqing.grabteacherbackend.service;

import com.touhouqing.grabteacherbackend.model.vo.MonthlyCalendarVO;

public interface CalendarService {
    MonthlyCalendarVO getTeacherMonthlyCalendar(Long teacherId, int year, int month);
}


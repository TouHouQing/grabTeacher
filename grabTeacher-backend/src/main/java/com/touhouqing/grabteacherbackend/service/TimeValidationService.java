package com.touhouqing.grabteacherbackend.service;

import com.touhouqing.grabteacherbackend.dto.TimeSlotDTO;
import com.touhouqing.grabteacherbackend.dto.TimeValidationResultDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * 时间验证服务接口
 */
public interface TimeValidationService {
    
    /**
     * 验证教师设置的可上课时间是否与现有课程冲突
     * @param teacherId 教师ID
     * @param availableTimeSlots 教师设置的可上课时间
     * @param startDate 验证开始日期
     * @param endDate 验证结束日期
     * @return 验证结果
     */
    TimeValidationResultDTO validateTeacherAvailableTime(Long teacherId,
                                                         List<TimeSlotDTO> availableTimeSlots,
                                                         LocalDate startDate,
                                                         LocalDate endDate);
    
    /**
     * 获取教师时间设置建议
     * @param teacherId 教师ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 时间设置建议
     */
    List<TimeSlotDTO> getRecommendedTimeSlots(Long teacherId, LocalDate startDate, LocalDate endDate);
    
    /**
     * 检查学生预约时间是否与教师可用时间匹配
     * @param teacherId 教师ID
     * @param studentPreferredWeekdays 学生偏好星期几
     * @param studentPreferredTimeSlots 学生偏好时间段
     * @return 匹配结果
     */
    TimeValidationResultDTO validateStudentBookingTime(Long teacherId,
                                                       List<Integer> studentPreferredWeekdays,
                                                       List<String> studentPreferredTimeSlots);

    /**
     * 验证周期性预约的时间匹配度（考虑具体日期范围和课程冲突）
     * @param teacherId 教师ID
     * @param studentPreferredWeekdays 学生偏好星期几
     * @param studentPreferredTimeSlots 学生偏好时间段
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param totalTimes 总课程次数
     * @return 详细的匹配结果
     */
    TimeValidationResultDTO validateRecurringBookingTime(Long teacherId,
                                                         List<Integer> studentPreferredWeekdays,
                                                         List<String> studentPreferredTimeSlots,
                                                         LocalDate startDate,
                                                         LocalDate endDate,
                                                         Integer totalTimes);
}

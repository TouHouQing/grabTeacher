package com.touhouqing.grabteacherbackend.service;

import com.touhouqing.grabteacherbackend.dto.TeacherDetailResponse;
import com.touhouqing.grabteacherbackend.dto.TeacherInfoRequest;
import com.touhouqing.grabteacherbackend.dto.TeacherListResponse;
import com.touhouqing.grabteacherbackend.dto.TeacherMatchRequest;
import com.touhouqing.grabteacherbackend.dto.TeacherMatchResponse;
import com.touhouqing.grabteacherbackend.dto.TeacherProfileResponse;
import com.touhouqing.grabteacherbackend.dto.TeacherScheduleResponse;
import com.touhouqing.grabteacherbackend.dto.TimeSlotAvailability;
import com.touhouqing.grabteacherbackend.entity.Teacher;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface TeacherService {
    
    /**
     * 根据用户ID获取教师信息
     */
    Teacher getTeacherByUserId(Long userId);
    
    /**
     * 根据ID获取教师信息
     */
    Teacher getTeacherById(Long teacherId);

    /**
     * 根据ID获取教师详情（包含用户信息、科目信息、年级信息）
     */
    TeacherDetailResponse getTeacherDetailById(Long teacherId);

    /**
     * 根据用户ID获取教师详细信息（包含科目信息）
     */
    TeacherProfileResponse getTeacherProfileByUserId(Long userId);
    
    /**
     * 获取教师列表
     */
    List<Teacher> getTeacherList(int page, int size, String subject, String keyword);

    /**
     * 获取教师列表（包含科目信息）
     */
    List<TeacherListResponse> getTeacherListWithSubjects(int page, int size, String subject, String grade, String keyword);

    /**
     * 获取精选教师列表（天下名师页面使用）
     */
    List<TeacherListResponse> getFeaturedTeachers(int page, int size, String subject, String grade, String keyword);
    
    /**
     * 更新教师信息
     */
    Teacher updateTeacherInfo(Long userId, TeacherInfoRequest request);

    /**
     * 匹配教师
     */
    List<TeacherMatchResponse> matchTeachers(TeacherMatchRequest request);

    /**
     * 获取所有可用的年级选项（从课程表获取）
     */
    List<String> getAvailableGrades();

    /**
     * 获取教师的公开课表（供学生查看）
     * @param teacherId 教师ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 教师课表信息
     */
    TeacherScheduleResponse getTeacherPublicSchedule(Long teacherId, LocalDate startDate, LocalDate endDate);

    /**
     * 检查教师时间段可用性（供学生预约时查看）
     * @param teacherId 教师ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param timeSlots 要检查的时间段列表（可选）
     * @return 时间段可用性列表
     */
    List<TimeSlotAvailability> checkTeacherAvailability(Long teacherId, LocalDate startDate, LocalDate endDate, List<String> timeSlots);

    /**
     * 获取教师控制台统计数据
     * @param userId 教师用户ID
     * @return 统计数据
     */
    Map<String, Object> getTeacherStatistics(Long userId);
}
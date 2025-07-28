package com.touhouqing.grabteacherbackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.dto.*;
import com.touhouqing.grabteacherbackend.entity.BookingRequest;

import java.time.LocalDate;
import java.util.List;

public interface BookingService {
    
    /**
     * 创建预约申请
     * @param request 预约申请参数
     * @param studentUserId 学生用户ID
     * @return 预约申请响应数据
     */
    BookingResponseDTO createBookingRequest(BookingRequestDTO request, Long studentUserId);
    
    /**
     * 审批预约申请（管理员操作）
     * @param bookingId 预约申请ID
     * @param approval 审批参数
     * @param adminUserId 管理员用户ID
     * @return 预约申请响应数据
     */
    BookingResponseDTO approveBookingRequest(Long bookingId, BookingApprovalDTO approval, Long adminUserId);
    
    /**
     * 取消预约申请（学生操作）
     * @param bookingId 预约申请ID
     * @param studentUserId 学生用户ID
     * @return 预约申请响应数据
     */
    BookingResponseDTO cancelBookingRequest(Long bookingId, Long studentUserId);
    
    /**
     * 根据ID获取预约申请详情
     * @param bookingId 预约申请ID
     * @return 预约申请响应数据
     */
    BookingResponseDTO getBookingRequestById(Long bookingId);
    
    /**
     * 获取学生的预约申请列表
     * @param studentUserId 学生用户ID
     * @param page 页码
     * @param size 每页大小
     * @param status 状态筛选
     * @return 分页预约申请列表
     */
    Page<BookingResponseDTO> getStudentBookingRequests(Long studentUserId, int page, int size, String status);
    
    /**
     * 获取教师的预约申请列表
     * @param teacherUserId 教师用户ID
     * @param page 页码
     * @param size 每页大小
     * @param status 状态筛选
     * @return 分页预约申请列表
     */
    Page<BookingResponseDTO> getTeacherBookingRequests(Long teacherUserId, int page, int size, String status);
    
    /**
     * 获取教师的课程安排列表
     * @param teacherUserId 教师用户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 课程安排列表
     */
    List<ScheduleResponseDTO> getTeacherSchedules(Long teacherUserId, LocalDate startDate, LocalDate endDate);
    
    /**
     * 获取学生的课程安排列表
     * @param studentUserId 学生用户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 课程安排列表
     */
    List<ScheduleResponseDTO> getStudentSchedules(Long studentUserId, LocalDate startDate, LocalDate endDate);
    
    /**
     * 检查用户是否可以使用免费试听
     * @param userId 用户ID
     * @return 是否可以使用免费试听
     */
    boolean canUseFreeTrial(Long userId);
    
    /**
     * 标记用户已使用免费试听
     * @param userId 用户ID
     */
    void markTrialUsed(Long userId);
    
    /**
     * 检查教师在指定时间是否有冲突
     * @param teacherId 教师ID
     * @param date 日期
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 是否有冲突
     */
    boolean hasTeacherTimeConflict(Long teacherId, LocalDate date, String startTime, String endTime);
    
    /**
     * 检查学生在指定时间是否有冲突
     * @param studentId 学生ID
     * @param date 日期
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 是否有冲突
     */
    boolean hasStudentTimeConflict(Long studentId, LocalDate date, String startTime, String endTime);
    
    /**
     * 生成周期性课程安排
     * @param bookingRequest 预约申请
     * @return 生成的课程安排数量
     */
    int generateRecurringSchedules(BookingRequest bookingRequest);
    
    /**
     * 获取管理员预约申请列表（管理员操作）
     * @param page 页码
     * @param size 每页大小
     * @param status 状态筛选
     * @param keyword 搜索关键词
     * @return 分页预约申请列表
     */
    Page<BookingResponseDTO> getAdminBookingRequests(int page, int size, String status, String keyword);
    

}

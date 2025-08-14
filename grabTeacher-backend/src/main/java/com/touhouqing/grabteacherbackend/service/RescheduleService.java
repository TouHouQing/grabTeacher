package com.touhouqing.grabteacherbackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.dto.RescheduleApprovalDTO;
import com.touhouqing.grabteacherbackend.dto.RescheduleRequestDTO;
import com.touhouqing.grabteacherbackend.dto.RescheduleResponseDTO;

/**
 * 调课服务接口
 */
public interface RescheduleService {

    /**
     * 创建调课申请（学生操作）
     * @param request 调课申请参数
     * @param studentUserId 学生用户ID
     * @return 调课申请响应数据
     */
    RescheduleResponseDTO createRescheduleRequest(RescheduleRequestDTO request, Long studentUserId);

    /**
     * 审批调课申请（教师操作）
     * @param rescheduleId 调课申请ID
     * @param approval 审批参数
     * @param teacherUserId 教师用户ID
     * @return 调课申请响应数据
     */
    RescheduleResponseDTO approveRescheduleRequest(Long rescheduleId, RescheduleApprovalDTO approval, Long teacherUserId);

    /**
     * 取消调课申请（学生操作）
     * @param rescheduleId 调课申请ID
     * @param studentUserId 学生用户ID
     * @return 调课申请响应数据
     */
    RescheduleResponseDTO cancelRescheduleRequest(Long rescheduleId, Long studentUserId);

    /**
     * 获取学生的调课申请列表
     * @param studentUserId 学生用户ID
     * @param page 页码
     * @param size 每页大小
     * @param status 状态筛选
     * @return 分页调课申请列表
     */
    Page<RescheduleResponseDTO> getStudentRescheduleRequests(Long studentUserId, int page, int size, String status);

    /**
     * 获取教师需要审批的调课申请列表
     * @param teacherUserId 教师用户ID
     * @param page 页码
     * @param size 每页大小
     * @param status 状态筛选
     * @return 分页调课申请列表
     */
    Page<RescheduleResponseDTO> getTeacherRescheduleRequests(Long teacherUserId, int page, int size, String status);

    /**
     * 根据ID获取调课申请详情
     * @param rescheduleId 调课申请ID
     * @param currentUserId 当前用户ID
     * @param userType 用户类型
     * @return 调课申请详情
     */
    RescheduleResponseDTO getRescheduleRequestById(Long rescheduleId, Long currentUserId, String userType);

    /**
     * 获取教师待处理的调课申请数量
     * @param teacherUserId 教师用户ID
     * @return 待处理数量
     */
    int getPendingRescheduleCount(Long teacherUserId);

    /**
     * 检查学生是否可以申请调课
     * @param scheduleId 课程安排ID
     * @param studentUserId 学生用户ID
     * @return 是否可以申请
     */
    boolean canApplyReschedule(Long scheduleId, Long studentUserId);

    /**
     * 检查时间冲突
     * @param teacherId 教师ID
     * @param studentId 学生ID
     * @param newDate 新日期
     * @param newStartTime 新开始时间
     * @param newEndTime 新结束时间
     * @param excludeScheduleId 排除的课程安排ID
     * @return 是否有冲突
     */
    boolean hasTimeConflict(Long teacherId, Long studentId, String newDate, String newStartTime, String newEndTime, Long excludeScheduleId);
}

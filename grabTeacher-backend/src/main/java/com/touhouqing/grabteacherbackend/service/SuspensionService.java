package com.touhouqing.grabteacherbackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.model.dto.SuspensionApprovalDTO;
import com.touhouqing.grabteacherbackend.model.dto.SuspensionApplyDTO;
import com.touhouqing.grabteacherbackend.model.vo.SuspensionVO;

public interface SuspensionService {
    /**
     * 学生或教师创建请假申请（根据当前用户自动识别身份）。
     */
    SuspensionVO createSuspensionRequest(SuspensionApplyDTO request, Long currentUserId);

    SuspensionVO adminApproveSuspension(Long requestId, SuspensionApprovalDTO approval, Long adminUserId);

    Page<SuspensionVO> getStudentSuspensionRequests(Long studentUserId, int page, int size, String status);

    /** 教师查看自己的请假申请列表 */
    Page<SuspensionVO> getTeacherSuspensionRequests(Long teacherUserId, int page, int size, String status);

    Page<SuspensionVO> getAllSuspensionRequests(int page, int size, String status);
}

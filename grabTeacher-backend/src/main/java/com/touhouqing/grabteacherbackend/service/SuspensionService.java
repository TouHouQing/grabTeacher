package com.touhouqing.grabteacherbackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.model.dto.SuspensionApprovalDTO;
import com.touhouqing.grabteacherbackend.model.dto.SuspensionApplyDTO;
import com.touhouqing.grabteacherbackend.model.vo.SuspensionVO;

public interface SuspensionService {
    SuspensionVO createSuspensionRequest(SuspensionApplyDTO request, Long studentUserId);
    SuspensionVO adminApproveSuspension(Long requestId, SuspensionApprovalDTO approval, Long adminUserId);
    Page<SuspensionVO> getStudentSuspensionRequests(Long studentUserId, int page, int size, String status);
    Page<SuspensionVO> getAllSuspensionRequests(int page, int size, String status);
}



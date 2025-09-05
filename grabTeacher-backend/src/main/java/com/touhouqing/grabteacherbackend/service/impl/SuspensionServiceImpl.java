package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.mapper.*;
import com.touhouqing.grabteacherbackend.model.dto.SuspensionApprovalDTO;
import com.touhouqing.grabteacherbackend.model.dto.SuspensionApplyDTO;
import com.touhouqing.grabteacherbackend.model.entity.*;
import com.touhouqing.grabteacherbackend.model.vo.SuspensionVO;
import com.touhouqing.grabteacherbackend.service.SuspensionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class SuspensionServiceImpl implements SuspensionService {

    @Autowired
    private SuspensionRequestMapper suspensionRequestMapper;
    @Autowired
    private CourseEnrollmentMapper courseEnrollmentMapper;
    @Autowired
    private CourseScheduleMapper courseScheduleMapper;
    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private TeacherMapper teacherMapper;
    @Autowired
    private CourseMapper courseMapper;

    @Override
    @Transactional
    public SuspensionVO createSuspensionRequest(SuspensionApplyDTO request, Long studentUserId) {
        Student student = studentMapper.findByUserId(studentUserId);
        if (student == null) throw new RuntimeException("学生信息不存在");

        CourseEnrollment enrollment = courseEnrollmentMapper.selectById(request.getEnrollmentId());
        if (enrollment == null || Boolean.TRUE.equals(enrollment.getDeleted())) throw new RuntimeException("报名关系不存在");
        if (!Objects.equals(enrollment.getStudentId(), student.getId())) throw new RuntimeException("无权限操作该报名");
        if (!"active".equals(enrollment.getEnrollmentStatus())) throw new RuntimeException("仅进行中的课程可申请停课");

        QueryWrapper<SuspensionRequest> qw = new QueryWrapper<>();
        qw.eq("enrollment_id", enrollment.getId()).eq("status", "pending").eq("is_deleted", false);
        SuspensionRequest existing = suspensionRequestMapper.selectOne(qw);
        if (existing != null) throw new RuntimeException("该报名已有待处理的停课申请");

        SuspensionRequest sr = SuspensionRequest.builder()
                .enrollmentId(enrollment.getId())
                .studentId(student.getId())
                .teacherId(enrollment.getTeacherId())
                .reason(request.getReason())
                .status("pending")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deleted(false)
                .build();
        suspensionRequestMapper.insert(sr);
        return toVO(sr);
    }

    @Override
    @Transactional
    public SuspensionVO adminApproveSuspension(Long requestId, SuspensionApprovalDTO approval, Long adminUserId) {
        SuspensionRequest sr = suspensionRequestMapper.selectById(requestId);
        if (sr == null || Boolean.TRUE.equals(sr.getDeleted())) throw new RuntimeException("停课申请不存在");
        if (!"pending".equals(sr.getStatus())) throw new RuntimeException("该停课申请已被处理");

        CourseEnrollment enrollment = courseEnrollmentMapper.selectById(sr.getEnrollmentId());
        if (enrollment == null) throw new RuntimeException("报名关系不存在");

        sr.setStatus(approval.getStatus());
        sr.setAdminId(adminUserId);
        sr.setAdminNotes(approval.getReviewNotes());
        sr.setReviewedAt(LocalDateTime.now());
        sr.setUpdatedAt(LocalDateTime.now());
        suspensionRequestMapper.updateById(sr);

        if ("approved".equals(approval.getStatus())) {
            // 1) 将报名状态置为 suspended
            UpdateWrapper<CourseEnrollment> uw = new UpdateWrapper<>();
            uw.eq("id", enrollment.getId()).set("enrollment_status", "suspended");
            courseEnrollmentMapper.update(null, uw);

            // 2) 软删该报名下未开始或未取消的课程安排
            List<CourseSchedule> all = courseScheduleMapper.findByBookingRequestId(enrollment.getBookingRequestId());
            LocalDate today = LocalDate.now();
            for (CourseSchedule cs : all) {
                if (cs.getScheduledDate() == null) continue;
                if (!Boolean.TRUE.equals(cs.getDeleted()) && (cs.getScheduledDate().isAfter(today) || "scheduled".equals(cs.getScheduleStatus()))) {
                    UpdateWrapper<CourseSchedule> csUw = new UpdateWrapper<>();
                    csUw.eq("id", cs.getId()).set("is_deleted", true).set("deleted_at", LocalDateTime.now());
                    courseScheduleMapper.update(null, csUw);
                }
            }
        }

        return toVO(sr);
    }

    @Override
    public Page<SuspensionVO> getStudentSuspensionRequests(Long studentUserId, int page, int size, String status) {
        Student student = studentMapper.findByUserId(studentUserId);
        if (student == null) throw new RuntimeException("学生信息不存在");
        List<SuspensionRequest> list = suspensionRequestMapper.findByStudentId(student.getId());
        List<SuspensionRequest> filtered = new ArrayList<>();
        for (SuspensionRequest s : list) {
            if (status == null || status.equals(s.getStatus())) filtered.add(s);
        }
        Page<SuspensionVO> voPage = new Page<>(page, size);
        voPage.setTotal(filtered.size());
        int from = Math.max(0, (page - 1) * size);
        int to = Math.min(filtered.size(), from + size);
        List<SuspensionVO> vos = new ArrayList<>();
        for (int i = from; i < to; i++) vos.add(toVO(filtered.get(i)));
        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    public Page<SuspensionVO> getAllSuspensionRequests(int page, int size, String status) {
        Page<SuspensionVO> voPage = new Page<>(page, size);
        QueryWrapper<SuspensionRequest> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_deleted", false);
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq("status", status);
        }
        queryWrapper.orderByAsc("created_at");
        List<SuspensionRequest> list = suspensionRequestMapper.selectList(queryWrapper);
        voPage.setTotal(list.size());
        int from = Math.max(0, (page - 1) * size);
        int to = Math.min(list.size(), from + size);
        List<SuspensionVO> vos = new ArrayList<>();
        for (int i = from; i < to; i++) vos.add(toVO(list.get(i)));
        voPage.setRecords(vos);
        return voPage;
    }

    private SuspensionVO toVO(SuspensionRequest s) {
        SuspensionVO.SuspensionVOBuilder b = SuspensionVO.builder()
                .id(s.getId())
                .enrollmentId(s.getEnrollmentId())
                .studentId(s.getStudentId())
                .teacherId(s.getTeacherId())
                .reason(s.getReason())
                .status(s.getStatus())
                .statusDisplay(statusDisplay(s.getStatus()))
                .adminId(s.getAdminId())
                .adminNotes(s.getAdminNotes())
                .reviewedAt(s.getReviewedAt())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt());

        try {
            CourseEnrollment ce = courseEnrollmentMapper.selectById(s.getEnrollmentId());
            if (ce != null) {
                Course c = ce.getCourseId() != null ? courseMapper.selectById(ce.getCourseId()) : null;
                if (c != null) {
                    b.courseTitle(c.getTitle());
                }
                Teacher t = teacherMapper.selectById(ce.getTeacherId());
                if (t != null) b.teacherName(t.getRealName());
                Student st = studentMapper.selectById(ce.getStudentId());
                if (st != null) b.studentName(st.getRealName());
            }
        } catch (Exception ignored) {}

        return b.build();
    }

    private String statusDisplay(String status) {
        return switch (status) {
            case "pending" -> "待审批";
            case "approved" -> "已同意";
            case "rejected" -> "已拒绝";
            case "cancelled" -> "已取消";
            default -> status;
        };
    }
}



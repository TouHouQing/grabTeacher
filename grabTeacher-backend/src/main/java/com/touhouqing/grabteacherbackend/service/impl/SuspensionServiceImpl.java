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
    @Autowired
    private BookingRequestMapper bookingRequestMapper;
    @Autowired
    private com.touhouqing.grabteacherbackend.service.StudentService studentService;

    @Override
    @Transactional
    public SuspensionVO createSuspensionRequest(SuspensionApplyDTO request, Long currentUserId) {
        if (request.getEnrollmentId() == null) throw new RuntimeException("缺少报名ID");

        // 识别身份：优先学生，其次教师
        Student student = studentMapper.findByUserId(currentUserId);
        Teacher teacher = null;
        if (student == null) {
            teacher = teacherMapper.findByUserId(currentUserId);
            if (teacher == null) throw new RuntimeException("用户身份不存在，请重新登录");
        }

        CourseEnrollment enrollment = courseEnrollmentMapper.selectById(request.getEnrollmentId());
        if (enrollment == null || Boolean.TRUE.equals(enrollment.getDeleted())) throw new RuntimeException("报名关系不存在");
        if (!"active".equals(enrollment.getEnrollmentStatus())) throw new RuntimeException("仅进行中的课程可申请停课");

        // 仅支持1v1课程
        if (enrollment.getCourseId() != null) {
            Course course = courseMapper.selectById(enrollment.getCourseId());
            if (course != null && course.getCourseType() != null && !"one_on_one".equals(course.getCourseType())) {
                throw new RuntimeException("班课不支持停课，请联系管理员");
            }
        }

        // 试听课无需停课申请（直接在试听课节上操作停课）
        if (Boolean.TRUE.equals(enrollment.getTrial())) {
            throw new RuntimeException("试听课无需停课申请，请直接在试听课节点击停课");
        }

        // 权限校验
        if (student != null) {
            if (!Objects.equals(enrollment.getStudentId(), student.getId())) throw new RuntimeException("无权限操作该报名");
        } else {
            if (!Objects.equals(enrollment.getTeacherId(), teacher.getId())) throw new RuntimeException("无权限操作该报名");
        }

        // 校验区间：至少从一周后开始，且连续覆盖不少于两周（>= 开始+13天）
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new RuntimeException("请提供停课起止日期");
        }
        LocalDate today = LocalDate.now();
        LocalDate minStart = today.plusDays(7);
        if (request.getStartDate().isBefore(minStart)) {
            throw new RuntimeException("停课开始日期需从一周后开始");
        }
        if (request.getEndDate().isBefore(request.getStartDate().plusDays(13))) {
            throw new RuntimeException("停课时长不得少于两周");
        }

        // 同一报名下是否已有待处理申请
        QueryWrapper<SuspensionRequest> qw = new QueryWrapper<>();
        qw.eq("enrollment_id", enrollment.getId()).eq("status", "pending").eq("is_deleted", false);
        SuspensionRequest existing = suspensionRequestMapper.selectOne(qw);
        if (existing != null) throw new RuntimeException("该报名已有待处理的停课申请");

        SuspensionRequest sr = SuspensionRequest.builder()
                .enrollmentId(enrollment.getId())
                .studentId(enrollment.getStudentId())
                .teacherId(enrollment.getTeacherId())
                .reason(request.getReason())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
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
            // 仅软删所选日期区间内的课节；并据此调整报名总课次与退款
            List<CourseSchedule> all = courseScheduleMapper.findByBookingRequestId(enrollment.getBookingRequestId());
            LocalDate today = LocalDate.now();
            LocalDate startSuspendDate = sr.getStartDate() != null ? sr.getStartDate() : today.plusDays(7);
            LocalDate endSuspendDate = sr.getEndDate() != null ? sr.getEndDate() : startSuspendDate.plusDays(13);

            int deletedCount = 0;
            java.math.BigDecimal refundTotal = java.math.BigDecimal.ZERO;

            // 拿到课程单价（每小时）；若无价格则退款为0
            java.math.BigDecimal pricePerHour = null;
            if (enrollment.getCourseId() != null) {
                Course c = courseMapper.selectById(enrollment.getCourseId());
                if (c != null) pricePerHour = c.getPrice();
            }
            // 兜底：一对一未绑定课程时，按教师时薪退款
            if (pricePerHour == null) {
                try {
                    Teacher t = teacherMapper.selectById(enrollment.getTeacherId());
                    if (t != null && t.getHourlyRate() != null) {
                        pricePerHour = t.getHourlyRate();
                    }
                } catch (Exception ignored) {}
            }

            for (CourseSchedule cs : all) {
                if (cs.getScheduledDate() == null) continue;
                LocalDate d = cs.getScheduledDate();
                boolean inRange = (d.isEqual(startSuspendDate) || d.isAfter(startSuspendDate))
                        && (d.isEqual(endSuspendDate) || d.isBefore(endSuspendDate));
                boolean notDeleted = !Boolean.TRUE.equals(cs.getDeleted());
                boolean futureOrScheduled = ("scheduled".equals(cs.getScheduleStatus()) || d.isAfter(today));
                if (notDeleted && futureOrScheduled && inRange) {
                    // 软删课节
                    UpdateWrapper<CourseSchedule> csUw = new UpdateWrapper<>();
                    csUw.eq("id", cs.getId()).set("is_deleted", true).set("deleted_at", LocalDateTime.now());
                    courseScheduleMapper.update(null, csUw);
                    deletedCount++;

                    // 计算本节课退款（按时长*每小时单价），试听课不退款
                    if (pricePerHour != null && (enrollment.getTrial() == null || !enrollment.getTrial())) {
                        if (cs.getStartTime() != null && cs.getEndTime() != null) {
                            java.time.Duration dur = java.time.Duration.between(cs.getStartTime(), cs.getEndTime());
                            java.math.BigDecimal hours = new java.math.BigDecimal(dur.toMinutes())
                                    .divide(new java.math.BigDecimal("60"), 2, java.math.RoundingMode.HALF_UP);
                            refundTotal = refundTotal.add(pricePerHour.multiply(hours));
                        }
                    }
                }
            }

            // 调整报名总课次：扣除被删除的节数
            if (deletedCount > 0) {
                Integer total = enrollment.getTotalSessions() == null ? 0 : enrollment.getTotalSessions();
                int newTotal = Math.max(0, total - deletedCount);
                UpdateWrapper<CourseEnrollment> euw = new UpdateWrapper<>();
                euw.eq("id", enrollment.getId()).set("total_sessions", newTotal);
                courseEnrollmentMapper.update(null, euw);

                // 同步扣减 booking_requests.total_times（若为有限次预约）
                if (enrollment.getBookingRequestId() != null) {
                    try {
                        BookingRequest br = bookingRequestMapper.selectById(enrollment.getBookingRequestId());
                        if (br != null && br.getTotalTimes() != null) {
                            int newBrTotal = Math.max(0, br.getTotalTimes() - deletedCount);
                            UpdateWrapper<BookingRequest> brUw = new UpdateWrapper<>();
                            brUw.eq("id", br.getId()).set("total_times", newBrTotal);
                            bookingRequestMapper.update(null, brUw);
                        }
                    } catch (Exception ex) {
                        log.warn("同步扣减 booking_requests.total_times 失败", ex);
                    }
                }
            }

            // 退款入账并记录交易
            if (refundTotal.compareTo(java.math.BigDecimal.ZERO) > 0) {
                try {
                    Student st = studentMapper.selectById(enrollment.getStudentId());
                    if (st != null && st.getUserId() != null) {
                        boolean ok = studentService.updateStudentBalance(
                                st.getUserId(),
                                refundTotal,
                                "停课删除课节退款（报名ID:" + enrollment.getId() + ")",
                                enrollment.getBookingRequestId(),
                                adminUserId
                        );
                        if (!ok) {
                            log.error("停课退款失败，userId={}, refund={}", st.getUserId(), refundTotal);
                            throw new RuntimeException("停课退款失败");
                        }
                    }
                } catch (Exception ex) {
                    log.error("停课退款异常", ex);
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
    public Page<SuspensionVO> getTeacherSuspensionRequests(Long teacherUserId, int page, int size, String status) {
        Teacher teacher = teacherMapper.findByUserId(teacherUserId);
        if (teacher == null) throw new RuntimeException("教师信息不存在");
        List<SuspensionRequest> list = suspensionRequestMapper.findByTeacherId(teacher.getId());
        List<SuspensionRequest> filtered = new ArrayList<>();
        for (SuspensionRequest s : list) {
            if (status == null || status.equals(s.getStatus())) filtered.add(s);
        }
        Page<SuspensionVO> voPage = new Page<>(page, size);
        voPage.setTotal(filtered.size());
        int from = Math.max(0, (page - 1) * size);
        int to = Math.min(filtered.size(), from + size);
        List<SuspensionVO> records = new ArrayList<>();
        for (int i = from; i < to; i++) {
            records.add(toVO(filtered.get(i)));
        }
        voPage.setRecords(records);
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
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
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



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
import com.touhouqing.grabteacherbackend.service.QuotaService;
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
    private QuotaService quotaService;
    @Autowired
    private BalanceTransactionMapper balanceTransactionMapper;
    @Autowired
    private HourDetailMapper hourDetailMapper;

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
        if (!"active".equals(enrollment.getEnrollmentStatus())) throw new RuntimeException("仅进行中的课程可申请请假");

        // 仅支持1v1课程
        if (enrollment.getCourseId() != null) {
            Course course = courseMapper.selectById(enrollment.getCourseId());
            if (course != null && course.getCourseType() != null && !"one_on_one".equals(course.getCourseType())) {
                throw new RuntimeException("班课不支持请假，请联系管理员");
            }
        }

        // 试听课无需请假申请（直接在试听课节上操作请假）
        if (Boolean.TRUE.equals(enrollment.getTrial())) {
            throw new RuntimeException("试听课无需请假申请，请直接在试听课节点击请假");
        }

        // 权限校验
        if (student != null) {
            if (!Objects.equals(enrollment.getStudentId(), student.getId())) throw new RuntimeException("无权限操作该报名");
        } else {
            if (!Objects.equals(enrollment.getTeacherId(), teacher.getId())) throw new RuntimeException("无权限操作该报名");
        }

        // 校验区间：必须提供起止日期，且开始<=结束；4小时规则（学生/教师均生效）
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new RuntimeException("请提供请假起止日期");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new RuntimeException("结束日期不能早于开始日期");
        }
        // 4小时规则：选定区间内若存在距离当前开始时间 < 4 小时的课节，则不允许请假
        try {
            List<CourseSchedule> allSchedules = courseScheduleMapper.findByBookingRequestId(enrollment.getBookingRequestId());
            LocalDate start = request.getStartDate();
            LocalDate end = request.getEndDate();
            LocalDateTime now = LocalDateTime.now();
            for (CourseSchedule cs : allSchedules) {
                if (cs.getScheduledDate() == null || cs.getStartTime() == null) continue;
                LocalDate d = cs.getScheduledDate();
                boolean inRange = (d.isEqual(start) || d.isAfter(start)) && (d.isEqual(end) || d.isBefore(end));
                boolean notDeleted = !Boolean.TRUE.equals(cs.getDeleted());
                boolean scheduled = "scheduled".equals(cs.getScheduleStatus());
                if (inRange && notDeleted && scheduled) {
                    LocalDateTime startAt = LocalDateTime.of(cs.getScheduledDate(), cs.getStartTime());
                    if (startAt.isAfter(now)) {
                        long hours = java.time.Duration.between(now, startAt).toHours();
                        if (hours < 4) {
                            throw new RuntimeException("选定区间内包含开课前4小时内的课节，无法请假");
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.warn("4小时规则校验失败，降级放行", e);
        }

        // 同一报名下是否已有待处理申请
        QueryWrapper<SuspensionRequest> qw = new QueryWrapper<>();
        qw.eq("enrollment_id", enrollment.getId()).eq("status", "pending").eq("is_deleted", false);
        SuspensionRequest existing = suspensionRequestMapper.selectOne(qw);
        if (existing != null) throw new RuntimeException("该报名已有待处理的请假申请");

        // 计算本次区间内将被取消的课节数量（与审批通过时删除条件保持一致）
        List<CourseSchedule> allForCount = courseScheduleMapper.findByBookingRequestId(enrollment.getBookingRequestId());
        LocalDate today0 = LocalDate.now();
        LocalDate start0 = request.getStartDate();
        LocalDate end0 = request.getEndDate();
        int eligibleCount = 0;
        for (CourseSchedule cs : allForCount) {
            if (cs.getScheduledDate() == null) continue;
            LocalDate d = cs.getScheduledDate();
            boolean inRange = (d.isEqual(start0) || d.isAfter(start0)) && (d.isEqual(end0) || d.isBefore(end0));
            boolean notDeleted = !Boolean.TRUE.equals(cs.getDeleted());
            boolean futureOrScheduled = ("scheduled".equals(cs.getScheduleStatus()) || d.isAfter(today0));
            if (notDeleted && futureOrScheduled && inRange) {
                eligibleCount++;
            }
        }
        if (eligibleCount <= 0) {
            throw new RuntimeException("区间内无可请假的课节");
        }

        // 配额：提交即按课节数计次；学生超额需校验余额充足，否则整体回滚
        String applicantFlag = (student != null) ? "STUDENT" : "TEACHER";
        int overCount = 0;
        try {
            Long actorId = (student != null) ? student.getId() : teacher.getId();
            String role = applicantFlag;
            for (int i = 0; i < eligibleCount; i++) {
                boolean over = quotaService.consumeOnApply(role, actorId, enrollment.getId());
                if (over && student != null) overCount++;
            }
            if (student != null && overCount > 0) {
                Course course = enrollment.getCourseId() != null ? courseMapper.selectById(enrollment.getCourseId()) : null;
                if (course == null || course.getPrice() == null) {
                    // 回滚已消耗的次数
                    for (int i = 0; i < eligibleCount; i++) {
                        quotaService.rollbackOnReject(role, actorId, enrollment.getId());
                    }
                    throw new RuntimeException("课程价格缺失，无法计算超额请假扣费");
                }
                java.math.BigDecimal halfHourBeans = course.getPrice().multiply(new java.math.BigDecimal("0.5"));
                java.math.BigDecimal need = halfHourBeans.multiply(new java.math.BigDecimal(overCount));
                Student st = studentMapper.selectById(enrollment.getStudentId());
                java.math.BigDecimal bal = (st != null && st.getBalance() != null) ? st.getBalance() : java.math.BigDecimal.ZERO;
                if (bal.compareTo(need) < 0) {
                    for (int i = 0; i < eligibleCount; i++) {
                        quotaService.rollbackOnReject(role, actorId, enrollment.getId());
                    }
                    throw new RuntimeException("学生余额不足，无法进行超额请假");
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.warn("配额消耗失败，降级放行", e);
        }

        SuspensionRequest sr = SuspensionRequest.builder()
                .enrollmentId(enrollment.getId())
                .studentId(enrollment.getStudentId())
                .teacherId(enrollment.getTeacherId())
                .reason(request.getReason())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status("pending")
                .adminNotes("[applicant=" + applicantFlag + "][applyCount=" + eligibleCount + "][overCount=" + overCount + "]")
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
        if (sr == null || Boolean.TRUE.equals(sr.getDeleted())) throw new RuntimeException("请假申请不存在");
        if (!"pending".equals(sr.getStatus())) throw new RuntimeException("该请假申请已被处理");

        CourseEnrollment enrollment = courseEnrollmentMapper.selectById(sr.getEnrollmentId());
        if (enrollment == null) throw new RuntimeException("报名关系不存在");

        sr.setStatus(approval.getStatus());
        sr.setAdminId(adminUserId);
        String existingNotes = sr.getAdminNotes();
        String reviewNotes = approval.getReviewNotes();
        sr.setAdminNotes((existingNotes != null ? existingNotes : "") + (reviewNotes != null && !reviewNotes.isEmpty() ? (" " + reviewNotes) : ""));
        sr.setReviewedAt(LocalDateTime.now());
        sr.setUpdatedAt(LocalDateTime.now());
        suspensionRequestMapper.updateById(sr);

        // 管理员拒绝：回滚本次配额消耗
        if ("rejected".equals(approval.getStatus())) {
            String notes = sr.getAdminNotes() == null ? "" : sr.getAdminNotes();
            String applicant = notes.contains("TEACHER") ? "TEACHER" : "STUDENT";
            int applyCnt = 1;
            int idxApply = notes.indexOf("applyCount=");
            if (idxApply >= 0) {
                int endIdx = notes.indexOf(']', idxApply);
                String numStr = (endIdx > idxApply) ? notes.substring(idxApply + 11, endIdx) : notes.substring(idxApply + 11);
                try { applyCnt = Integer.parseInt(numStr.trim()); } catch (Exception ignore) {}
            }
            try {
                Long actorId = "STUDENT".equals(applicant) ? enrollment.getStudentId() : enrollment.getTeacherId();
                String role = applicant;
                for (int i = 0; i < applyCnt; i++) {
                    quotaService.rollbackOnReject(role, actorId, enrollment.getId());
                }
            } catch (Exception e) {
                log.warn("请假配额回滚失败，requestId={}", requestId, e);
            }
            return toVO(sr);
        }

        if ("approved".equals(approval.getStatus())) {
            // 仅软删所选日期区间内的课节；并据此调整报名总课次与退款
            List<CourseSchedule> all = courseScheduleMapper.findByBookingRequestId(enrollment.getBookingRequestId());
            LocalDate today = LocalDate.now();
            LocalDate startSuspendDate = sr.getStartDate() != null ? sr.getStartDate() : today.plusDays(7);
            LocalDate endSuspendDate = sr.getEndDate() != null ? sr.getEndDate() : startSuspendDate.plusDays(13);

            int deletedCount = 0;
            java.math.BigDecimal refundTotal = java.math.BigDecimal.ZERO;

                // 超额扣费/补偿结算（审批通过后）：按 overCount 次累积结算
                try {
                    Course courseForPrice = enrollment.getCourseId() != null ? courseMapper.selectById(enrollment.getCourseId()) : null;
                    java.math.BigDecimal price = (courseForPrice != null) ? courseForPrice.getPrice() : null;
                    if (price != null) {
                        String notes = sr.getAdminNotes() == null ? "" : sr.getAdminNotes();
                        String applicant = notes.contains("TEACHER") ? "TEACHER" : "STUDENT";
                        int overCnt = 0;
                        int idx = notes.indexOf("overCount=");
                        if (idx >= 0) {
                            int endIdx = notes.indexOf(']', idx);
                            String numStr = (endIdx > idx) ? notes.substring(idx + 10, endIdx) : notes.substring(idx + 10);
                            try { overCnt = Integer.parseInt(numStr.trim()); } catch (Exception ignore) {}
                        }
                        if (overCnt > 0) {
                            java.math.BigDecimal halfHourBeans = price.multiply(new java.math.BigDecimal("0.5"));
                            java.math.BigDecimal totalBeans = halfHourBeans.multiply(new java.math.BigDecimal(overCnt));
                            java.math.BigDecimal totalHours = new java.math.BigDecimal(overCnt);
                            if ("STUDENT".equals(applicant)) {
                                // 学生超额：扣学生 M豆=0.5h*overCnt；教师 +overCnt 小时
                                Student st = studentMapper.selectById(enrollment.getStudentId());
                                if (st != null) {
                                    java.math.BigDecimal before = st.getBalance() == null ? java.math.BigDecimal.ZERO : st.getBalance();
                                    java.math.BigDecimal after = before.subtract(totalBeans);
                                    studentMapper.incrementBalance(st.getId(), totalBeans.negate());
                                    BalanceTransaction tx = BalanceTransaction.builder()
                                            .userId(st.getUserId())
                                            .name(st.getRealName())
                                            .amount(totalBeans.negate())
                                            .balanceBefore(before)
                                            .balanceAfter(after)
                                            .transactionType("DEDUCT")
                                            .reason("学生超额请假扣除0.5小时M豆×" + overCnt)
                                            .bookingId(enrollment.getBookingRequestId())
                                            .createdAt(LocalDateTime.now())
                                            .build();
                                    balanceTransactionMapper.insert(tx);
                                }
                                teacherMapper.incrementCurrentHours(enrollment.getTeacherId(), totalHours);
                                Teacher tch = teacherMapper.selectById(enrollment.getTeacherId());
                                if (tch != null) {
                                    HourDetail hd = HourDetail.builder()
                                            .userId(tch.getUserId())
                                            .name(tch.getRealName())
                                            .hours(totalHours)
                                            .transactionType(1)
                                            .reason("学生超额请假补偿×" + overCnt)
                                            .bookingId(enrollment.getBookingRequestId())
                                            .createdAt(LocalDateTime.now())
                                            .build();
                                    hourDetailMapper.insert(hd);
                                }
                            } else { // TEACHER
                                // 教师超额：学生 +0.5h*overCnt M豆；教师 -overCnt 小时
                                teacherMapper.decrementCurrentHours(enrollment.getTeacherId(), totalHours);
                                Teacher tch = teacherMapper.selectById(enrollment.getTeacherId());
                                if (tch != null) {
                                    HourDetail hd = HourDetail.builder()
                                            .userId(tch.getUserId())
                                            .name(tch.getRealName())
                                            .hours(totalHours.negate())
                                            .transactionType(0)
                                            .reason("教师超额请假扣减×" + overCnt)
                                            .bookingId(enrollment.getBookingRequestId())
                                            .createdAt(LocalDateTime.now())
                                            .build();
                                    hourDetailMapper.insert(hd);
                                }
                                Student st = studentMapper.selectById(enrollment.getStudentId());
                                if (st != null) {
                                    java.math.BigDecimal before = st.getBalance() == null ? java.math.BigDecimal.ZERO : st.getBalance();
                                    java.math.BigDecimal after = before.add(totalBeans);
                                    studentMapper.incrementBalance(st.getId(), totalBeans);
                                    BalanceTransaction tx = BalanceTransaction.builder()
                                            .userId(st.getUserId())
                                            .name(st.getRealName())
                                            .amount(totalBeans)
                                            .balanceBefore(before)
                                            .balanceAfter(after)
                                            .transactionType("REFUND")
                                            .reason("教师超额请假补偿0.5小时M豆×" + overCnt)
                                            .bookingId(enrollment.getBookingRequestId())
                                            .createdAt(LocalDateTime.now())
                                            .build();
                                    balanceTransactionMapper.insert(tx);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("请假超额扣费/补偿结算失败，requestId={}", requestId, e);
                }


            // 拿到课程单价（每小时）；若无价格则退款为0
            java.math.BigDecimal pricePerHour = null;
            if (enrollment.getCourseId() != null) {
                Course c = courseMapper.selectById(enrollment.getCourseId());
                if (c != null) pricePerHour = c.getPrice();
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
                                "请假删除课节退款（报名ID:" + enrollment.getId() + ")",
                                enrollment.getBookingRequestId(),
                                adminUserId
                        );
                        if (!ok) {
                            log.error("请假退款失败，userId={}, refund={}", st.getUserId(), refundTotal);


                            throw new RuntimeException("请假退款失败");
                        }
                    }
                } catch (Exception ex) {
                    log.error("请假退款异常", ex);
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



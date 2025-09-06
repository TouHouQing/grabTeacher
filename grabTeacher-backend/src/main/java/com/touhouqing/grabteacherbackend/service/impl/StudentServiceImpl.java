package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.touhouqing.grabteacherbackend.model.dto.StudentInfoDTO;
import com.touhouqing.grabteacherbackend.model.entity.*;
import com.touhouqing.grabteacherbackend.model.vo.StudentProfileVO;
import com.touhouqing.grabteacherbackend.mapper.StudentMapper;
import com.touhouqing.grabteacherbackend.mapper.StudentSubjectMapper;
import com.touhouqing.grabteacherbackend.mapper.BookingRequestMapper;
import com.touhouqing.grabteacherbackend.mapper.UserMapper;
import com.touhouqing.grabteacherbackend.service.StudentService;
import com.touhouqing.grabteacherbackend.mapper.BalanceTransactionMapper;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.touhouqing.grabteacherbackend.util.AliyunOssUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.touhouqing.grabteacherbackend.security.UserPrincipal;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentMapper studentMapper;
    private final StudentSubjectMapper studentSubjectMapper;
    private final BookingRequestMapper bookingRequestMapper;
    private final com.touhouqing.grabteacherbackend.mapper.CourseScheduleMapper courseScheduleMapper;
    private final UserMapper userMapper;
    private final BalanceTransactionMapper balanceTransactionMapper;

    private final AliyunOssUtil ossUtil;

    /**
     * 获取当前认证的管理员用户ID
     */
    private Long getCurrentOperatorId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
                UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
                return userPrincipal.getId();
            }
        } catch (Exception e) {
            log.warn("获取当前操作员ID失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 根据用户ID获取学生信息
     */
    @Override
    public Student getStudentByUserId(Long userId) {
        QueryWrapper<Student> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("is_deleted", false);
        return studentMapper.selectOne(queryWrapper);
    }

    /**
     * 根据用户ID获取学生个人信息（包含出生年月）
     */
    @Override
    public StudentProfileVO getStudentProfileByUserId(Long userId) {
        Student student = getStudentByUserId(userId);
        if (student == null) {
            return null;
        }

        // 获取用户的出生年月和头像
        User user = userMapper.selectById(userId);
        String birthDate = user != null ? user.getBirthDate() : null;
        String avatarUrl = user != null ? user.getAvatarUrl() : null;

        // 获取学生感兴趣的科目ID列表
        List<Long> subjectIds = studentSubjectMapper.getSubjectIdsByStudentId(student.getId());

        return StudentProfileVO.builder()
                .id(student.getId())
                .userId(student.getUserId())
                .realName(student.getRealName())
                .birthDate(birthDate)
                .subjectsInterested(student.getSubjectsInterested())
                .subjectIds(subjectIds)
                .learningGoals(student.getLearningGoals())
                .preferredTeachingStyle(student.getPreferredTeachingStyle())
                .budgetRange(student.getBudgetRange())
                .gender(student.getGender())
                .avatarUrl(avatarUrl)
                .balance(student.getBalance())
                .adjustmentTimes(user != null ? user.getAdjustmentTimes() : null)
                .deleted(student.getDeleted())
                .deletedAt(student.getDeletedAt())
                .build();
    }

    /**
     * 更新学生信息
     */
    @Override
    @Transactional
    public Student updateStudentInfo(Long userId, StudentInfoDTO request) {
        Student student = getStudentByUserId(userId);
        if (student == null) {
            throw new RuntimeException("学生信息不存在");
        }

        // 更新学生信息
        if (request.getRealName() != null) {
            student.setRealName(request.getRealName());
        }

        // 更新用户表中的出生年月/头像
        if (request.getBirthDate() != null || request.getAvatarUrl() != null) {
            User user = userMapper.selectById(userId);
            if (user != null) {
                String oldAvatar = user.getAvatarUrl();
                if (request.getBirthDate() != null) {
                    user.setBirthDate(request.getBirthDate());
                }
                if (request.getAvatarUrl() != null && !request.getAvatarUrl().isEmpty()) {
                    user.setAvatarUrl(request.getAvatarUrl());
                }
                userMapper.updateById(user);
                // 删除旧头像
                if (request.getAvatarUrl() != null && oldAvatar != null && !oldAvatar.isEmpty() && !oldAvatar.equals(request.getAvatarUrl())) {
                    ossUtil.deleteByUrl(oldAvatar);
                }
            }
        }

        if (request.getSubjectsInterested() != null) {
            student.setSubjectsInterested(request.getSubjectsInterested());
        }
        if (request.getLearningGoals() != null) {
            student.setLearningGoals(request.getLearningGoals());
        }
        if (request.getPreferredTeachingStyle() != null) {
            student.setPreferredTeachingStyle(request.getPreferredTeachingStyle());
        }
        if (request.getBudgetRange() != null) {
            student.setBudgetRange(request.getBudgetRange());
        }
        if (request.getGender() != null) {
            student.setGender(request.getGender());
        }

        studentMapper.updateById(student);

        // 更新学生感兴趣的科目关联
        if (request.getSubjectIds() != null) {
            // 先删除现有的科目关联
            studentSubjectMapper.deleteByStudentId(student.getId());

            // 添加新的科目关联
            if (!request.getSubjectIds().isEmpty()) {
                for (Long subjectId : request.getSubjectIds()) {
                    StudentSubject studentSubject = new StudentSubject(student.getId(), subjectId);
                    studentSubjectMapper.insert(studentSubject);
                }
            }
        }

        log.info("更新学生信息成功: userId={}", userId);

        return student;
    }

    /**
     * 获取学生控制台统计数据
     */
    @Override
    public Map<String, Object> getStudentStatistics(Long userId) {
        Map<String, Object> statistics = new HashMap<>();

        // 获取学生信息
        Student student = getStudentByUserId(userId);
        if (student == null) {
            throw new RuntimeException("学生信息不存在");
        }

        // 1. 试听次数 - 从User表获取trialTimes字段
        User user = userMapper.selectById(userId);
        Integer trialTimes = user != null ? user.getTrialTimes() : 0;
        Long remainingTrialTimes = trialTimes != null ? trialTimes.longValue() : 0L;

        // 2. 待审批预约数 - 查询状态为pending的预约申请
        QueryWrapper<BookingRequest> bookingWrapper = new QueryWrapper<>();
        bookingWrapper.eq("student_id", student.getId());
        bookingWrapper.eq("status", "pending");
        bookingWrapper.eq("is_deleted", false);
        Long pendingBookings = bookingRequestMapper.selectCount(bookingWrapper);

        // 3. 完成课程数 - 查询course_schedules + join enrollments（使用 Mapper 进行统计）
        QueryWrapper<CourseSchedule> completedQw =
                new QueryWrapper<>();
        completedQw.apply(
                "exists (select 1 from course_enrollments ce where ce.id = course_schedules.enrollment_id and ce.student_id = {0} and ce.is_deleted = 0)",
                student.getId()
        );
        completedQw.eq("schedule_status", "completed");
        completedQw.eq("is_deleted", 0);
        Long completedCourses = courseScheduleMapper.selectCount(completedQw);

        statistics.put("remainingTrialTimes", remainingTrialTimes);
        statistics.put("pendingBookings", pendingBookings != null ? pendingBookings.intValue() : 0);
        statistics.put("completedCourses", completedCourses != null ? completedCourses.intValue() : 0);

        log.info("获取学生统计数据成功: userId={}, statistics={}", userId, statistics);

        return statistics;
    }

    /**
     * 管理员更新学生信息（包括余额）
     */
    @Override
    @Transactional
    public Student updateStudentInfoByAdmin(Long userId, StudentInfoDTO request) {
        Student student = getStudentByUserId(userId);
        if (student == null) {
            throw new RuntimeException("学生信息不存在");
        }

        BigDecimal oldBalance = student.getBalance();

        // 更新学生信息
        if (request.getRealName() != null) {
            student.setRealName(request.getRealName());
        }

        // 更新用户表中的出生年月/头像
        if (request.getBirthDate() != null || request.getAvatarUrl() != null) {
            User user = userMapper.selectById(userId);
            if (user != null) {
                String oldAvatar = user.getAvatarUrl();
                if (request.getBirthDate() != null) {
                    user.setBirthDate(request.getBirthDate());
                }
                if (request.getAvatarUrl() != null && !request.getAvatarUrl().isEmpty()) {
                    user.setAvatarUrl(request.getAvatarUrl());
                }
                userMapper.updateById(user);
                // 删除旧头像
                if (request.getAvatarUrl() != null && oldAvatar != null && !oldAvatar.isEmpty() && !oldAvatar.equals(request.getAvatarUrl())) {
                    ossUtil.deleteByUrl(oldAvatar);
                }
            }
        }

        if (request.getSubjectsInterested() != null) {
            student.setSubjectsInterested(request.getSubjectsInterested());
        }
        if (request.getLearningGoals() != null) {
            student.setLearningGoals(request.getLearningGoals());
        }
        if (request.getPreferredTeachingStyle() != null) {
            student.setPreferredTeachingStyle(request.getPreferredTeachingStyle());
        }
        if (request.getBudgetRange() != null) {
            student.setBudgetRange(request.getBudgetRange());
        }
        if (request.getGender() != null) {
            student.setGender(request.getGender());
        }

        // 管理员可以更新余额
        if (request.getBalance() != null && !request.getBalance().equals(oldBalance)) {
            student.setBalance(request.getBalance());
            
            // 记录余额变动
            BigDecimal amountChange = request.getBalance().subtract(oldBalance);
            BalanceTransaction transaction = BalanceTransaction.builder()
                    .userId(userId)
                    .name(student.getRealName()) // 学生姓名
                    .amount(amountChange)
                    .balanceBefore(oldBalance)
                    .balanceAfter(request.getBalance())
                    .transactionType(amountChange.compareTo(BigDecimal.ZERO) > 0 ? "RECHARGE" : "DEDUCT")
                    .reason("管理员调整余额")
                    .operatorId(getCurrentOperatorId()) // 从当前认证信息中获取管理员ID
                    .createdAt(LocalDateTime.now())
                    .build();
            balanceTransactionMapper.insert(transaction);
        }

        studentMapper.updateById(student);

        // 更新学生感兴趣的科目关联
        if (request.getSubjectIds() != null) {
            // 先删除现有的科目关联
            studentSubjectMapper.deleteByStudentId(student.getId());

            // 添加新的科目关联
            for (Long subjectId : request.getSubjectIds()) {
                StudentSubject studentSubject = new StudentSubject();
                studentSubject.setStudentId(student.getId());
                studentSubject.setSubjectId(subjectId);
                studentSubjectMapper.insert(studentSubject);
            }
        }

        return student;
    }

    /**
     * 更新学生余额（用于预约扣费和退费）
     */
    @Override
    @Transactional
    public boolean updateStudentBalance(Long userId, BigDecimal amount, String reason) {
        return updateStudentBalance(userId, amount, reason, null, null);
    }

    @Override
    @Transactional
    public boolean updateStudentBalance(Long userId, BigDecimal amount, String reason, Long bookingId, Long operatorId) {
        try {
            Student student = getStudentByUserId(userId);
            if (student == null) {
                log.error("学生信息不存在，userId: {}", userId);
                return false;
            }

            BigDecimal oldBalance = student.getBalance();
            BigDecimal newBalance = oldBalance.add(amount);

            // 检查余额不能为负数
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                log.error("余额不足，用户ID: {}, 当前余额: {}, 扣除金额: {}", userId, oldBalance, amount.abs());
                return false;
            }

            student.setBalance(newBalance);
            studentMapper.updateById(student);

            // 记录余额变动
            String transactionType;
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                transactionType = "REFUND";
            } else {
                transactionType = "DEDUCT";
            }

            BalanceTransaction transaction = BalanceTransaction.builder()
                    .userId(userId)
                    .name(student.getRealName())
                    .amount(amount)
                    .balanceBefore(oldBalance)
                    .balanceAfter(newBalance)
                    .transactionType(transactionType)
                    .reason(reason)
                    .bookingId(bookingId)
                    .operatorId(operatorId)
                    .createdAt(LocalDateTime.now())
                    .build();
            balanceTransactionMapper.insert(transaction);

            log.info("学生余额更新成功，用户ID: {}, 变动金额: {}, 余额: {} -> {}, 预约ID: {}, 操作员ID: {}", 
                    userId, amount, oldBalance, newBalance, bookingId, operatorId);
            return true;

        } catch (Exception e) {
            log.error("更新学生余额失败，用户ID: {}, 变动金额: {}, 原因: {}, 预约ID: {}, 操作员ID: {}", 
                    userId, amount, reason, bookingId, operatorId, e);
            return false;
        }
    }

    /**
     * 检查学生余额是否足够
     */
    @Override
    public boolean checkBalance(Long userId, BigDecimal amount) {
        try {
            Student student = getStudentByUserId(userId);
            if (student == null) {
                log.error("学生信息不存在，userId: {}", userId);
                return false;
            }

            return student.getBalance().compareTo(amount) >= 0;
        } catch (Exception e) {
            log.error("检查学生余额失败，用户ID: {}, 需要金额: {}", userId, amount, e);
            return false;
        }
    }
}
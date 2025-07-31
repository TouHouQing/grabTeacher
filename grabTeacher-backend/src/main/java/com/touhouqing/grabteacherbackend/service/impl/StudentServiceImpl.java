package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.touhouqing.grabteacherbackend.dto.StudentInfoRequest;
import com.touhouqing.grabteacherbackend.dto.StudentProfileResponse;
import com.touhouqing.grabteacherbackend.entity.Student;
import com.touhouqing.grabteacherbackend.entity.StudentSubject;
import com.touhouqing.grabteacherbackend.entity.BookingRequest;
import com.touhouqing.grabteacherbackend.entity.Schedule;
import com.touhouqing.grabteacherbackend.mapper.StudentMapper;
import com.touhouqing.grabteacherbackend.mapper.StudentSubjectMapper;
import com.touhouqing.grabteacherbackend.mapper.BookingRequestMapper;
import com.touhouqing.grabteacherbackend.mapper.ScheduleMapper;
import com.touhouqing.grabteacherbackend.mapper.UserMapper;
import com.touhouqing.grabteacherbackend.entity.User;
import com.touhouqing.grabteacherbackend.service.StudentService;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentMapper studentMapper;
    private final StudentSubjectMapper studentSubjectMapper;
    private final BookingRequestMapper bookingRequestMapper;
    private final ScheduleMapper scheduleMapper;
    private final UserMapper userMapper;

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
    public StudentProfileResponse getStudentProfileByUserId(Long userId) {
        Student student = getStudentByUserId(userId);
        if (student == null) {
            return null;
        }

        // 获取用户的出生年月
        User user = userMapper.selectById(userId);
        String birthDate = user != null ? user.getBirthDate() : null;

        // 获取学生感兴趣的科目ID列表
        List<Long> subjectIds = studentSubjectMapper.getSubjectIdsByStudentId(student.getId());

        return StudentProfileResponse.builder()
                .id(student.getId())
                .userId(student.getUserId())
                .realName(student.getRealName())
                .birthDate(birthDate)
                .gradeLevel(student.getGradeLevel())
                .subjectsInterested(student.getSubjectsInterested())
                .subjectIds(subjectIds)
                .learningGoals(student.getLearningGoals())
                .preferredTeachingStyle(student.getPreferredTeachingStyle())
                .budgetRange(student.getBudgetRange())
                .gender(student.getGender())
                .isDeleted(student.getIsDeleted())
                .deletedAt(student.getDeletedAt())
                .build();
    }

    /**
     * 更新学生信息
     */
    @Override
    @Transactional
    public Student updateStudentInfo(Long userId, StudentInfoRequest request) {
        Student student = getStudentByUserId(userId);
        if (student == null) {
            throw new RuntimeException("学生信息不存在");
        }

        // 更新学生信息
        if (request.getRealName() != null) {
            student.setRealName(request.getRealName());
        }

        // 更新用户表中的出生年月
        if (request.getBirthDate() != null) {
            User user = userMapper.selectById(userId);
            if (user != null) {
                user.setBirthDate(request.getBirthDate());
                userMapper.updateById(user);
            }
        }

        if (request.getGradeLevel() != null) {
            student.setGradeLevel(request.getGradeLevel());
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

        // 1. 进行中的课程数 - 按预约申请ID去重统计，而不是按课时数统计
        // 查询状态为progressing的所有课程安排
        List<Schedule> progressingSchedules = scheduleMapper.selectList(
            new QueryWrapper<Schedule>()
                .eq("student_id", student.getId())
                .eq("status", "progressing")
                .eq("is_deleted", false)
        );

        // 按预约申请ID去重统计（同一个预约申请的多个课时算作一个课程）
        log.info("查询到的进行中课程安排数量: {}", progressingSchedules.size());

        Set<Long> uniqueBookingRequestIds = progressingSchedules.stream()
            .map(Schedule::getBookingRequestId)
            .filter(bookingRequestId -> bookingRequestId != null)
            .collect(java.util.stream.Collectors.toSet());

        log.info("去重后的预约申请ID集合: {}", uniqueBookingRequestIds);

        Long progressingCourses = (long) uniqueBookingRequestIds.size();

        // 2. 待审批预约数 - 查询状态为pending的预约申请
        QueryWrapper<BookingRequest> bookingWrapper = new QueryWrapper<>();
        bookingWrapper.eq("student_id", student.getId());
        bookingWrapper.eq("status", "pending");
        bookingWrapper.eq("is_deleted", false);
        Long pendingBookings = bookingRequestMapper.selectCount(bookingWrapper);

        // 3. 完成课程数 - 查询状态为completed的课程安排
        QueryWrapper<Schedule> completedWrapper = new QueryWrapper<>();
        completedWrapper.eq("student_id", student.getId());
        completedWrapper.eq("status", "completed");
        completedWrapper.eq("is_deleted", false);
        Long completedCourses = scheduleMapper.selectCount(completedWrapper);

        statistics.put("progressingCourses", progressingCourses != null ? progressingCourses.intValue() : 0);
        statistics.put("pendingBookings", pendingBookings != null ? pendingBookings.intValue() : 0);
        statistics.put("completedCourses", completedCourses != null ? completedCourses.intValue() : 0);

        log.info("获取学生统计数据成功: userId={}, statistics={}", userId, statistics);

        return statistics;
    }
}
package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.entity.BookingRequest;
import com.touhouqing.grabteacherbackend.entity.Course;
import com.touhouqing.grabteacherbackend.entity.Student;
import com.touhouqing.grabteacherbackend.entity.StudentSubject;
import com.touhouqing.grabteacherbackend.entity.Teacher;
import com.touhouqing.grabteacherbackend.entity.TeacherSubject;
import com.touhouqing.grabteacherbackend.entity.User;
import com.touhouqing.grabteacherbackend.dto.StudentInfoRequest;
import com.touhouqing.grabteacherbackend.dto.TeacherInfoRequest;
import com.touhouqing.grabteacherbackend.util.TimeSlotUtil;
import com.touhouqing.grabteacherbackend.mapper.BookingRequestMapper;
import com.touhouqing.grabteacherbackend.mapper.CourseMapper;
import com.touhouqing.grabteacherbackend.mapper.StudentMapper;
import com.touhouqing.grabteacherbackend.mapper.StudentSubjectMapper;
import com.touhouqing.grabteacherbackend.mapper.TeacherMapper;
import com.touhouqing.grabteacherbackend.mapper.TeacherSubjectMapper;
import com.touhouqing.grabteacherbackend.mapper.UserMapper;
import com.touhouqing.grabteacherbackend.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserMapper userMapper;
    private final StudentMapper studentMapper;
    private final StudentSubjectMapper studentSubjectMapper;
    private final TeacherMapper teacherMapper;
    private final TeacherSubjectMapper teacherSubjectMapper;
    private final BookingRequestMapper bookingRequestMapper;
    private final CourseMapper courseMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 获取系统统计信息
     */
    @Override
    public Map<String, Object> getSystemStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        // 用户统计
        QueryWrapper<User> userWrapper = new QueryWrapper<>();
        userWrapper.eq("is_deleted", false);
        Long totalUsers = userMapper.selectCount(userWrapper);

        QueryWrapper<User> studentUserWrapper = new QueryWrapper<>();
        studentUserWrapper.eq("is_deleted", false);
        studentUserWrapper.eq("user_type", "student");
        Long totalStudents = userMapper.selectCount(studentUserWrapper);

        QueryWrapper<User> teacherUserWrapper = new QueryWrapper<>();
        teacherUserWrapper.eq("is_deleted", false);
        teacherUserWrapper.eq("user_type", "teacher");
        Long totalTeachers = userMapper.selectCount(teacherUserWrapper);

        // 认证教师统计
        QueryWrapper<Teacher> verifiedTeacherWrapper = new QueryWrapper<>();
        verifiedTeacherWrapper.eq("is_deleted", false);
        verifiedTeacherWrapper.eq("is_verified", true);
        Long verifiedTeachers = teacherMapper.selectCount(verifiedTeacherWrapper);

        // 预约待审批统计
        QueryWrapper<BookingRequest> pendingBookingWrapper = new QueryWrapper<>();
        pendingBookingWrapper.eq("status", "pending");
        pendingBookingWrapper.eq("is_deleted", false);
        Long pendingBookings = bookingRequestMapper.selectCount(pendingBookingWrapper);

        // 课程待审批统计
        QueryWrapper<Course> pendingCourseWrapper = new QueryWrapper<>();
        pendingCourseWrapper.eq("status", "pending");
        pendingCourseWrapper.eq("is_deleted", false);
        Long pendingCourses = courseMapper.selectCount(pendingCourseWrapper);

        // 教师待认证统计
        QueryWrapper<Teacher> unverifiedTeacherWrapper = new QueryWrapper<>();
        unverifiedTeacherWrapper.eq("is_deleted", false);
        unverifiedTeacherWrapper.eq("is_verified", false);
        Long unverifiedTeachers = teacherMapper.selectCount(unverifiedTeacherWrapper);

        statistics.put("totalUsers", totalUsers);
        statistics.put("totalStudents", totalStudents);
        statistics.put("totalTeachers", totalTeachers);
        statistics.put("verifiedTeachers", verifiedTeachers);
        statistics.put("pendingBookings", pendingBookings);
        statistics.put("pendingCourses", pendingCourses);
        statistics.put("unverifiedTeachers", unverifiedTeachers);

        return statistics;
    }

    /**
     * 获取用户列表
     */
    @Override
    public List<User> getUserList(int page, int size, String userType, String keyword) {
        Page<User> pageParam = new Page<>(page, size);
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("is_deleted", false);

        if (StringUtils.hasText(userType)) {
            queryWrapper.eq("user_type", userType);
        }

        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like("username", keyword)
                    .or()
                    .like("email", keyword)
            );
        }

        queryWrapper.orderByDesc("created_at");

        Page<User> result = userMapper.selectPage(pageParam, queryWrapper);
        return result.getRecords();
    }

    /**
     * 更新用户状态
     */
    @Override
    @Transactional
    public void updateUserStatus(Long userId, String status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (!Arrays.asList("active", "inactive", "banned").contains(status)) {
            throw new RuntimeException("无效的状态值");
        }

        user.setStatus(status);
        userMapper.updateById(user);

        log.info("更新用户状态成功: userId={}, status={}", userId, status);
    }

    // ===================== 学生管理实现 =====================

    @Override
    public Page<Student> getStudentList(int page, int size, String keyword, String gradeLevel) {
        Page<Student> pageParam = new Page<>(page, size);
        QueryWrapper<Student> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("is_deleted", false); // 添加软删除条件
        
        // 搜索条件
        if (StringUtils.hasText(keyword)) {
            queryWrapper.like("real_name", keyword);
        }
        if (StringUtils.hasText(gradeLevel)) {
            queryWrapper.eq("grade_level", gradeLevel);
        }
        
        // 排序
        queryWrapper.orderByDesc("id");
        
        return studentMapper.selectPage(pageParam, queryWrapper);
    }

    @Override
    public Student getStudentById(Long studentId) {
        QueryWrapper<Student> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", studentId);
        queryWrapper.eq("is_deleted", false);
        return studentMapper.selectOne(queryWrapper);
    }

    @Override
    @Transactional
    public Student addStudent(StudentInfoRequest request) {
        // 只创建学生信息，不创建用户（管理员直接创建学生档案）
        Student student = Student.builder()
                .userId(null) // 管理员创建的学生可能没有关联用户
                .realName(request.getRealName())
                .gradeLevel(request.getGradeLevel())
                .subjectsInterested(request.getSubjectsInterested())
                .learningGoals(request.getLearningGoals())
                .preferredTeachingStyle(request.getPreferredTeachingStyle())
                .budgetRange(request.getBudgetRange())
                .gender(request.getGender() != null ? request.getGender() : "不愿透露")
                .build();

        studentMapper.insert(student);

        // 处理学生感兴趣的科目关联
        if (request.getSubjectIds() != null && !request.getSubjectIds().isEmpty()) {
            for (Long subjectId : request.getSubjectIds()) {
                StudentSubject studentSubject = new StudentSubject(student.getId(), subjectId);
                studentSubjectMapper.insert(studentSubject);
            }
        }

        return student;
    }

    @Override
    @Transactional
    public Student updateStudent(Long studentId, StudentInfoRequest request) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new RuntimeException("学生不存在");
        }

        // 更新学生信息
        student.setRealName(request.getRealName());
        student.setGradeLevel(request.getGradeLevel());
        student.setSubjectsInterested(request.getSubjectsInterested());
        student.setLearningGoals(request.getLearningGoals());
        student.setPreferredTeachingStyle(request.getPreferredTeachingStyle());
        student.setBudgetRange(request.getBudgetRange());
        student.setGender(request.getGender() != null ? request.getGender() : "不愿透露");

        studentMapper.updateById(student);

        // 更新学生感兴趣的科目关联
        if (request.getSubjectIds() != null) {
            // 先删除现有的科目关联
            studentSubjectMapper.deleteByStudentId(studentId);

            // 添加新的科目关联
            if (!request.getSubjectIds().isEmpty()) {
                for (Long subjectId : request.getSubjectIds()) {
                    StudentSubject studentSubject = new StudentSubject(studentId, subjectId);
                    studentSubjectMapper.insert(studentSubject);
                }
            }
        }

        return student;
    }

    @Override
    @Transactional
    public void deleteStudent(Long studentId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new RuntimeException("学生不存在");
        }

        // 逻辑删除学生
        student.setIsDeleted(true);
        student.setDeletedAt(LocalDateTime.now());
        studentMapper.updateById(student);

        // 如果有关联用户，同时逻辑删除对应的用户
        if (student.getUserId() != null) {
            User user = userMapper.selectById(student.getUserId());
            if (user != null) {
                user.setIsDeleted(true);
                user.setDeletedAt(LocalDateTime.now());
                userMapper.updateById(user);
            }
        }
    }

    // ===================== 教师管理实现 =====================

    @Override
    public Page<Teacher> getTeacherList(int page, int size, String keyword, String subject, String gender, Boolean isVerified) {
        Page<Teacher> pageParam = new Page<>(page, size);
        QueryWrapper<Teacher> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("is_deleted", false); // 添加软删除条件

        // 搜索条件
        if (StringUtils.hasText(keyword)) {
            queryWrapper.like("real_name", keyword);
        }
        if (StringUtils.hasText(subject)) {
            queryWrapper.like("subjects", subject);
        }
        if (StringUtils.hasText(gender)) {
            queryWrapper.eq("gender", gender);
        }
        if (isVerified != null) {
            queryWrapper.eq("is_verified", isVerified);
        }
        
        // 排序
        queryWrapper.orderByDesc("id");
        
        return teacherMapper.selectPage(pageParam, queryWrapper);
    }

    @Override
    public Teacher getTeacherById(Long teacherId) {
        return teacherMapper.selectById(teacherId);
    }

    @Override
    @Transactional
    public Teacher addTeacher(TeacherInfoRequest request) {
        // 验证必填字段
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new RuntimeException("邮箱不能为空");
        }
        if (request.getRealName() == null || request.getRealName().trim().isEmpty()) {
            throw new RuntimeException("真实姓名不能为空");
        }

        // 检查用户名是否已存在
        if (userMapper.existsByUsername(request.getUsername().trim())) {
            throw new RuntimeException("用户名已被使用");
        }

        // 检查邮箱是否已存在
        if (userMapper.existsByEmail(request.getEmail().trim())) {
            throw new RuntimeException("邮箱已被注册");
        }

        // 创建用户账号，默认密码为123456
        User user = User.builder()
                .username(request.getUsername().trim())
                .email(request.getEmail().trim())
                .password(passwordEncoder.encode("123456")) // 默认密码
                .phone(request.getPhone())
                .userType("teacher")
                .status("active")
                .isDeleted(false)
                .hasUsedTrial(false)
                .build();

        userMapper.insert(user);
        log.info("管理员创建教师用户账号成功: {}, ID: {}", user.getEmail(), user.getId());

        // 处理可上课时间
        String availableTimeSlotsJson = null;
        if (request.getAvailableTimeSlots() != null && !request.getAvailableTimeSlots().isEmpty()) {
            if (TimeSlotUtil.isValidTimeSlots(request.getAvailableTimeSlots())) {
                availableTimeSlotsJson = TimeSlotUtil.toJsonString(request.getAvailableTimeSlots());
                log.info("管理员为教师设置了可上课时间，时间段数量: {}",
                        request.getAvailableTimeSlots().stream()
                                .mapToInt(slot -> slot.getTimeSlots() != null ? slot.getTimeSlots().size() : 0)
                                .sum());
            } else {
                // 如果格式不正确，设置为null（表示所有时间都可以）
                availableTimeSlotsJson = null;
                log.warn("管理员提供的可上课时间格式不正确，将设置为所有时间可用");
            }
        } else {
            // 如果没有提供可上课时间，设置为null（表示所有时间都可以）
            availableTimeSlotsJson = null;
            log.info("管理员未为教师设置可上课时间，默认所有时间都可以");
        }

        // 创建教师信息，关联到刚创建的用户
        Teacher teacher = Teacher.builder()
                .userId(user.getId()) // 关联到刚创建的用户
                .realName(request.getRealName())
                .educationBackground(request.getEducationBackground())
                .teachingExperience(request.getTeachingExperience())
                .specialties(request.getSpecialties())
                .hourlyRate(request.getHourlyRate())
                .introduction(request.getIntroduction())
                .videoIntroUrl(request.getVideoIntroUrl())
                .gender(request.getGender() != null ? request.getGender() : "不愿透露")
                .availableTimeSlots(availableTimeSlotsJson)
                .isVerified(true) // 管理员添加的教师默认已审核
                .build();

        teacherMapper.insert(teacher);
        log.info("管理员创建教师档案成功: 教师ID={}, 用户ID={}", teacher.getId(), user.getId());

        // 处理教师科目关联
        if (request.getSubjectIds() != null && !request.getSubjectIds().isEmpty()) {
            for (Long subjectId : request.getSubjectIds()) {
                TeacherSubject teacherSubject = new TeacherSubject(teacher.getId(), subjectId);
                teacherSubjectMapper.insert(teacherSubject);
            }
            log.info("教师科目关联创建成功: 教师ID={}, 科目数量={}", teacher.getId(), request.getSubjectIds().size());
        }

        return teacher;
    }

    @Override
    @Transactional
    public Teacher updateTeacher(Long teacherId, TeacherInfoRequest request) {
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            throw new RuntimeException("教师不存在");
        }

        // 更新教师信息
        teacher.setRealName(request.getRealName());
        teacher.setEducationBackground(request.getEducationBackground());
        teacher.setTeachingExperience(request.getTeachingExperience());
        teacher.setSpecialties(request.getSpecialties());
        teacher.setHourlyRate(request.getHourlyRate());
        teacher.setIntroduction(request.getIntroduction());
        teacher.setVideoIntroUrl(request.getVideoIntroUrl());
        teacher.setGender(request.getGender() != null ? request.getGender() : "不愿透露");

        // 更新可上课时间
        if (request.getAvailableTimeSlots() != null) {
            if (TimeSlotUtil.isValidTimeSlots(request.getAvailableTimeSlots())) {
                teacher.setAvailableTimeSlots(TimeSlotUtil.toJsonString(request.getAvailableTimeSlots()));
            } else {
                // 如果格式不正确，保持原有时间不变
                // 可以选择抛出异常或者使用默认时间
                throw new RuntimeException("可上课时间格式不正确");
            }
        }

        teacherMapper.updateById(teacher);

        // 更新教师科目关联
        if (request.getSubjectIds() != null) {
            // 先删除原有关联
            teacherSubjectMapper.deleteByTeacherId(teacherId);
            // 添加新的关联
            if (!request.getSubjectIds().isEmpty()) {
                for (Long subjectId : request.getSubjectIds()) {
                    TeacherSubject teacherSubject = new TeacherSubject(teacherId, subjectId);
                    teacherSubjectMapper.insert(teacherSubject);
                }
            }
        }

        return teacher;
    }

    @Override
    @Transactional
    public void deleteTeacher(Long teacherId) {
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            throw new RuntimeException("教师不存在");
        }

        // 逻辑删除教师
        teacher.setIsDeleted(true);
        teacher.setDeletedAt(LocalDateTime.now());
        teacherMapper.updateById(teacher);

        // 如果有关联用户，同时逻辑删除对应的用户
        if (teacher.getUserId() != null) {
            User user = userMapper.selectById(teacher.getUserId());
            if (user != null) {
                user.setIsDeleted(true);
                user.setDeletedAt(LocalDateTime.now());
                userMapper.updateById(user);
            }
        }
    }

    @Override
    public void verifyTeacher(Long teacherId, Boolean isVerified) {
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            throw new RuntimeException("教师不存在");
        }

        teacher.setIsVerified(isVerified);
        teacherMapper.updateById(teacher);
    }

    @Override
    public List<Long> getTeacherSubjects(Long teacherId) {
        return teacherSubjectMapper.getSubjectIdsByTeacherId(teacherId);
    }

    @Override
    public List<Long> getStudentSubjects(Long studentId) {
        return studentSubjectMapper.getSubjectIdsByStudentId(studentId);
    }
}

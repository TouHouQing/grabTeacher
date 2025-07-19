package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.entity.Student;
import com.touhouqing.grabteacherbackend.entity.Teacher;
import com.touhouqing.grabteacherbackend.entity.User;
import com.touhouqing.grabteacherbackend.dto.StudentInfoRequest;
import com.touhouqing.grabteacherbackend.dto.TeacherInfoRequest;
import com.touhouqing.grabteacherbackend.mapper.StudentMapper;
import com.touhouqing.grabteacherbackend.mapper.TeacherMapper;
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
    private final TeacherMapper teacherMapper;
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

        statistics.put("totalUsers", totalUsers);
        statistics.put("totalStudents", totalStudents);
        statistics.put("totalTeachers", totalTeachers);
        statistics.put("verifiedTeachers", verifiedTeachers);

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
                .build();

        studentMapper.insert(student);
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

        studentMapper.updateById(student);
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
    public Page<Teacher> getTeacherList(int page, int size, String keyword, String subject, Boolean isVerified) {
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
        // 只创建教师信息，不创建用户（管理员直接创建教师档案）
        Teacher teacher = Teacher.builder()
                .userId(null) // 管理员创建的教师可能没有关联用户
                .realName(request.getRealName())
                .educationBackground(request.getEducationBackground())
                .teachingExperience(request.getTeachingExperience())
                .specialties(request.getSpecialties())
                .subjects(request.getSubjects())
                .hourlyRate(request.getHourlyRate())
                .introduction(request.getIntroduction())
                .videoIntroUrl(request.getVideoIntroUrl())
                .build();

        teacherMapper.insert(teacher);
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
        teacher.setSubjects(request.getSubjects());
        teacher.setHourlyRate(request.getHourlyRate());
        teacher.setIntroduction(request.getIntroduction());
        teacher.setVideoIntroUrl(request.getVideoIntroUrl());

        teacherMapper.updateById(teacher);
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
}

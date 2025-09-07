package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.mapper.*;
import com.touhouqing.grabteacherbackend.model.entity.*;
import com.touhouqing.grabteacherbackend.model.dto.StudentInfoDTO;
import com.touhouqing.grabteacherbackend.model.dto.TeacherInfoDTO;
import com.touhouqing.grabteacherbackend.model.dto.TimeSlotDTO;
import com.touhouqing.grabteacherbackend.model.vo.AdminStudentDetailVO;
import com.touhouqing.grabteacherbackend.model.vo.AdminTeacherDetailVO;
import com.touhouqing.grabteacherbackend.util.AliyunOssUtil;
import com.touhouqing.grabteacherbackend.util.TimeSlotUtil;
import com.touhouqing.grabteacherbackend.model.entity.Admin;

import com.touhouqing.grabteacherbackend.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.touhouqing.grabteacherbackend.cache.FeaturedTeachersLocalCache;
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
    private final AdminMapper adminMapper;
    private final BalanceTransactionMapper balanceTransactionMapper;
    private final HourDetailMapper hourDetailMapper;
    private final AliyunOssUtil ossUtil;
    private final StringRedisTemplate stringRedisTemplate;
    private final FeaturedTeachersLocalCache featuredTeachersLocalCache;
    private final PasswordEncoder passwordEncoder;
    @Autowired
    private org.springframework.cache.CacheManager cacheManager;

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
    public Page<Student> getStudentList(int page, int size, String keyword) {
        Page<Student> pageParam = new Page<>(page, size);
        QueryWrapper<Student> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("is_deleted", false); // 添加软删除条件

        // 搜索条件
        if (StringUtils.hasText(keyword)) {
            queryWrapper.like("real_name", keyword);
        }

        // 排序
        queryWrapper.orderByDesc("id");

        Page<Student> result = studentMapper.selectPage(pageParam, queryWrapper);
        
        // 为每个学生填充trialTimes和avatarUrl字段
        if (result.getRecords() != null) {
            for (Student student : result.getRecords()) {
                if (student.getUserId() != null) {
                    User user = userMapper.selectById(student.getUserId());
                    if (user != null) {
                        student.setAvatarUrl(user.getAvatarUrl());
                        student.setTrialTimes(user.getTrialTimes());
                    }
                }
            }
        }
        
        return result;
    }

    @Override
    public Student getStudentById(Long studentId) {
        QueryWrapper<Student> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", studentId);
        queryWrapper.eq("is_deleted", false);
        Student student = studentMapper.selectOne(queryWrapper);
        if (student != null && student.getUserId() != null) {
            User u = userMapper.selectById(student.getUserId());
            if (u != null) {
                student.setAvatarUrl(u.getAvatarUrl());
                student.setTrialTimes(u.getTrialTimes());
            }
        }
        return student;
    }
    @Override
    public AdminStudentDetailVO getStudentDetailById(Long studentId) {
        Student student = getStudentById(studentId);
        if (student == null) {
            return null;
        }

        User user = userMapper.selectById(student.getUserId());
        List<Long> subjectIds = studentSubjectMapper.getSubjectIdsByStudentId(studentId);

        return AdminStudentDetailVO.builder()
                .id(student.getId())
                .userId(student.getUserId())
                .realName(student.getRealName())
                .username(user != null ? user.getUsername() : null)
                .email(user != null ? user.getEmail() : null)
                .phone(user != null ? user.getPhone() : null)
                .avatarUrl(user != null ? user.getAvatarUrl() : null)
                .birthDate(user != null ? user.getBirthDate() : null)
                .subjectsInterested(student.getSubjectsInterested())
                .subjectIds(subjectIds)
                .learningGoals(student.getLearningGoals())
                .preferredTeachingStyle(student.getPreferredTeachingStyle())
                .budgetRange(student.getBudgetRange())
                .gender(student.getGender())
                .balance(student.getBalance())
                .trialTimes(user != null ? user.getTrialTimes() : 0)
                .adjustmentTimes(user != null ? user.getAdjustmentTimes() : null)
                .deleted(student.getDeleted())
                .deletedAt(student.getDeletedAt())
                .build();
    }

    @Override
    @Transactional
    public Student addStudent(StudentInfoDTO request) {
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
                .avatarUrl(request.getAvatarUrl())
                .userType("student")
                .status("active")
                .deleted(false)
                .trialTimes(1)
                .adjustmentTimes(3)
                .build();

        userMapper.insert(user);
        log.info("管理员创建学生用户账号成功: {}, ID: {}", user.getEmail(), user.getId());

        // 创建学生信息，关联到刚创建的用户
        Student student = Student.builder()
                .userId(user.getId()) // 关联到刚创建的用户
                .realName(request.getRealName())
                .subjectsInterested(request.getSubjectsInterested())
                .learningGoals(request.getLearningGoals())
                .preferredTeachingStyle(request.getPreferredTeachingStyle())
                .budgetRange(request.getBudgetRange())
                .gender(request.getGender() != null ? request.getGender() : "不愿透露")
                .balance(request.getBalance() != null ? request.getBalance() : BigDecimal.ZERO)
                .build();

        studentMapper.insert(student);
        log.info("管理员创建学生档案成功: 学生ID={}, 用户ID={}", student.getId(), user.getId());

        // 处理学生感兴趣的科目关联
        if (request.getSubjectIds() != null && !request.getSubjectIds().isEmpty()) {
            for (Long subjectId : request.getSubjectIds()) {
                StudentSubject studentSubject = new StudentSubject(student.getId(), subjectId);
                studentSubjectMapper.insert(studentSubject);
            }
            log.info("学生科目关联创建成功: 学生ID={}, 科目数量={}", student.getId(), request.getSubjectIds().size());
        }

        return student;
    }

    @Override
    @Transactional
    public Student updateStudent(Long studentId, StudentInfoDTO request) {
        return updateStudent(studentId, request, null);
    }

    @Override
    @Transactional
    public Student updateStudent(Long studentId, StudentInfoDTO request, Long operatorId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new RuntimeException("学生不存在");
        }

        // 如果更新了用户名或邮箱，需要检查是否与其他用户冲突
        if (student.getUserId() != null) {
            User currentUser = userMapper.selectById(student.getUserId());
            if (currentUser != null) {
                String oldAvatar = currentUser.getAvatarUrl();
                // 检查用户名是否与其他用户冲突（排除当前用户）
                if (request.getUsername() != null && !request.getUsername().equals(currentUser.getUsername())) {
                    if (userMapper.existsByUsername(request.getUsername())) {
                        throw new RuntimeException("用户名已被使用");
                    }
                    currentUser.setUsername(request.getUsername());
                }

                // 检查邮箱是否与其他用户冲突（排除当前用户）
                if (request.getEmail() != null && !request.getEmail().equals(currentUser.getEmail())) {
                    if (userMapper.existsByEmail(request.getEmail())) {
                        throw new RuntimeException("邮箱已被注册");
                    }
                    currentUser.setEmail(request.getEmail());
                }

                // 更新用户表中的其他信息（含头像）
                if (request.getPhone() != null) {
                    currentUser.setPhone(request.getPhone());
                }
                if (request.getAvatarUrl() != null && !request.getAvatarUrl().isEmpty()) {
                    currentUser.setAvatarUrl(request.getAvatarUrl());
                }
                // 更新密码（如果提供了新密码）
                if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
                    currentUser.setPassword(passwordEncoder.encode(request.getPassword()));
                }
                // 更新试听课次数
                if (request.getTrialTimes() != null) {
                    currentUser.setTrialTimes(request.getTrialTimes());
                }
                // 更新本月调课次数（管理员编辑）
                if (request.getAdjustmentTimes() != null) {
                    currentUser.setAdjustmentTimes(request.getAdjustmentTimes());
                }
                currentUser.setUpdatedAt(LocalDateTime.now());
                userMapper.updateById(currentUser);
                if (request.getAvatarUrl() != null && oldAvatar != null && !oldAvatar.isEmpty() && !oldAvatar.equals(request.getAvatarUrl())) {
                    ossUtil.deleteByUrl(oldAvatar);
                }
            }
        }

        // 更新学生信息
        BigDecimal oldBalance = student.getBalance();
        
        student.setRealName(request.getRealName());
        student.setSubjectsInterested(request.getSubjectsInterested());
        student.setLearningGoals(request.getLearningGoals());
        student.setPreferredTeachingStyle(request.getPreferredTeachingStyle());
        student.setBudgetRange(request.getBudgetRange());
        student.setGender(request.getGender() != null ? request.getGender() : "不愿透露");

        // 管理员可以更新余额
        if (request.getBalance() != null && !request.getBalance().equals(oldBalance)) {
            student.setBalance(request.getBalance());
            
            // 记录余额变动
            BigDecimal amountChange = request.getBalance().subtract(oldBalance);
            BalanceTransaction transaction = BalanceTransaction.builder()
                    .userId(student.getUserId())
                    .name(student.getRealName())
                    .amount(amountChange)
                    .balanceBefore(oldBalance)
                    .balanceAfter(request.getBalance())
                    .transactionType(amountChange.compareTo(BigDecimal.ZERO) > 0 ? "RECHARGE" : "DEDUCT")
                    .reason("管理员调整余额")
                    .operatorId(operatorId) // 管理员ID
                    .createdAt(LocalDateTime.now())
                    .build();
            balanceTransactionMapper.insert(transaction);
            
            log.info("管理员更新学生余额: studentId={}, 余额变动: {} -> {}, 变动金额: {}", 
                    studentId, oldBalance, request.getBalance(), amountChange);
        }

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
        student.setDeleted(true);
        student.setDeletedAt(LocalDateTime.now());
        studentMapper.updateById(student);

        // 如果有关联用户，同时逻辑删除对应的用户
        if (student.getUserId() != null) {
            User user = userMapper.selectById(student.getUserId());
            if (user != null) {
                user.setDeleted(true);
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
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher != null && teacher.getUserId() != null) {
            User u = userMapper.selectById(teacher.getUserId());
            if (u != null) {
                teacher.setAvatarUrl(u.getAvatarUrl());
            }
        }
        return teacher;
    }

    @Override
    public AdminTeacherDetailVO getTeacherDetailById(Long teacherId) {
        Teacher teacher = getTeacherById(teacherId);
        if (teacher == null) {
            return null;
        }

        User user = userMapper.selectById(teacher.getUserId());
        List<Long> subjectIds = teacherSubjectMapper.getSubjectIdsByTeacherId(teacherId);

        // 解析可上课时间
        List<TimeSlotDTO> availableTimeSlots = null;
        if (teacher.getAvailableTimeSlots() != null) {
            try {
                availableTimeSlots = TimeSlotUtil.fromJsonString(teacher.getAvailableTimeSlots());
            } catch (Exception e) {
                log.warn("解析教师可上课时间失败: teacherId={}, error={}", teacherId, e.getMessage());
                availableTimeSlots = null;
            }
        }

        return AdminTeacherDetailVO.builder()
                .id(teacher.getId())
                .userId(teacher.getUserId())
                .realName(teacher.getRealName())
                .username(user != null ? user.getUsername() : null)
                .email(user != null ? user.getEmail() : null)
                .phone(user != null ? user.getPhone() : null)
                .avatarUrl(user != null ? user.getAvatarUrl() : null)
                .birthDate(user != null ? user.getBirthDate() : null)
                .educationBackground(teacher.getEducationBackground())
                .teachingExperience(teacher.getTeachingExperience())
                .specialties(teacher.getSpecialties())
                .subjectIds(subjectIds)
                .hourlyRate(teacher.getHourlyRate())
                .rating(teacher.getRating())
                .introduction(teacher.getIntroduction())
                .videoIntroUrl(teacher.getVideoIntroUrl())
                .gender(teacher.getGender())
                .availableTimeSlots(availableTimeSlots)
                .verified(teacher.getVerified())
                .featured(teacher.getFeatured())
                .adjustmentTimes(user != null ? user.getAdjustmentTimes() : null)
                .currentHours(teacher.getCurrentHours())
                .lastHours(teacher.getLastHours())
                .deleted(teacher.getDeleted())
                .deletedAt(teacher.getDeletedAt())
                .build();
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = "teachers", allEntries = true),
        @CacheEvict(cacheNames = "teacherDetails", allEntries = true),
        @CacheEvict(cacheNames = "teacherList", allEntries = true),
        @CacheEvict(cacheNames = "teacherMatch", allEntries = true),
        @CacheEvict(cacheNames = "featuredTeachers", allEntries = true)
    })
    public Teacher addTeacher(TeacherInfoDTO request) {
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
                .avatarUrl(request.getAvatarUrl())

                .userType("teacher")
                .status("active")
                .deleted(false)
                .trialTimes(1)
                .adjustmentTimes(3)
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
                .rating(request.getRating() != null ? request.getRating() : BigDecimal.valueOf(5.0)) // 默认评分5.0
                .verified(true) // 管理员添加的教师默认已审核
                .build();

        teacherMapper.insert(teacher);
        log.info("管理员创建教师档案成功: 教师ID={}, 用户ID={}", teacher.getId(), user.getId());
        // 公开端缓存失效（教师相关 + 精选教师 JSON）
        invalidateFeaturedTeachersJsonCaches();

        // 处理教师科目关联
        if (request.getSubjectIds() != null && !request.getSubjectIds().isEmpty()) {
            for (Long subjectId : request.getSubjectIds()) {
                TeacherSubject teacherSubject = new TeacherSubject(teacher.getId(), subjectId);
                teacherSubjectMapper.insert(teacherSubject);
            }
            log.info("教师科目关联创建成功: 教师ID={}, 科目数量={}", teacher.getId(), request.getSubjectIds().size());
        }

        // 公开端缓存失效（教师相关 + 精选教师 JSON）
        invalidateFeaturedTeachersJsonCaches();
        return teacher;
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = "teachers", allEntries = true),
        @CacheEvict(cacheNames = "teacherDetails", allEntries = true),
        @CacheEvict(cacheNames = "teacherList", allEntries = true),
        @CacheEvict(cacheNames = "teacherMatch", allEntries = true),
        @CacheEvict(cacheNames = "featuredTeachers", allEntries = true)
    })
    public Teacher updateTeacher(Long teacherId, TeacherInfoDTO request) {
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            throw new RuntimeException("教师不存在");
        }

        // 如果更新了用户名或邮箱，需要检查是否与其他用户冲突
        if (teacher.getUserId() != null) {
            User currentUser = userMapper.selectById(teacher.getUserId());
            if (currentUser != null) {
                // 检查用户名是否与其他用户冲突（排除当前用户）
                if (request.getUsername() != null && !request.getUsername().equals(currentUser.getUsername())) {
                    if (userMapper.existsByUsername(request.getUsername())) {
                        throw new RuntimeException("用户名已被使用");
                    }
                    currentUser.setUsername(request.getUsername());
                }

                // 检查邮箱是否与其他用户冲突（排除当前用户）
                if (request.getEmail() != null && !request.getEmail().equals(currentUser.getEmail())) {
                    if (userMapper.existsByEmail(request.getEmail())) {
                        throw new RuntimeException("邮箱已被注册");
                    }
                    currentUser.setEmail(request.getEmail());
                }

                // 更新用户表中的其他信息（含头像）
                String oldAvatar = currentUser.getAvatarUrl();
                if (request.getPhone() != null) {
                    currentUser.setPhone(request.getPhone());
                }
                if (request.getAvatarUrl() != null && !request.getAvatarUrl().isEmpty()) {
                    currentUser.setAvatarUrl(request.getAvatarUrl());
                }
                // 更新密码（如果提供了新密码）
                if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
                    currentUser.setPassword(passwordEncoder.encode(request.getPassword()));
                }
                currentUser.setUpdatedAt(LocalDateTime.now());
                userMapper.updateById(currentUser);
                if (request.getAvatarUrl() != null && oldAvatar != null && !oldAvatar.isEmpty() && !oldAvatar.equals(request.getAvatarUrl())) {
                    ossUtil.deleteByUrl(oldAvatar);
                }
            }
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
        
        // 更新教师评分
        if (request.getRating() != null) {
            teacher.setRating(request.getRating());
        }

        // 管理员可更新教师课时（小时）
        if (request.getCurrentHours() != null) {
            java.math.BigDecimal oldCurrent = teacher.getCurrentHours() == null ? java.math.BigDecimal.ZERO : teacher.getCurrentHours();
            if (request.getCurrentHours().compareTo(oldCurrent) != 0) {
                // 写入课时明细
                java.math.BigDecimal delta = request.getCurrentHours().subtract(oldCurrent);
                HourDetail detail = HourDetail.builder()
                        .userId(teacher.getUserId())
                        .name(teacher.getRealName())
                        .hours(delta)
                        .hoursBefore(oldCurrent)
                        .hoursAfter(request.getCurrentHours())
                        .transactionType(delta.compareTo(java.math.BigDecimal.ZERO) > 0 ? 1 : 0)
                        .reason("管理员调整课时")
                        .operatorId(null)
                        .createdAt(java.time.LocalDateTime.now())
                        .build();
                hourDetailMapper.insert(detail);
            }
            teacher.setCurrentHours(request.getCurrentHours());
        }
        if (request.getLastHours() != null) {
            teacher.setLastHours(request.getLastHours());
        }

        // 更新可上课时间
        if (request.getAvailableTimeSlots() != null) {
            try {
                // 使用sanitizeTimeSlots方法来清理和验证时间数据
                List<TimeSlotDTO> sanitizedTimeSlots = TimeSlotUtil.sanitizeTimeSlots(request.getAvailableTimeSlots());
                teacher.setAvailableTimeSlots(TimeSlotUtil.toJsonString(sanitizedTimeSlots));
                log.debug("成功更新教师时间安排: teacherId={}", teacherId);
            } catch (Exception e) {
                log.error("处理可上课时间时发生错误: teacherId={}, error={}", teacherId, e.getMessage());
                // 如果出现异常，使用默认时间安排
                teacher.setAvailableTimeSlots(TimeSlotUtil.toJsonString(TimeSlotUtil.getDefaultTimeSlots()));
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

        // 清理精选教师 JSON 两级缓存
        invalidateFeaturedTeachersJsonCaches();
        return teacher;
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = "teachers", allEntries = true),
        @CacheEvict(cacheNames = "teacherDetails", allEntries = true),
        @CacheEvict(cacheNames = "teacherList", allEntries = true),
        @CacheEvict(cacheNames = "teacherMatch", allEntries = true),
        @CacheEvict(cacheNames = "featuredTeachers", allEntries = true)
    })
    public void deleteTeacher(Long teacherId) {
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            throw new RuntimeException("教师不存在");
        }

        // 逻辑删除教师
        teacher.setDeleted(true);
        teacher.setDeletedAt(LocalDateTime.now());
        teacherMapper.updateById(teacher);

        // 如果有关联用户，同时逻辑删除对应的用户
        if (teacher.getUserId() != null) {
            User user = userMapper.selectById(teacher.getUserId());
            if (user != null) {
                user.setDeleted(true);
                user.setDeletedAt(LocalDateTime.now());
                userMapper.updateById(user);
            }
        }
    }

    @Override
    @Caching(evict = {
        @CacheEvict(cacheNames = "teachers", allEntries = true),
        @CacheEvict(cacheNames = "teacherDetails", allEntries = true),
        @CacheEvict(cacheNames = "teacherList", allEntries = true),
        @CacheEvict(cacheNames = "teacherMatch", allEntries = true),
        @CacheEvict(cacheNames = "featuredTeachers", allEntries = true)
    })
    public void verifyTeacher(Long teacherId, Boolean isVerified) {
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            throw new RuntimeException("教师不存在");
        }

        teacher.setVerified(isVerified);
        teacherMapper.updateById(teacher);
    }

    @Override
    @CacheEvict(cacheNames = {"featuredTeachers", "teacherList", "teachers"}, allEntries = true)
    public void setTeacherFeatured(Long teacherId, Boolean isFeatured) {
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            throw new RuntimeException("教师不存在");
        }

        teacher.setFeatured(isFeatured);
        teacherMapper.updateById(teacher);

        // 清理精选教师相关缓存（包括 JSON 两级）
        invalidateFeaturedTeachersJsonCaches();

        log.info("设置教师精选状态成功: teacherId={}, isFeatured={}", teacherId, isFeatured);
    }

    @Override
    public List<Long> getTeacherSubjects(Long teacherId) {
        return teacherSubjectMapper.getSubjectIdsByTeacherId(teacherId);
    }

    @Override
    public java.util.Map<Long, java.util.List<Long>> getSubjectsByTeacherIds(java.util.List<Long> teacherIds) {
        if (teacherIds == null || teacherIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        List<TeacherSubject> list = teacherSubjectMapper.findByTeacherIds(teacherIds);
        java.util.Map<Long, java.util.List<Long>> map = new java.util.HashMap<>();
        for (TeacherSubject ts : list) {
            map.computeIfAbsent(ts.getTeacherId(), k -> new java.util.ArrayList<>()).add(ts.getSubjectId());
        }
        return map;
    }

    @Override
    public List<Long> getStudentSubjects(Long studentId) {
        return studentSubjectMapper.getSubjectIdsByStudentId(studentId);
    }

    // ============== 管理员资料 ==============
    @Override
    public Map<String, Object> getCurrentAdminProfile(Long currentUserId) {
        Admin admin = adminMapper.findByUserId(currentUserId);
        Map<String, Object> map = new HashMap<>();
        if (admin != null) {
            map.put("realName", admin.getRealName());
            map.put("avatarUrl", admin.getAvatarUrl());
            map.put("wechatQrcodeUrl", admin.getWechatQrcodeUrl());
            map.put("whatsappNumber", admin.getWhatsappNumber());
            map.put("email", admin.getEmail());
        }
        return map;
    }

    /**
     * 轻量清理精选教师 JSON 两级缓存（L1 本地 + L2 Redis）
     * 与 TeacherController.getFeaturedTeachers 的 key 约定保持一致
     */
    private void invalidateFeaturedTeachersJsonCaches() {
        try {
            if (featuredTeachersLocalCache != null) {
                featuredTeachersLocalCache.clear();
            }
        } catch (Exception ignore) {}
        try {
            if (stringRedisTemplate != null) {
                // 清理不分页的缓存键格式：featuredTeachers:json:all:subject:...:keyword:...
                String[] subjects = new String[]{"all"};
                String[] keywords = new String[]{"all"};
                for (String sub : subjects) {
                    for (String k : keywords) {
                        String key = String.format("featuredTeachers:json:all:subject:%s:keyword:%s", sub, k);
                            stringRedisTemplate.delete(key);
                    }
                }
            }
        } catch (Exception ignore) {}
    }


    @Override
    @Transactional
    public void updateCurrentAdminProfile(Long currentUserId, com.touhouqing.grabteacherbackend.model.dto.AdminProfileUpdateDTO dto) {
        Admin admin = adminMapper.findByUserId(currentUserId);
        if (admin == null) {
            admin = Admin.builder()
                    .userId(currentUserId)
                    .deleted(false)
                    .build();
            adminMapper.insert(admin);
        }
        if (dto.getRealName() != null) admin.setRealName(dto.getRealName());
        if (dto.getWhatsappNumber() != null) admin.setWhatsappNumber(dto.getWhatsappNumber());
        if (dto.getEmail() != null) admin.setEmail(dto.getEmail());

        if (dto.getAvatarUrl() != null && !dto.getAvatarUrl().isEmpty()) {
            String old = admin.getAvatarUrl();
            if (old != null && !old.isEmpty() && !old.equals(dto.getAvatarUrl())) {
                ossUtil.deleteByUrl(old);
            }
            admin.setAvatarUrl(dto.getAvatarUrl());
        }
        if (dto.getWechatQrcodeUrl() != null && !dto.getWechatQrcodeUrl().isEmpty()) {
            String old = admin.getWechatQrcodeUrl();
            if (old != null && !old.isEmpty() && !old.equals(dto.getWechatQrcodeUrl())) {
                ossUtil.deleteByUrl(old);
            }
            admin.setWechatQrcodeUrl(dto.getWechatQrcodeUrl());
        }
        adminMapper.updateById(admin);

        // 轻量驱逐公开端管理员联系方式缓存
        try {
            org.springframework.cache.Cache cache = cacheManager.getCache("public:adminContacts");
            if (cache != null) {
                cache.evict("first");
            }
        } catch (Exception ignore) {}
    }
}
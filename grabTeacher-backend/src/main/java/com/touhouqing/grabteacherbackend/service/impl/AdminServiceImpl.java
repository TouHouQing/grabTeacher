package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.mapper.*;
import com.touhouqing.grabteacherbackend.model.entity.*;
import com.touhouqing.grabteacherbackend.model.dto.StudentInfoDTO;
import com.touhouqing.grabteacherbackend.model.dto.TeacherInfoDTO;

import com.touhouqing.grabteacherbackend.model.vo.AdminStudentDetailVO;
import com.touhouqing.grabteacherbackend.model.vo.AdminTeacherDetailVO;
import com.touhouqing.grabteacherbackend.util.AliyunOssUtil;

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

import java.util.stream.Collectors;

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

    private final TeacherLevelMapper teacherLevelMapper;
    private final TeachingLocationMapper teachingLocationMapper;


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

    @Override
    @Transactional
    public void updateOneOnOneCourseMetrics(Long teacherId,
                                            java.util.List<com.touhouqing.grabteacherbackend.model.dto.AdminCourseMetricsUpdateDTO> items,
                                            Long operatorId) {
        if (teacherId == null) {
            throw new RuntimeException("teacherId 不能为空");
        }
        if (items == null || items.isEmpty()) {
            return; // 无变更，直接返回
        }
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null || Boolean.TRUE.equals(teacher.getDeleted())) {
            throw new RuntimeException("教师不存在或已删除");
        }
        Long teacherUserId = teacher.getUserId();
        String teacherName = teacher.getRealName();

        for (com.touhouqing.grabteacherbackend.model.dto.AdminCourseMetricsUpdateDTO dto : items) {
            if (dto == null || dto.getCourseId() == null) {
                continue;
            }
            Course course = courseMapper.selectById(dto.getCourseId());
            if (course == null || Boolean.TRUE.equals(course.getDeleted())) {
                log.warn("跳过：课程不存在或已删除，courseId={}", dto.getCourseId());
                continue;
            }
            if (!teacherId.equals(course.getTeacherId())) {
                log.warn("跳过：课程不属于该教师，teacherId={}, courseId={}, ownerTeacherId={} ", teacherId, course.getId(), course.getTeacherId());
                continue;
            }
            if (!"one_on_one".equals(course.getCourseType())) {
                log.warn("跳过：不是一对一课程，courseId={}, courseType={}", course.getId(), course.getCourseType());
                continue;
            }

            boolean changed = false;
            // 校验与更新时薪
            if (dto.getTeacherHourlyRate() != null) {
                java.math.BigDecimal rate = dto.getTeacherHourlyRate();
                if (rate.compareTo(java.math.BigDecimal.ZERO) < 0) {
                    throw new RuntimeException("时薪不能为负数");
                }
                rate = rate.setScale(2, java.math.RoundingMode.HALF_UP);
                if (course.getTeacherHourlyRate() == null || course.getTeacherHourlyRate().compareTo(rate) != 0) {
                    course.setTeacherHourlyRate(rate);
                    changed = true;
                }
            }

            // 校验与更新本月课时（差值入账）
            if (dto.getCurrentHours() != null) {
                java.math.BigDecimal newHours = dto.getCurrentHours();
                if (newHours.compareTo(java.math.BigDecimal.ZERO) < 0) {
                    throw new RuntimeException("本月课时不能为负数");
                }
                newHours = newHours.setScale(2, java.math.RoundingMode.HALF_UP);
                java.math.BigDecimal oldHours = course.getCurrentHours() != null ? course.getCurrentHours() : java.math.BigDecimal.ZERO;
                oldHours = oldHours.setScale(2, java.math.RoundingMode.HALF_UP);
                java.math.BigDecimal delta = newHours.subtract(oldHours);
                if (delta.compareTo(java.math.BigDecimal.ZERO) != 0) {
                    // 写入调整明细（Admin 手动调整）
                    HourDetail record = HourDetail.builder()
                            .userId(teacherUserId)
                            .name(teacherName)
                            .hours(delta)
                            .hoursBefore(oldHours)
                            .hoursAfter(newHours)
                            .reasonCode(com.touhouqing.grabteacherbackend.model.entity.HourDetail.REASON_CODE_ADMIN_ADJUSTMENT)
                            .transactionType(delta.signum() >= 0 ? 1 : 0)
                            .reason("管理员手动调整：课程(ID=" + course.getId() + ", 标题=" + course.getTitle() + ")")
                            .operatorId(operatorId)
                            .createdAt(java.time.LocalDateTime.now())
                            .build();
                    hourDetailMapper.insert(record);

                    course.setCurrentHours(newHours);
                    changed = true;
                }
            }

            if (changed) {
                courseMapper.updateById(course);
            }
        }
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
        // 验证必填字段（用户名可选：留空则自动生成）
        if (request.getRealName() == null || request.getRealName().trim().isEmpty()) {
            throw new RuntimeException("真实姓名不能为空");
        }

        // 处理用户名逻辑：如果管理员未填写，则先用临时用户名插入，拿到ID后再设置为 student+userId
        String providedUsername = request.getUsername() != null ? request.getUsername().trim() : "";
        boolean needAutoUsername = !StringUtils.hasText(providedUsername);
        if (!needAutoUsername) {
            // 检查用户名是否已存在（仅当管理员提供时）
            if (userMapper.existsByUsername(providedUsername)) {
                throw new RuntimeException("用户名已被使用");
            }
        }

        // 生成唯一邮箱（使用时间戳避免冲突）
        String autoEmail = "student_" + System.currentTimeMillis() + "@grabteacher.com";

        // 创建用户账号，默认密码为123456
        String usernameToUse = needAutoUsername ? ("student_tmp_" + System.currentTimeMillis()) : providedUsername;
        User user = User.builder()
                .username(usernameToUse)
                .email(autoEmail)
                .password(passwordEncoder.encode("123456")) // 默认密码
                .avatarUrl(request.getAvatarUrl())
                .userType("student")
                .status("active")
                .deleted(false)
                .trialTimes(1)
                .adjustmentTimes(3)
                .build();

        userMapper.insert(user);
        log.info("管理员创建学生用户账号成功: {}, ID: {}", user.getEmail(), user.getId());

        // 若使用自动用户名，拿到ID后设置为最终用户名：student+userId
        if (needAutoUsername) {
            String finalUsername = "student" + user.getId();
            user.setUsername(finalUsername);
            userMapper.updateById(user);
            log.info("学生默认用户名已设置为: {}", finalUsername);
        }

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

        // 如果更新了用户名，需要检查是否与其他用户冲突
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

        // 管理员可以更新余额（仅在余额实际变化时记录明细；忽略 0 与 0.00 的刻度差异）
        if (request.getBalance() != null) {
            BigDecimal oldBal = (oldBalance != null) ? oldBalance : BigDecimal.ZERO;
            if (request.getBalance().compareTo(oldBal) != 0) {
                student.setBalance(request.getBalance());

                // 记录余额变动（仅在有变动时）
                BigDecimal amountChange = request.getBalance().subtract(oldBal);
                BalanceTransaction transaction = BalanceTransaction.builder()
                        .userId(student.getUserId())
                        .name(student.getRealName())
                        .amount(amountChange)
                        .balanceBefore(oldBal)
                        .balanceAfter(request.getBalance())
                        .transactionType(amountChange.compareTo(BigDecimal.ZERO) > 0 ? "RECHARGE" : "DEDUCT")
                        .reason("管理员调整余额")
                        .operatorId(operatorId) // 管理员ID
                        .createdAt(LocalDateTime.now())
                        .build();
                balanceTransactionMapper.insert(transaction);

                log.info("管理员更新学生余额: studentId={}, 余额变动: {} -> {}, 变动金额: {}",
                        studentId, oldBal, request.getBalance(), amountChange);
            }
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
        // 如果有科目筛选，需要通过关联表查询
        if (StringUtils.hasText(subject)) {
            return getTeacherListBySubject(page, size, keyword, subject, gender, isVerified);
        }

        // 没有科目筛选时的简单查询
        Page<Teacher> pageParam = new Page<>(page, size);
        QueryWrapper<Teacher> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("is_deleted", false); // 添加软删除条件

        // 搜索条件
        if (StringUtils.hasText(keyword)) {
            queryWrapper.like("real_name", keyword);
        }
        if (StringUtils.hasText(gender)) {
            queryWrapper.eq("gender", gender);
        }
        if (isVerified != null) {
            queryWrapper.eq("is_verified", isVerified);
        }

        // 排序
        queryWrapper.orderByDesc("id");

        Page<Teacher> pageResult = teacherMapper.selectPage(pageParam, queryWrapper);
        java.util.List<Teacher> records = pageResult.getRecords();
        if (records != null && !records.isEmpty()) {
            java.util.List<Long> teacherIds = new java.util.ArrayList<>();
            for (Teacher t : records) { if (t != null) teacherIds.add(t.getId()); }
            try {
                java.util.List<java.util.Map<String, Object>> sums = courseMapper.sumHoursByTeacherIds(teacherIds);
                java.util.Map<Long, java.math.BigDecimal> curMap = new java.util.HashMap<>();
                java.util.Map<Long, java.math.BigDecimal> lastMap = new java.util.HashMap<>();
                if (sums != null) {
                    for (java.util.Map<String, Object> row : sums) {
                        if (row == null) continue;
                        Object tidObj = row.get("teacherId");
                        if (tidObj == null) continue;
                        Long tid = tidObj instanceof Number ? ((Number) tidObj).longValue() : Long.valueOf(String.valueOf(tidObj));
                        java.math.BigDecimal ch = row.get("currentHours") instanceof java.math.BigDecimal
                                ? (java.math.BigDecimal) row.get("currentHours")
                                : new java.math.BigDecimal(String.valueOf(row.get("currentHours")));
                        java.math.BigDecimal lh = row.get("lastHours") instanceof java.math.BigDecimal
                                ? (java.math.BigDecimal) row.get("lastHours")
                                : new java.math.BigDecimal(String.valueOf(row.get("lastHours")));
                        curMap.put(tid, ch);
                        lastMap.put(tid, lh);
                    }
                }
                for (Teacher t : records) {
                    if (t == null) continue;
                    t.setCurrentHours(curMap.getOrDefault(t.getId(), java.math.BigDecimal.ZERO));
                    t.setLastHours(lastMap.getOrDefault(t.getId(), java.math.BigDecimal.ZERO));
                }
            } catch (Exception ignore) { }
        }
        return pageResult;
    }

    /**
     * 根据科目筛选教师列表（管理员端）
     */
    private Page<Teacher> getTeacherListBySubject(int page, int size, String keyword, String subject, String gender, Boolean isVerified) {
        int offset = Math.max(0, (page - 1) * size);

        // 查询教师列表
        List<Teacher> teachers = teacherMapper.findTeachersBySubjectForAdmin(subject, keyword, gender, isVerified, offset, size);

        // 查询总数
        long total = teacherMapper.countTeachersBySubjectForAdmin(subject, keyword, gender, isVerified);
        // 
        if (teachers != null && !teachers.isEmpty()) {
            java.util.List<Long> ids = new java.util.ArrayList<>();
            for (Teacher t : teachers) { if (t != null) ids.add(t.getId()); }
            try {
                java.util.List<java.util.Map<String, Object>> sums = courseMapper.sumHoursByTeacherIds(ids);
                java.util.Map<Long, java.math.BigDecimal> curMap = new java.util.HashMap<>();
                java.util.Map<Long, java.math.BigDecimal> lastMap = new java.util.HashMap<>();
                if (sums != null) {
                    for (java.util.Map<String, Object> row : sums) {
                        if (row == null) continue;
                        Object tidObj = row.get("teacherId");
                        if (tidObj == null) continue;
                        Long tid = tidObj instanceof Number ? ((Number) tidObj).longValue() : Long.valueOf(String.valueOf(tidObj));
                        java.math.BigDecimal ch = row.get("currentHours") instanceof java.math.BigDecimal
                                ? (java.math.BigDecimal) row.get("currentHours")
                                : new java.math.BigDecimal(String.valueOf(row.get("currentHours")));
                        java.math.BigDecimal lh = row.get("lastHours") instanceof java.math.BigDecimal
                                ? (java.math.BigDecimal) row.get("lastHours")
                                : new java.math.BigDecimal(String.valueOf(row.get("lastHours")));
                        curMap.put(tid, ch);
                        lastMap.put(tid, lh);
                    }
                }
                for (Teacher t : teachers) {
                    if (t == null) continue;
                    t.setCurrentHours(curMap.getOrDefault(t.getId(), java.math.BigDecimal.ZERO));
                    t.setLastHours(lastMap.getOrDefault(t.getId(), java.math.BigDecimal.ZERO));
                }
            } catch (Exception ignore) { }
        }


        // 构建分页结果
        Page<Teacher> pageResult = new Page<>(page, size);
        pageResult.setRecords(teachers);
        pageResult.setTotal(total);

        return pageResult;
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

        // 按星期字段已废弃：管理端不再回显 weekly availableTimeSlots

        // 授课地点回显：supportsOnline 独立，线下为ID列表（从CSV解析，保持兼容）
        java.util.List<Long> offlineIds;
        if (StringUtils.hasText(teacher.getTeachingLocations())) {
            offlineIds = Arrays.stream(teacher.getTeachingLocations().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
        } else {
            offlineIds = java.util.Collections.emptyList();
        }
        boolean supportsOnline = teacher.getSupportsOnline() != null ? teacher.getSupportsOnline() : offlineIds.isEmpty();

        //
        //
        java.util.List<com.touhouqing.grabteacherbackend.model.entity.Course> _courses = courseMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.touhouqing.grabteacherbackend.model.entity.Course>()
                        .eq("teacher_id", teacherId)
                        .eq("course_type", "one_on_one")
                        .eq("is_deleted", false)
        );
        java.util.List<com.touhouqing.grabteacherbackend.model.vo.CourseSalaryBriefVO> courseSalaries = new java.util.ArrayList<>();
        for (var c : _courses) {
            java.math.BigDecimal ch = c.getCurrentHours() != null ? c.getCurrentHours() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal lh = c.getLastHours() != null ? c.getLastHours() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal rate = c.getTeacherHourlyRate() != null ? c.getTeacherHourlyRate() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal curAmt = rate.multiply(ch).setScale(2, java.math.RoundingMode.HALF_UP);
            java.math.BigDecimal lastAmt = rate.multiply(lh).setScale(2, java.math.RoundingMode.HALF_UP);
            courseSalaries.add(
                    com.touhouqing.grabteacherbackend.model.vo.CourseSalaryBriefVO.builder()
                            .courseId(c.getId())
                            .title(c.getTitle())
                            .teacherHourlyRate(rate)
                            .currentHours(ch)
                            .lastHours(lh)
                            .currentAmount(curAmt)
                            .lastAmount(lastAmt)
                            .build()
            );
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
                .rating(teacher.getRating())
                .introduction(teacher.getIntroduction())
                .hourlyRateText(teacher.getHourlyRateText())
                .gender(teacher.getGender())
                .level(teacher.getLevel())

                .verified(teacher.getVerified())
                .featured(teacher.getFeatured())
                .adjustmentTimes(user != null ? user.getAdjustmentTimes() : null)
                .supportsOnline(supportsOnline)
                .teachingLocationIds(offlineIds)
                .teachingLocations(teacher.getTeachingLocations())
                .deleted(teacher.getDeleted())
                .deletedAt(teacher.getDeletedAt())
                .courseSalaries(courseSalaries)
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
        // 验证必填字段（用户名可选：留空则自动生成；邮箱/手机号非必填）
        if (request.getRealName() == null || request.getRealName().trim().isEmpty()) {
            throw new RuntimeException("真实姓名不能为空");
        }


        // 处理用户名逻辑：如果管理员未填写，则先用临时用户名插入，拿到ID后再设置为 teacher+userId
        String providedUsername = request.getUsername() != null ? request.getUsername().trim() : "";
        boolean needAutoUsername = !StringUtils.hasText(providedUsername);
        if (!needAutoUsername) {
            // 检查用户名是否已存在（仅当管理员提供时）
            if (userMapper.existsByUsername(providedUsername)) {
                throw new RuntimeException("用户名已被使用");
            }
        }

        // 邮箱为可选字段：仅当提供时检查唯一性
        if (org.springframework.util.StringUtils.hasText(request.getEmail())
                && userMapper.existsByEmail(request.getEmail().trim())) {
            throw new RuntimeException("邮箱已被注册");
        }
        // 学历必填
        if (!StringUtils.hasText(request.getEducationBackground())) {
            throw new RuntimeException("学历不能为空");
        }
        // 教学科目必填
        if (request.getSubjectIds() == null || request.getSubjectIds().isEmpty()) {
            throw new RuntimeException("教学科目不能为空");
        }

        // 创建用户账号，默认密码为123456
        String usernameToUse = needAutoUsername ? ("teacher_tmp_" + System.currentTimeMillis()) : providedUsername;
        User user = User.builder()
                .username(usernameToUse)
                .email(StringUtils.hasText(request.getEmail()) ? request.getEmail().trim() : null)
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

        // 若使用自动用户名，拿到ID后设置为最终用户名：teacher+userId
        if (needAutoUsername) {
            String finalUsername = "teacher" + user.getId();
            user.setUsername(finalUsername);
            userMapper.updateById(user);
            log.info("教师默认用户名已设置为: {}", finalUsername);
        }

        // 按星期字段已废弃：创建教师不再接受 weekly availableTimeSlots

        // 教师级别必填且必须为激活状态（从教师级别表选择）
        String resolvedLevelName;
        if (!StringUtils.hasText(request.getLevel())) {
            throw new RuntimeException("教师级别不能为空");
        } else {
            TeacherLevel lv = teacherLevelMapper.selectOne(new QueryWrapper<TeacherLevel>()
                    .eq("name", request.getLevel())
                    .eq("is_active", true));
            if (lv == null) {
                throw new RuntimeException("无效的教师级别");
            }
            resolvedLevelName = lv.getName();
        }

        // 授课地点校验：supportsOnline 独立，线下地点必须来源于授课地点表且启用
        boolean supportsOnline = Boolean.TRUE.equals(request.getSupportsOnline());
        String teachingLocationsCsv = null;
        if (request.getTeachingLocationIds() != null) {
            if (!request.getTeachingLocationIds().isEmpty()) {
                List<Long> ids = request.getTeachingLocationIds();
                QueryWrapper<TeachingLocation> lq = new QueryWrapper<>();
                lq.in("id", ids).eq("is_active", true);
                Long cnt = teachingLocationMapper.selectCount(lq);
                if (cnt == null || cnt.intValue() != ids.size()) {
                    throw new RuntimeException("存在无效或未启用的授课地点");
                }
                teachingLocationsCsv = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
            }
        }
        // 至少选择一个授课地点（线上或线下）
        boolean hasOffline = org.springframework.util.StringUtils.hasText(teachingLocationsCsv);
        if (!supportsOnline && !hasOffline) {
            throw new RuntimeException("请至少选择一个授课地点（线上或线下）");
        }

        // 创建教师信息，关联到刚创建的用户
        Teacher teacher = Teacher.builder()
                .userId(user.getId()) // 关联到刚创建的用户
                .realName(request.getRealName())
                .educationBackground(request.getEducationBackground())
                .teachingExperience(request.getTeachingExperience())
                .specialties(request.getSpecialties())
                .introduction(request.getIntroduction())
                .gender(request.getGender() != null ? request.getGender() : "不愿透露")
                .level(resolvedLevelName)
                .supportsOnline(supportsOnline)
                .teachingLocations(teachingLocationsCsv)
                .hourlyRateText(request.getHourlyRateText())

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
        if (request.getRealName() != null) {
            teacher.setRealName(request.getRealName());
        }
        if (request.getEducationBackground() != null) {
            if (!StringUtils.hasText(request.getEducationBackground())) {
                throw new RuntimeException("学历不能为空");
            }
            teacher.setEducationBackground(request.getEducationBackground());
        }
        if (request.getTeachingExperience() != null) {
            teacher.setTeachingExperience(request.getTeachingExperience());
        }
        if (request.getSpecialties() != null) {
            teacher.setSpecialties(request.getSpecialties());
        }
        if (request.getIntroduction() != null) {
            teacher.setIntroduction(request.getIntroduction());
        }
        if (request.getGender() != null) {
            teacher.setGender(request.getGender());
        }
        // 更新教师级别（必填字段：若传入则校验，不允许置空）
        if (request.getLevel() != null) {
            if (!StringUtils.hasText(request.getLevel())) {
                throw new RuntimeException("教师级别不能为空");
            }
            TeacherLevel lv = teacherLevelMapper.selectOne(new QueryWrapper<TeacherLevel>()
                    .eq("name", request.getLevel())
                    .eq("is_active", true));
            if (lv == null) {
                throw new RuntimeException("无效的教师级别");
            }
            teacher.setLevel(lv.getName());
        }

        // 更新 supportsOnline（线上开关）
        if (request.getSupportsOnline() != null) {
            teacher.setSupportsOnline(Boolean.TRUE.equals(request.getSupportsOnline()));
        }

        // 
        if (request.getTeachingLocationIds() != null) {
            if (request.getTeachingLocationIds().isEmpty()) {
                // 
                teacher.setTeachingLocations(null);
            } else {
                List<Long> ids = request.getTeachingLocationIds();
                QueryWrapper<TeachingLocation> q = new QueryWrapper<>();
                q.in("id", ids).eq("is_active", true);
                Long cnt = teachingLocationMapper.selectCount(q);
                if (cnt == null || cnt.intValue() != ids.size()) {
                    throw new RuntimeException("存在无效或未启用的授课地点");
                }
                teacher.setTeachingLocations(ids.stream().map(String::valueOf).collect(Collectors.joining(",")));
            }
        }
        // 校验最终授课地点状态：至少保留线上或至少一个线下地点
        {
            Boolean finalSupportsOnline = teacher.getSupportsOnline() != null ? teacher.getSupportsOnline() : Boolean.FALSE;
            boolean finalHasOffline = org.springframework.util.StringUtils.hasText(teacher.getTeachingLocations());
            if (!finalSupportsOnline && !finalHasOffline) {
                throw new RuntimeException("请至少选择一个授课地点（线上或线下）");
            }
        }


        // 更新教师评分
        if (request.getRating() != null) {
            teacher.setRating(request.getRating());
        }

        // 按星期字段已废弃：更新教师不再处理 weekly availableTimeSlots

        // 更新教师时薪展示文本（varchar）
        if (request.getHourlyRateText() != null) {
            teacher.setHourlyRateText(request.getHourlyRateText());
        }

        teacherMapper.updateById(teacher);

        // 更新教师科目关联（必填：不允许置空）
        if (request.getSubjectIds() != null) {
            if (request.getSubjectIds().isEmpty()) {
                throw new RuntimeException("教学科目不能为空");
            }
            // 先删除原有关联
            teacherSubjectMapper.deleteByTeacherId(teacherId);
            // 添加新的关联
            for (Long subjectId : request.getSubjectIds()) {
                TeacherSubject teacherSubject = new TeacherSubject(teacherId, subjectId);
                teacherSubjectMapper.insert(teacherSubject);
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
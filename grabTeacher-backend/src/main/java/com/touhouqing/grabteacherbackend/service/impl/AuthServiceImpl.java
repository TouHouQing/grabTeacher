package com.touhouqing.grabteacherbackend.service.impl;

import com.touhouqing.grabteacherbackend.dto.AuthResponseDTO;
import com.touhouqing.grabteacherbackend.dto.LoginRequestDTO;
import com.touhouqing.grabteacherbackend.dto.RegisterRequestDTO;
import com.touhouqing.grabteacherbackend.dto.UserUpdateRequestDTO;
import com.touhouqing.grabteacherbackend.util.TimeSlotUtil;
import com.touhouqing.grabteacherbackend.entity.Student;
import com.touhouqing.grabteacherbackend.entity.Teacher;
import com.touhouqing.grabteacherbackend.entity.User;
import com.touhouqing.grabteacherbackend.entity.Admin;

import com.touhouqing.grabteacherbackend.entity.StudentSubject;
import com.touhouqing.grabteacherbackend.mapper.StudentMapper;
import com.touhouqing.grabteacherbackend.mapper.StudentSubjectMapper;
import com.touhouqing.grabteacherbackend.mapper.TeacherMapper;
import com.touhouqing.grabteacherbackend.mapper.UserMapper;
import com.touhouqing.grabteacherbackend.mapper.AdminMapper;
import com.touhouqing.grabteacherbackend.mapper.TeacherSubjectMapper;
import com.touhouqing.grabteacherbackend.service.AuthService;
import com.touhouqing.grabteacherbackend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime; // 添加这个import

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final StudentMapper studentMapper;
    private final StudentSubjectMapper studentSubjectMapper;
    private final TeacherMapper teacherMapper;
    private final AdminMapper adminMapper; // 新增
    private final TeacherSubjectMapper teacherSubjectMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * 用户注册
     */
    @Override
    @Transactional
    public AuthResponseDTO registerUser(RegisterRequestDTO registerRequestDTO) {
        try {
            // 验证密码确认
            if (!registerRequestDTO.getPassword().equals(registerRequestDTO.getConfirmPassword())) {
                throw new RuntimeException("密码和确认密码不匹配");
            }

            // 检查用户名是否已存在
            if (userMapper.existsByUsername(registerRequestDTO.getUsername())) {
                throw new RuntimeException("用户名已被使用");
            }

            // 检查邮箱是否已存在
            if (userMapper.existsByEmail(registerRequestDTO.getEmail())) {
                throw new RuntimeException("邮箱已被注册");
            }

            // 创建用户
            User user = User.builder()
                    .username(registerRequestDTO.getUsername())
                    .email(registerRequestDTO.getEmail())
                    .password(passwordEncoder.encode(registerRequestDTO.getPassword()))
                    .phone(registerRequestDTO.getPhone())
                    .birthDate(registerRequestDTO.getBirthDate())
                    .userType(registerRequestDTO.getUserType().name())
                    .status("active")
                    .isDeleted(false)
                    .hasUsedTrial(false) // 新用户默认未使用免费试听
                    .build();

            userMapper.insert(user);
            log.info("用户注册成功: {}, ID: {}", user.getEmail(), user.getId());

            // 根据用户类型创建对应的详细信息记录
            if ("student".equals(registerRequestDTO.getUserType().name())) {
                Student student = Student.builder()
                        .userId(user.getId())
                        .realName(registerRequestDTO.getRealName())
                        .gradeLevel(StringUtils.hasText(registerRequestDTO.getGradeLevel()) ? registerRequestDTO.getGradeLevel() : null)
                        .subjectsInterested(StringUtils.hasText(registerRequestDTO.getSubjectsInterested()) ? registerRequestDTO.getSubjectsInterested() : null)
                        .learningGoals(StringUtils.hasText(registerRequestDTO.getLearningGoals()) ? registerRequestDTO.getLearningGoals() : null)
                        .preferredTeachingStyle(StringUtils.hasText(registerRequestDTO.getPreferredTeachingStyle()) ? registerRequestDTO.getPreferredTeachingStyle() : null)
                        .budgetRange(StringUtils.hasText(registerRequestDTO.getBudgetRange()) ? registerRequestDTO.getBudgetRange() : null)
                        .gender(StringUtils.hasText(registerRequestDTO.getGender()) ? registerRequestDTO.getGender() : "不愿透露")
                        .isDeleted(false)
                        .build();
                
                studentMapper.insert(student);
                log.info("学生信息创建成功: {}", user.getId());

                // 处理学生感兴趣的科目关联
                if (registerRequestDTO.getStudentSubjectIds() != null && !registerRequestDTO.getStudentSubjectIds().isEmpty()) {
                    for (Long subjectId : registerRequestDTO.getStudentSubjectIds()) {
                        StudentSubject studentSubject = new StudentSubject(student.getId(), subjectId);
                        studentSubjectMapper.insert(studentSubject);
                    }
                    log.info("学生科目关联创建成功: 学生ID={}, 科目数量={}", student.getId(), registerRequestDTO.getStudentSubjectIds().size());
                }
                
            } else if ("teacher".equals(registerRequestDTO.getUserType().name())) {
                // 处理可上课时间
                String availableTimeSlotsJson = null;
                if (registerRequestDTO.getAvailableTimeSlots() != null && !registerRequestDTO.getAvailableTimeSlots().isEmpty()) {
                    if (TimeSlotUtil.isValidTimeSlots(registerRequestDTO.getAvailableTimeSlots())) {
                        availableTimeSlotsJson = TimeSlotUtil.toJsonString(registerRequestDTO.getAvailableTimeSlots());
                        log.info("教师注册时设置了可上课时间，时间段数量: {}",
                                registerRequestDTO.getAvailableTimeSlots().stream()
                                        .mapToInt(slot -> slot.getTimeSlots() != null ? slot.getTimeSlots().size() : 0)
                                        .sum());
                    } else {
                        log.warn("教师注册时提供的可上课时间格式不正确，将设置为所有时间可用");
                        availableTimeSlotsJson = null; // 格式不正确时设置为null，表示所有时间都可以
                    }
                } else {
                    // 如果没有提供可上课时间，设置为null（表示所有时间都可以）
                    availableTimeSlotsJson = null;
                    log.info("教师注册时未设置可上课时间，默认所有时间都可以");
                }

                Teacher teacher = Teacher.builder()
                        .userId(user.getId())
                        .realName(registerRequestDTO.getRealName())
                        .educationBackground(StringUtils.hasText(registerRequestDTO.getEducationBackground()) ? registerRequestDTO.getEducationBackground() : null)
                        .teachingExperience(registerRequestDTO.getTeachingExperience() != null && registerRequestDTO.getTeachingExperience() > 0 ? registerRequestDTO.getTeachingExperience() : null)
                        .specialties(StringUtils.hasText(registerRequestDTO.getSpecialties()) ? registerRequestDTO.getSpecialties() : null)
                        // 注册时不设置收费，由管理员后续设置
                        .hourlyRate(null)
                        .introduction(StringUtils.hasText(registerRequestDTO.getIntroduction()) ? registerRequestDTO.getIntroduction() : null)
                        .gender(StringUtils.hasText(registerRequestDTO.getGender()) ? registerRequestDTO.getGender() : "不愿透露")
                        .availableTimeSlots(availableTimeSlotsJson)
                        .isVerified(false)
                        .isDeleted(false)
                        .build();

                teacherMapper.insert(teacher);

                // 注册时不设置科目关联，由管理员后续设置
                log.info("教师信息创建成功: {}, 可上课时间已设置", user.getId());
            }

            // 生成 JWT token
            String jwt = jwtUtil.generateTokenFromUserId(user.getId());

            return new AuthResponseDTO(jwt, user.getId(), user.getUsername(),
                                  user.getEmail(), user.getUserType());
        } catch (Exception e) {
            log.error("用户注册失败: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 判断输入是否为邮箱格式
     */
    private boolean isEmailFormat(String input) {
        return input != null && input.contains("@") && input.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /**
     * 根据输入类型生成相应的错误消息
     */
    private String getLoginErrorMessage(String input) {
        if (isEmailFormat(input)) {
            return "邮箱或密码错误";
        } else {
            return "用户名或密码错误";
        }
    }

    /**
     * 用户登录
     */
    @Override
    public AuthResponseDTO authenticateUser(LoginRequestDTO loginRequestDTO) {
        try {
            log.info("尝试登录用户: {}", loginRequestDTO.getUsername());

            // 支持用户名或邮箱登录
            User user = userMapper.findByUsernameOrEmail(loginRequestDTO.getUsername());
            if (user == null) {
                log.warn("登录失败，用户不存在: {}", loginRequestDTO.getUsername());
                String errorMessage = getLoginErrorMessage(loginRequestDTO.getUsername());
                throw new BadCredentialsException(errorMessage);
            }

            log.info("找到用户: {}, 状态: {}", user.getEmail(), user.getStatus());

            // 检查用户状态
            if (!"active".equals(user.getStatus())) {
                log.warn("登录失败，用户账户未激活: {}", loginRequestDTO.getUsername());
                throw new BadCredentialsException("账户未激活");
            }

            // 验证密码
            if (!passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPassword())) {
                log.warn("登录失败，密码错误: {}", loginRequestDTO.getUsername());
                String errorMessage = getLoginErrorMessage(loginRequestDTO.getUsername());
                throw new BadCredentialsException(errorMessage);
            }

            // 进行身份验证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequestDTO.getUsername(),
                            loginRequestDTO.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String jwt = jwtUtil.generateToken(authentication);

            // 根据用户类型获取真实姓名
            String realName = null;
            if ("student".equals(user.getUserType())) {
                Student student = studentMapper.findByUserId(user.getId());
                realName = student != null ? student.getRealName() : null;
            } else if ("teacher".equals(user.getUserType())) {
                Teacher teacher = teacherMapper.findByUserId(user.getId());
                realName = teacher != null ? teacher.getRealName() : null;
            }

            log.info("用户登录成功: {}", user.getEmail());

            return new AuthResponseDTO(jwt, user.getId(), user.getUsername(),
                                  user.getEmail(), user.getUserType(), realName);
        } catch (BadCredentialsException e) {
            log.warn("登录失败，认证错误: {}", e.getMessage());
            // 直接抛出原始异常，保持错误消息
            throw e;
        } catch (AuthenticationException e) {
            log.warn("登录失败，认证异常: {}", e.getMessage());
            String errorMessage = getLoginErrorMessage(loginRequestDTO.getUsername());
            throw new BadCredentialsException(errorMessage);
        } catch (Exception e) {
            log.error("登录失败，系统错误: {}", e.getMessage(), e);
            throw new RuntimeException("登录失败，请稍后重试");
        }
    }

    /**
     * 管理员登录
     */
    @Override
    public AuthResponseDTO authenticateAdmin(LoginRequestDTO loginRequestDTO) {
        try {
            log.info("尝试管理员登录: {}", loginRequestDTO.getUsername());

            // 支持用户名或邮箱登录
            User user = userMapper.findByUsernameOrEmail(loginRequestDTO.getUsername());
            if (user == null) {
                log.warn("管理员登录失败，用户不存在: {}", loginRequestDTO.getUsername());
                String errorMessage = getLoginErrorMessage(loginRequestDTO.getUsername());
                throw new BadCredentialsException(errorMessage);
            }

            // 检查是否为管理员类型
            if (!"admin".equals(user.getUserType())) {
                log.warn("登录失败，用户类型不是管理员: {}", loginRequestDTO.getUsername());
                String errorMessage = getLoginErrorMessage(loginRequestDTO.getUsername());
                throw new BadCredentialsException(errorMessage);
            }

            log.info("找到管理员用户: {}, 状态: {}", user.getEmail(), user.getStatus());

            // 检查用户状态
            if (!"active".equals(user.getStatus())) {
                log.warn("管理员登录失败，账户未激活: {}", loginRequestDTO.getUsername());
                throw new BadCredentialsException("账户未激活");
            }

            // 验证密码
            if (!passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPassword())) {
                log.warn("管理员登录失败，密码错误: {}", loginRequestDTO.getUsername());
                String errorMessage = getLoginErrorMessage(loginRequestDTO.getUsername());
                throw new BadCredentialsException(errorMessage);
            }

            // 进行身份验证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequestDTO.getUsername(),
                            loginRequestDTO.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String jwt = jwtUtil.generateToken(authentication);

            // 获取管理员真实姓名
            Admin admin = adminMapper.findByUserId(user.getId());
            String realName = admin != null ? admin.getRealName() : null;

            log.info("管理员登录成功: {}", user.getEmail());

            return new AuthResponseDTO(jwt, user.getId(), user.getUsername(),
                                  user.getEmail(), user.getUserType(), realName);
        } catch (BadCredentialsException e) {
            log.warn("管理员登录失败，认证错误: {}", e.getMessage());
            // 直接抛出原始异常，保持错误消息
            throw e;
        } catch (AuthenticationException e) {
            log.warn("管理员登录失败，认证异常: {}", e.getMessage());
            String errorMessage = getLoginErrorMessage(loginRequestDTO.getUsername());
            throw new BadCredentialsException(errorMessage);
        } catch (Exception e) {
            log.error("管理员登录失败，系统错误: {}", e.getMessage(), e);
            throw new RuntimeException("登录失败，请稍后重试");
        }
    }

    /**
     * 检查用户名是否可用
     */
    @Override
    public boolean isUsernameAvailable(String username) {
        try {
            return !userMapper.existsByUsername(username);
        } catch (Exception e) {
            log.error("检查用户名可用性失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查邮箱是否可用
     */
    @Override
    public boolean isEmailAvailable(String email) {
        try {
            return !userMapper.existsByEmail(email);
        } catch (Exception e) {
            log.error("检查邮箱可用性失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 根据用户ID获取用户信息
     */
    @Override
    public User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }

    /**
     * 更新用户密码
     */
    @Override
    @Transactional
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now()); // 显式设置更新时间
        userMapper.updateById(user);
        
        log.info("用户密码更新成功: userId={}", userId);
    }

    /**
     * 修改密码
     */
    @Override
    public boolean changePassword(Long userId, String currentPassword, String newPassword) {
        try {
            User user = userMapper.selectById(userId);
            if (user == null) {
                return false;
            }

            // 验证当前密码
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                return false;
            }

            // 更新密码
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setUpdatedAt(LocalDateTime.now()); // 显式设置更新时间
            userMapper.updateById(user);

            return true;
        } catch (Exception e) {
            log.error("修改密码失败: ", e);
            return false;
        }
    }

    /**
     * 更新用户邮箱
     */
    @Override
    @Transactional
    public boolean updateEmail(Long userId, String newEmail, String currentPassword) {
        try {
            User user = userMapper.selectById(userId);
            if (user == null) {
                log.warn("用户不存在: userId={}", userId);
                return false;
            }

            // 验证当前密码
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                log.warn("密码验证失败: userId={}", userId);
                return false;
            }

            // 检查新邮箱是否已被其他用户使用
            if (userMapper.existsByEmail(newEmail)) {
                log.warn("邮箱已被使用: email={}", newEmail);
                return false;
            }

            // 更新邮箱
            String oldEmail = user.getEmail();
            user.setEmail(newEmail);
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);

            log.info("用户邮箱更新成功: userId={}, oldEmail={}, newEmail={}", userId, oldEmail, newEmail);
            return true;
        } catch (Exception e) {
            log.error("更新邮箱失败: userId={}, newEmail={}", userId, newEmail, e);
            return false;
        }
    }

    /**
     * 更新用户基本信息
     */
    @Override
    @Transactional
    public boolean updateUserProfile(Long userId, UserUpdateRequestDTO request) {
        try {
            User user = userMapper.selectById(userId);
            if (user == null) {
                log.warn("用户不存在: userId={}", userId);
                return false;
            }

            // 更新出生年月
            user.setBirthDate(request.getBirthDate());
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);

            log.info("用户基本信息更新成功: userId={}", userId);
            return true;
        } catch (Exception e) {
            log.error("更新用户基本信息失败: userId={}", userId, e);
            return false;
        }
    }
}
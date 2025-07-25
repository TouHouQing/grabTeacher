package com.touhouqing.grabteacherbackend.service.impl;

import com.touhouqing.grabteacherbackend.dto.AuthResponse;
import com.touhouqing.grabteacherbackend.dto.LoginRequest;
import com.touhouqing.grabteacherbackend.dto.RegisterRequest;
import com.touhouqing.grabteacherbackend.entity.Student;
import com.touhouqing.grabteacherbackend.entity.Teacher;
import com.touhouqing.grabteacherbackend.entity.User;
import com.touhouqing.grabteacherbackend.entity.Admin;
import com.touhouqing.grabteacherbackend.entity.TeacherSubject;
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
    public AuthResponse registerUser(RegisterRequest registerRequest) {
        try {
            // 验证密码确认
            if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
                throw new RuntimeException("密码和确认密码不匹配");
            }

            // 检查用户名是否已存在
            if (userMapper.existsByUsername(registerRequest.getUsername())) {
                throw new RuntimeException("用户名已被使用");
            }

            // 检查邮箱是否已存在
            if (userMapper.existsByEmail(registerRequest.getEmail())) {
                throw new RuntimeException("邮箱已被注册");
            }

            // 创建用户
            User user = User.builder()
                    .username(registerRequest.getUsername())
                    .email(registerRequest.getEmail())
                    .password(passwordEncoder.encode(registerRequest.getPassword()))
                    .phone(registerRequest.getPhone())
                    .userType(registerRequest.getUserType().name())
                    .status("active")
                    .isDeleted(false)
                    .hasUsedTrial(false) // 新用户默认未使用免费试听
                    .build();

            userMapper.insert(user);
            log.info("用户注册成功: {}, ID: {}", user.getEmail(), user.getId());

            // 根据用户类型创建对应的详细信息记录
            if ("student".equals(registerRequest.getUserType().name())) {
                Student student = Student.builder()
                        .userId(user.getId())
                        .realName(registerRequest.getRealName())
                        .gradeLevel(StringUtils.hasText(registerRequest.getGradeLevel()) ? registerRequest.getGradeLevel() : null)
                        .subjectsInterested(StringUtils.hasText(registerRequest.getSubjectsInterested()) ? registerRequest.getSubjectsInterested() : null)
                        .learningGoals(StringUtils.hasText(registerRequest.getLearningGoals()) ? registerRequest.getLearningGoals() : null)
                        .preferredTeachingStyle(StringUtils.hasText(registerRequest.getPreferredTeachingStyle()) ? registerRequest.getPreferredTeachingStyle() : null)
                        .budgetRange(StringUtils.hasText(registerRequest.getBudgetRange()) ? registerRequest.getBudgetRange() : null)
                        .gender(StringUtils.hasText(registerRequest.getGender()) ? registerRequest.getGender() : "不愿透露")
                        .isDeleted(false)
                        .build();
                
                studentMapper.insert(student);
                log.info("学生信息创建成功: {}", user.getId());

                // 处理学生感兴趣的科目关联
                if (registerRequest.getStudentSubjectIds() != null && !registerRequest.getStudentSubjectIds().isEmpty()) {
                    for (Long subjectId : registerRequest.getStudentSubjectIds()) {
                        StudentSubject studentSubject = new StudentSubject(student.getId(), subjectId);
                        studentSubjectMapper.insert(studentSubject);
                    }
                    log.info("学生科目关联创建成功: 学生ID={}, 科目数量={}", student.getId(), registerRequest.getStudentSubjectIds().size());
                }
                
            } else if ("teacher".equals(registerRequest.getUserType().name())) {
                Teacher teacher = Teacher.builder()
                        .userId(user.getId())
                        .realName(registerRequest.getRealName())
                        .educationBackground(StringUtils.hasText(registerRequest.getEducationBackground()) ? registerRequest.getEducationBackground() : null)
                        .teachingExperience(registerRequest.getTeachingExperience() != null && registerRequest.getTeachingExperience() > 0 ? registerRequest.getTeachingExperience() : null)
                        .specialties(StringUtils.hasText(registerRequest.getSpecialties()) ? registerRequest.getSpecialties() : null)
                        .hourlyRate(registerRequest.getHourlyRate() != null && registerRequest.getHourlyRate().compareTo(java.math.BigDecimal.ZERO) > 0 ? registerRequest.getHourlyRate() : null)
                        .introduction(StringUtils.hasText(registerRequest.getIntroduction()) ? registerRequest.getIntroduction() : null)
                        .isVerified(false)
                        .isDeleted(false)
                        .build();

                teacherMapper.insert(teacher);

                // 处理教师科目关联
                if (registerRequest.getSubjectIds() != null && !registerRequest.getSubjectIds().isEmpty()) {
                    for (Long subjectId : registerRequest.getSubjectIds()) {
                        TeacherSubject teacherSubject = new TeacherSubject(teacher.getId(), subjectId);
                        teacherSubjectMapper.insert(teacherSubject);
                    }
                }
                log.info("教师信息创建成功: {}", user.getId());
            }

            // 生成 JWT token
            String jwt = jwtUtil.generateTokenFromUserId(user.getId());

            return new AuthResponse(jwt, user.getId(), user.getUsername(), 
                                  user.getEmail(), user.getUserType());
        } catch (Exception e) {
            log.error("用户注册失败: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 用户登录
     */
    @Override
    public AuthResponse authenticateUser(LoginRequest loginRequest) {
        try {
            log.info("尝试登录用户: {}", loginRequest.getUsername());

            // 支持用户名或邮箱登录
            User user = userMapper.findByUsernameOrEmail(loginRequest.getUsername());
            if (user == null) {
                log.warn("登录失败，用户不存在: {}", loginRequest.getUsername());
                throw new BadCredentialsException("用户名、邮箱或密码错误");
            }

            log.info("找到用户: {}, 状态: {}", user.getEmail(), user.getStatus());

            // 检查用户状态
            if (!"active".equals(user.getStatus())) {
                log.warn("登录失败，用户账户未激活: {}", loginRequest.getUsername());
                throw new BadCredentialsException("账户未激活");
            }

            // 验证密码
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                log.warn("登录失败，密码错误: {}", loginRequest.getUsername());
                throw new BadCredentialsException("用户名、邮箱或密码错误");
            }

            // 进行身份验证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
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

            return new AuthResponse(jwt, user.getId(), user.getUsername(),
                                  user.getEmail(), user.getUserType(), realName);
        } catch (BadCredentialsException e) {
            log.warn("登录失败，认证错误: {}", e.getMessage());
            throw new BadCredentialsException("用户名、邮箱或密码错误");
        } catch (AuthenticationException e) {
            log.warn("登录失败，认证异常: {}", e.getMessage());
            throw new BadCredentialsException("用户名、邮箱或密码错误");
        } catch (Exception e) {
            log.error("登录失败，系统错误: {}", e.getMessage(), e);
            throw new RuntimeException("登录失败，请稍后重试");
        }
    }

    /**
     * 管理员登录
     */
    @Override
    public AuthResponse authenticateAdmin(LoginRequest loginRequest) {
        try {
            log.info("尝试管理员登录: {}", loginRequest.getUsername());

            // 支持用户名或邮箱登录
            User user = userMapper.findByUsernameOrEmail(loginRequest.getUsername());
            if (user == null) {
                log.warn("管理员登录失败，用户不存在: {}", loginRequest.getUsername());
                throw new BadCredentialsException("用户名、邮箱或密码错误");
            }

            // 检查是否为管理员类型
            if (!"admin".equals(user.getUserType())) {
                log.warn("登录失败，用户类型不是管理员: {}", loginRequest.getUsername());
                throw new BadCredentialsException("用户名、邮箱或密码错误");
            }

            log.info("找到管理员用户: {}, 状态: {}", user.getEmail(), user.getStatus());

            // 检查用户状态
            if (!"active".equals(user.getStatus())) {
                log.warn("管理员登录失败，账户未激活: {}", loginRequest.getUsername());
                throw new BadCredentialsException("账户未激活");
            }

            // 验证密码
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                log.warn("管理员登录失败，密码错误: {}", loginRequest.getUsername());
                throw new BadCredentialsException("用户名、邮箱或密码错误");
            }

            // 进行身份验证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String jwt = jwtUtil.generateToken(authentication);

            // 获取管理员真实姓名
            Admin admin = adminMapper.findByUserId(user.getId());
            String realName = admin != null ? admin.getRealName() : null;

            log.info("管理员登录成功: {}", user.getEmail());

            return new AuthResponse(jwt, user.getId(), user.getUsername(), 
                                  user.getEmail(), user.getUserType(), realName);
        } catch (BadCredentialsException e) {
            log.warn("管理员登录失败，认证错误: {}", e.getMessage());
            throw new BadCredentialsException("用户名、邮箱或密码错误");
        } catch (AuthenticationException e) {
            log.warn("管理员登录失败，认证异常: {}", e.getMessage());
            throw new BadCredentialsException("用户名、邮箱或密码错误");
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
} 
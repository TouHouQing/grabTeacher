package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.entity.Student;
import com.touhouqing.grabteacherbackend.entity.Teacher;
import com.touhouqing.grabteacherbackend.entity.User;
import com.touhouqing.grabteacherbackend.mapper.StudentMapper;
import com.touhouqing.grabteacherbackend.mapper.TeacherMapper;
import com.touhouqing.grabteacherbackend.mapper.UserMapper;
import com.touhouqing.grabteacherbackend.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserMapper userMapper;
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;

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

        if (!List.of("active", "inactive", "banned").contains(status)) {
            throw new RuntimeException("无效的状态值");
        }

        user.setStatus(status);
        userMapper.updateById(user);

        log.info("更新用户状态成功: userId={}, status={}", userId, status);
    }
}

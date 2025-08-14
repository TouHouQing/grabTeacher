package com.touhouqing.grabteacherbackend;

import com.touhouqing.grabteacherbackend.entity.User;
import com.touhouqing.grabteacherbackend.mapper.UserMapper;
import com.touhouqing.grabteacherbackend.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class EmailUpdateTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    public void testUpdateEmailSuccess() {
        // 创建测试用户
        User testUser = User.builder()
                .username("testuser")
                .email("old@example.com")
                .password(passwordEncoder.encode("password123"))
                .userType("student")
                .status("active")
                .isDeleted(false)
                .build();
        
        userMapper.insert(testUser);
        
        // 测试邮箱更新
        boolean result = authService.updateEmail(testUser.getId(), "new@example.com", "password123");
        
        assertTrue(result, "邮箱更新应该成功");
        
        // 验证邮箱已更新
        User updatedUser = userMapper.selectById(testUser.getId());
        assertEquals("new@example.com", updatedUser.getEmail(), "邮箱应该已更新");
    }

    @Test
    public void testUpdateEmailWithWrongPassword() {
        // 创建测试用户
        User testUser = User.builder()
                .username("testuser2")
                .email("old2@example.com")
                .password(passwordEncoder.encode("password123"))
                .userType("student")
                .status("active")
                .isDeleted(false)
                .build();
        
        userMapper.insert(testUser);
        
        // 测试使用错误密码更新邮箱
        boolean result = authService.updateEmail(testUser.getId(), "new2@example.com", "wrongpassword");
        
        assertFalse(result, "使用错误密码更新邮箱应该失败");
        
        // 验证邮箱未更新
        User unchangedUser = userMapper.selectById(testUser.getId());
        assertEquals("old2@example.com", unchangedUser.getEmail(), "邮箱不应该被更新");
    }

    @Test
    public void testUpdateEmailWithDuplicateEmail() {
        // 创建两个测试用户
        User user1 = User.builder()
                .username("testuser3")
                .email("user1@example.com")
                .password(passwordEncoder.encode("password123"))
                .userType("student")
                .status("active")
                .isDeleted(false)
                .build();
        
        User user2 = User.builder()
                .username("testuser4")
                .email("user2@example.com")
                .password(passwordEncoder.encode("password123"))
                .userType("student")
                .status("active")
                .isDeleted(false)
                .build();
        
        userMapper.insert(user1);
        userMapper.insert(user2);
        
        // 尝试将user2的邮箱更新为user1的邮箱
        boolean result = authService.updateEmail(user2.getId(), "user1@example.com", "password123");
        
        assertFalse(result, "更新为已存在的邮箱应该失败");
        
        // 验证邮箱未更新
        User unchangedUser = userMapper.selectById(user2.getId());
        assertEquals("user2@example.com", unchangedUser.getEmail(), "邮箱不应该被更新");
    }

    @Test
    public void testEmailAvailabilityCheck() {
        // 创建测试用户
        User testUser = User.builder()
                .username("testuser5")
                .email("existing@example.com")
                .password(passwordEncoder.encode("password123"))
                .userType("student")
                .status("active")
                .isDeleted(false)
                .build();
        
        userMapper.insert(testUser);
        
        // 测试邮箱可用性检查
        assertFalse(authService.isEmailAvailable("existing@example.com"), "已存在的邮箱应该不可用");
        assertTrue(authService.isEmailAvailable("available@example.com"), "不存在的邮箱应该可用");
    }
}

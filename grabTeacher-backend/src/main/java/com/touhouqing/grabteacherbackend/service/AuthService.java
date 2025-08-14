package com.touhouqing.grabteacherbackend.service;

import com.touhouqing.grabteacherbackend.model.vo.AuthVO;
import com.touhouqing.grabteacherbackend.model.dto.LoginDTO;
import com.touhouqing.grabteacherbackend.model.dto.RegisterDTO;
import com.touhouqing.grabteacherbackend.model.dto.UserUpdateDTO;
import com.touhouqing.grabteacherbackend.model.entity.User;

public interface AuthService {
    
    /**
     * 用户注册
     */
    AuthVO registerUser(RegisterDTO registerDTO);
    
    /**
     * 用户登录
     */
    AuthVO authenticateUser(LoginDTO loginDTO);
    
    /**
     * 检查用户名是否可用
     */
    boolean isUsernameAvailable(String username);
    
    /**
     * 检查邮箱是否可用
     */
    boolean isEmailAvailable(String email);
    
    /**
     * 根据用户ID获取用户信息
     */
    User getUserById(Long userId);
    
    /**
     * 更新用户密码
     */
    void updatePassword(Long userId, String oldPassword, String newPassword);
    
    /**
     * 修改密码
     */
    boolean changePassword(Long userId, String currentPassword, String newPassword);

    /**
     * 更新用户邮箱
     */
    boolean updateEmail(Long userId, String newEmail, String currentPassword);

    /**
     * 管理员登录
     */
    AuthVO authenticateAdmin(LoginDTO loginDTO);

    /**
     * 更新用户基本信息
     */
    boolean updateUserProfile(Long userId, UserUpdateDTO request);
}
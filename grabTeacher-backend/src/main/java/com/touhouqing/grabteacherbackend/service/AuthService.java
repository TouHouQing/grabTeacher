package com.touhouqing.grabteacherbackend.service;

import com.touhouqing.grabteacherbackend.dto.AuthResponseDTO;
import com.touhouqing.grabteacherbackend.dto.LoginRequestDTO;
import com.touhouqing.grabteacherbackend.dto.RegisterRequestDTO;
import com.touhouqing.grabteacherbackend.dto.UserUpdateRequestDTO;
import com.touhouqing.grabteacherbackend.entity.User;

public interface AuthService {
    
    /**
     * 用户注册
     */
    AuthResponseDTO registerUser(RegisterRequestDTO registerRequestDTO);
    
    /**
     * 用户登录
     */
    AuthResponseDTO authenticateUser(LoginRequestDTO loginRequestDTO);
    
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
    AuthResponseDTO authenticateAdmin(LoginRequestDTO loginRequestDTO);

    /**
     * 更新用户基本信息
     */
    boolean updateUserProfile(Long userId, UserUpdateRequestDTO request);
}
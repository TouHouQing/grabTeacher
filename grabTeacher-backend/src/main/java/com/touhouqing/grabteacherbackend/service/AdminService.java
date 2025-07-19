package com.touhouqing.grabteacherbackend.service;

import com.touhouqing.grabteacherbackend.entity.User;

import java.util.List;
import java.util.Map;

public interface AdminService {
    
    /**
     * 获取系统统计信息
     */
    Map<String, Object> getSystemStatistics();
    
    /**
     * 获取用户列表
     */
    List<User> getUserList(int page, int size, String userType, String keyword);
    
    /**
     * 更新用户状态
     */
    void updateUserStatus(Long userId, String status);
} 
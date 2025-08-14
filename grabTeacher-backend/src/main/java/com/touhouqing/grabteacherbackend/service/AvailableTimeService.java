package com.touhouqing.grabteacherbackend.service;

import com.touhouqing.grabteacherbackend.dto.AvailableTimeRequestDTO;
import com.touhouqing.grabteacherbackend.dto.AvailableTimeResponseDTO;

/**
 * 可上课时间服务接口
 */
public interface AvailableTimeService {
    
    /**
     * 获取教师的可上课时间
     */
    AvailableTimeResponseDTO getTeacherAvailableTime(Long teacherId);
    
    /**
     * 设置教师的可上课时间
     */
    AvailableTimeResponseDTO setTeacherAvailableTime(AvailableTimeRequestDTO request);
    
    /**
     * 根据用户ID获取教师ID
     */
    Long getTeacherIdByUserId(Long userId);
}

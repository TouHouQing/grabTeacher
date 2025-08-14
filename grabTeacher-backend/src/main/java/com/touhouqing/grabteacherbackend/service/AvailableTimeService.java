package com.touhouqing.grabteacherbackend.service;

import com.touhouqing.grabteacherbackend.entity.dto.AvailableTimeRequest;
import com.touhouqing.grabteacherbackend.entity.dto.AvailableTimeResponse;

/**
 * 可上课时间服务接口
 */
public interface AvailableTimeService {
    
    /**
     * 获取教师的可上课时间
     */
    AvailableTimeResponse getTeacherAvailableTime(Long teacherId);
    
    /**
     * 设置教师的可上课时间
     */
    AvailableTimeResponse setTeacherAvailableTime(AvailableTimeRequest request);
    
    /**
     * 根据用户ID获取教师ID
     */
    Long getTeacherIdByUserId(Long userId);
}

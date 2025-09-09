package com.touhouqing.grabteacherbackend.service;

import com.touhouqing.grabteacherbackend.model.dto.AvailableTimeQueryDTO;
import com.touhouqing.grabteacherbackend.model.vo.AvailableTimeVO;

import java.util.List;

/**
 * 可上课时间服务接口
 */
public interface AvailableTimeService {

    /**
     * 获取教师的可上课时间
     */
    AvailableTimeVO getTeacherAvailableTime(Long teacherId);

    /**
     * 批量获取教师可上课时间
     */
    List<AvailableTimeVO> getTeachersAvailableTime(List<Long> teacherIds);

    /**
     * 设置教师的可上课时间
     */
    AvailableTimeVO setTeacherAvailableTime(AvailableTimeQueryDTO request);

    /**
     * 根据用户ID获取教师ID
     */
    Long getTeacherIdByUserId(Long userId);
}

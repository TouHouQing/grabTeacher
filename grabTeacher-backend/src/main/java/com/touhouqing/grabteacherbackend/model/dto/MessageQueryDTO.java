package com.touhouqing.grabteacherbackend.model.dto;

import com.touhouqing.grabteacherbackend.model.entity.Message;
import lombok.Data;

/**
 * 消息查询DTO
 */
@Data
public class MessageQueryDTO {
    
    /**
     * 消息标题（模糊查询）
     */
    private String title;
    
    /**
     * 目标用户类型
     */
    private Message.TargetType targetType;
    
    /**
     * 发布管理员姓名（模糊查询）
     */
    private String adminName;
    
    /**
     * 是否激活
     */
    private Boolean isActive;
    
    /**
     * 页码
     */
    private Integer pageNum = 1;
    
    /**
     * 页大小
     */
    private Integer pageSize = 10;
}

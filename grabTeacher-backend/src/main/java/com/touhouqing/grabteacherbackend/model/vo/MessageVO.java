package com.touhouqing.grabteacherbackend.model.vo;

import com.touhouqing.grabteacherbackend.model.entity.Message;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息VO
 */
@Data
public class MessageVO {
    
    /**
     * 消息ID
     */
    private Long id;
    
    /**
     * 消息标题
     */
    private String title;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 目标用户类型
     */
    private Message.TargetType targetType;
    
    /**
     * 目标用户类型描述
     */
    private String targetTypeDescription;
    
    /**
     * 发布管理员ID
     */
    private Long adminId;
    
    /**
     * 发布管理员姓名
     */
    private String adminName;
    
    /**
     * 是否激活
     */
    private Boolean isActive;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

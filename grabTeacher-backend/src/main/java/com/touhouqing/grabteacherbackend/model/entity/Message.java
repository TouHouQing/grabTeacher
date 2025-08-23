package com.touhouqing.grabteacherbackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 消息实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("message")
public class Message {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 消息标题
     */
    @TableField("title")
    private String title;
    
    /**
     * 消息内容
     */
    @TableField("content")
    private String content;
    
    /**
     * 目标用户类型：STUDENT-学生，TEACHER-教师，ALL-全体
     */
    @TableField("target_type")
    private TargetType targetType;
    
    /**
     * 发布管理员ID
     */
    @TableField("admin_id")
    private Long adminId;
    
    /**
     * 发布管理员姓名
     */
    @TableField("admin_name")
    private String adminName;
    
    /**
     * 是否激活：true-激活，false-停用
     */
    @TableField("is_active")
    private Boolean isActive;
    
    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    /**
     * 目标用户类型枚举
     */
    public enum TargetType {
        STUDENT("学生"),
        TEACHER("教师"),
        ALL("全体");
        
        private final String description;
        
        TargetType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}

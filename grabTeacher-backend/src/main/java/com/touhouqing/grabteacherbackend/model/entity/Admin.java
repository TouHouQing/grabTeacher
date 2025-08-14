package com.touhouqing.grabteacherbackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("admins")
public class Admin {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("user_id")
    private Long userId;
    
    @TableField("real_name")
    private String realName;
    
    @TableField("notes")
    private String notes;
    
    @TableField("is_deleted")
    private Boolean isDeleted;
    
    @TableField("deleted_at")
    private LocalDateTime deletedAt;
} 
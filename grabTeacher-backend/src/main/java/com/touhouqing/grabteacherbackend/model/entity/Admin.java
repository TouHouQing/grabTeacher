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

    @TableField("avatar_url")
    private String avatarUrl;

    @TableField("wechat_qrcode_url")
    private String wechatQrcodeUrl;

    @TableField("whatsapp_number")
    private String whatsappNumber;

    @TableField("email")
    private String email;

    @TableField("is_deleted")
    private Boolean deleted;
    
    @TableField("deleted_at")
    private LocalDateTime deletedAt;
} 
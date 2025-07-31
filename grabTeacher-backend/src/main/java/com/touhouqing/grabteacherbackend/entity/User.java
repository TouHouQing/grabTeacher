package com.touhouqing.grabteacherbackend.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    private String username;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度不能少于6位")
    private String password;

    @Size(max = 20, message = "手机号长度不能超过20位")
    private String phone;

    @TableField("birth_date")
    private String birthDate;

    @TableField("avatar_url")
    private String avatarUrl;

    @TableField("user_type")
    private String userType;

    @Builder.Default
    private String status = "active";

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField("is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;

    @TableField("has_used_trial")
    @Builder.Default
    private Boolean hasUsedTrial = false;

    @TableField("trial_used_at")
    private LocalDateTime trialUsedAt;

    // 保留必要的构造函数
    public User(String username, String email, String password, String userType) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.userType = userType;
        this.status = "active";
        this.isDeleted = false;
        this.hasUsedTrial = false;
    }
}
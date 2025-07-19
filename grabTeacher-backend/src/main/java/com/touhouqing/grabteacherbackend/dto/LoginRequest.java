package com.touhouqing.grabteacherbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @NotBlank(message = "用户名或邮箱不能为空")
    private String username; // 改为 username，可以接受用户名或邮箱

    @NotBlank(message = "密码不能为空")
    private String password;

    // 保持向后兼容
    public String getEmail() { 
        return username; 
    }
    
    public void setEmail(String email) { 
        this.username = email; 
    }
} 
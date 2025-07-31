package com.touhouqing.grabteacherbackend.dto;

import com.touhouqing.grabteacherbackend.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordChangeRequest {
    
    @NotBlank(message = "当前密码不能为空")
    private String currentPassword;
    
    @NotBlank(message = "新密码不能为空")
    @ValidPassword
    private String newPassword;
    
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
} 
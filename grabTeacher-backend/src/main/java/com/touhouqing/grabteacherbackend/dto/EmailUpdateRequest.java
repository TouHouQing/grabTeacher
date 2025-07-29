package com.touhouqing.grabteacherbackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailUpdateRequest {
    
    @NotBlank(message = "新邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String newEmail;
    
    @NotBlank(message = "当前密码不能为空")
    @Size(min = 6, message = "密码长度不能少于6位")
    private String currentPassword;
}
